import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Check, ArrowRight, AlertCircle } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { toast } from "sonner";
import {
  crearTransaccion,
  obtenerTiposTransaccion,
  TipoTransaccion,
} from "@/services/transaccionService";

const Transfer = () => {
  const { user } = useAuth();
  const [step, setStep] = useState(1);
  const [toAccount, setToAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [tipoSeleccionado, setTipoSeleccionado] =
    useState<string>("TRANSFERENCIA");
  const [tipos, setTipos] = useState<TipoTransaccion[]>([]);
  const [showConfirm, setShowConfirm] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    obtenerTiposTransaccion()
      .then(setTipos)
      .catch(() => {
        /* silencioso — se usa el default */
      });
  }, []);

  if (!user) return null;

  const parsedAmount = parseFloat(amount.replace(/,/g, "")) || 0;
  const canProceed =
    step === 1
      ? toAccount.length >= 4
      : parsedAmount > 0 && parsedAmount <= user.saldo;

  const handleConfirm = async () => {
    setLoading(true);
    try {
      const tipoObj = tipos.find((t) => t.nombre === tipoSeleccionado);
      const transaccion = {
        cuentaOrigenId: user.numeroCuenta,
        cuentaDestinoId: toAccount,
        monto: parsedAmount,
        tipoTransaccion: tipoObj,
      };

      const result = await crearTransaccion(transaccion);

      const estadoId = result.estadoId;
      let mensaje = "Transferencia creada";

      if (estadoId === 5) {
        mensaje = "Transferencia aprobada";
      } else if (estadoId === 4) {
        mensaje = "Transferencia pendiente de revision";
      } else if (estadoId === 6) {
        mensaje = "Transferencia rechazada";
      }

      toast.success(mensaje);

      setStep(1);
      setToAccount("");
      setAmount("");
      setShowConfirm(false);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : "Error al procesar la transferencia";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-lg mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">
          Nueva Transferencia
        </h1>
        <p className="text-muted-foreground text-sm mt-1">
          Envía dinero de forma segura
        </p>
      </div>

      {/* Steps indicator */}
      <div className="flex items-center gap-2">
        {[1, 2].map((s) => (
          <div key={s} className="flex items-center gap-2 flex-1">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-semibold transition-colors ${
                step >= s
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-muted-foreground"
              }`}
            >
              {step > s ? <Check className="h-4 w-4" /> : s}
            </div>
            <span
              className={`text-sm font-medium ${step >= s ? "text-foreground" : "text-muted-foreground"}`}
            >
              {s === 1 ? "Destino" : "Monto"}
            </span>
            {s < 2 && <div className="flex-1 h-px bg-border" />}
          </div>
        ))}
      </div>

      <motion.div
        key={step}
        initial={{ opacity: 0, x: 20 }}
        animate={{ opacity: 1, x: 0 }}
        className="bg-card rounded-2xl shadow-card p-6 space-y-5"
      >
        {step === 1 && (
          <>
            <label className="block">
              <span className="text-sm font-medium text-foreground mb-2 block">
                Cuenta Destino
              </span>
              <Input
                placeholder="Ej. 1000-0045"
                value={toAccount}
                onChange={(e) => setToAccount(e.target.value)}
                className="h-12 rounded-xl bg-secondary/50 border-0 focus-visible:ring-2 focus-visible:ring-primary/20"
              />
            </label>
            <label className="block">
              <span className="text-sm font-medium text-foreground mb-2 block">
                Tipo de Transacción
              </span>
              <Select
                value={tipoSeleccionado}
                onValueChange={setTipoSeleccionado}
              >
                <SelectTrigger className="h-12 rounded-xl bg-secondary/50 border-0 focus:ring-2 focus:ring-primary/20">
                  <SelectValue placeholder="Selecciona un tipo" />
                </SelectTrigger>
                <SelectContent>
                  {(tipos.length > 0
                    ? tipos
                    : [
                        { id: 1, nombre: "TRANSFERENCIA" },
                        { id: 2, nombre: "DEPOSITO" },
                        { id: 3, nombre: "RETIRO" },
                        { id: 4, nombre: "PAGO" },
                        { id: 5, nombre: "COMPRA" },
                      ]
                  ).map((t) => (
                    <SelectItem key={t.id} value={t.nombre}>
                      {t.nombre}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </label>
            <Button
              onClick={() => setStep(2)}
              disabled={!canProceed}
              className="w-full h-12 rounded-xl font-semibold"
            >
              Continuar <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </>
        )}

        {step === 2 && (
          <>
            <div className="bg-secondary/50 rounded-xl p-4 flex items-center justify-between">
              <span className="text-sm text-muted-foreground">
                Saldo Disponible
              </span>
              <span className="text-lg font-bold text-foreground">
                $
                {user.saldo.toLocaleString("es-MX", {
                  minimumFractionDigits: 2,
                })}
              </span>
            </div>
            <label className="block">
              <span className="text-sm font-medium text-foreground mb-2 block">
                Monto a Transferir
              </span>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-semibold">
                  $
                </span>
                <Input
                  type="number"
                  placeholder="0.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="pl-8 h-12 rounded-xl bg-secondary/50 border-0 text-lg font-semibold focus-visible:ring-2 focus-visible:ring-primary/20"
                  step="0.01"
                />
              </div>
              {parsedAmount > user.saldo && (
                <p className="text-xs text-status-rejected flex items-center gap-1 mt-2">
                  <AlertCircle className="h-3 w-3" /> Saldo insuficiente
                </p>
              )}
            </label>
            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={() => setStep(1)}
                className="flex-1 h-12 rounded-xl"
              >
                Atrás
              </Button>
              <Button
                onClick={() => setShowConfirm(true)}
                disabled={!canProceed}
                className="flex-1 h-12 rounded-xl font-semibold"
              >
                Transferir
              </Button>
            </div>
          </>
        )}
      </motion.div>

      {/* Confirmation modal */}
      <AnimatePresence>
        {showConfirm && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-foreground/20 backdrop-blur-sm z-50 flex items-center justify-center p-4"
            onClick={() => setShowConfirm(false)}
          >
            <motion.div
              initial={{ scale: 0.95 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0.95 }}
              onClick={(e) => e.stopPropagation()}
              className="bg-card rounded-2xl shadow-soft p-6 w-full max-w-sm space-y-5"
            >
              <h3 className="text-lg font-bold text-foreground text-center">
                Confirmar Transferencia
              </h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between py-2 border-b border-border">
                  <span className="text-muted-foreground">Cuenta destino</span>
                  <span className="font-semibold text-foreground">
                    {toAccount}
                  </span>
                </div>
                <div className="flex justify-between py-2 border-b border-border">
                  <span className="text-muted-foreground">Tipo</span>
                  <span className="font-semibold text-foreground">
                    {tipoSeleccionado}
                  </span>
                </div>
                <div className="flex justify-between py-2 border-b border-border">
                  <span className="text-muted-foreground">Monto</span>
                  <span className="font-bold text-foreground text-lg">
                    $
                    {parsedAmount.toLocaleString("es-MX", {
                      minimumFractionDigits: 2,
                    })}
                  </span>
                </div>
                <div className="flex justify-between py-2">
                  <span className="text-muted-foreground">Nuevo saldo</span>
                  <span className="font-semibold text-foreground">
                    $
                    {(user.saldo - parsedAmount).toLocaleString("es-MX", {
                      minimumFractionDigits: 2,
                    })}
                  </span>
                </div>
              </div>
              <div className="flex gap-3">
                <Button
                  variant="outline"
                  onClick={() => setShowConfirm(false)}
                  disabled={loading}
                  className="flex-1 h-11 rounded-xl"
                >
                  Cancelar
                </Button>
                <Button
                  onClick={handleConfirm}
                  disabled={loading}
                  className="flex-1 h-11 rounded-xl font-semibold"
                >
                  {loading ? (
                    <motion.div
                      animate={{ rotate: 360 }}
                      transition={{
                        repeat: Infinity,
                        duration: 1,
                        ease: "linear",
                      }}
                      className="w-4 h-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full"
                    />
                  ) : (
                    "Confirmar"
                  )}
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default Transfer;
