import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Label } from "@/components/ui/label";
import { toast } from "@/hooks/use-toast";
import {
  Zap,
  Droplets,
  Wifi,
  Flame,
  Phone,
  Tv,
  CheckCircle2,
  Clock,
  AlertCircle,
  CreditCard,
  Building2,
} from "lucide-react";

const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

interface Factura {
  id: number;
  tipoServicio: string;
  descripcion: string;
  referencia: string;
  monto: number;
  estado: string;
  fechaVencimiento: string;
  fechaPago?: string;
  tarjetaId?: number;
}

interface Tarjeta {
  id: number;
  marca: string;
  ultimosCuatro: string;
  tipoTarjeta: string;
  estadoId: number;
  limiteCredito: number;
  creditoDisponible: number;
  saldoTarjeta: number;
}

const servicioIcono: Record<string, React.ReactNode> = {
  LUZ: <Zap size={20} className="text-yellow-500" />,
  AGUA: <Droplets size={20} className="text-blue-500" />,
  INTERNET: <Wifi size={20} className="text-purple-500" />,
  GAS: <Flame size={20} className="text-orange-500" />,
  TELEFONO: <Phone size={20} className="text-green-500" />,
  TELEVISION: <Tv size={20} className="text-pink-500" />,
};

const estadoBadge = (estado: string) => {
  if (estado === "PAGADA")
    return (
      <Badge className="bg-green-100 text-green-700 border-green-300">
        <CheckCircle2 size={12} className="mr-1" /> Pagada
      </Badge>
    );
  if (estado === "VENCIDA")
    return (
      <Badge className="bg-red-100 text-red-700 border-red-300">
        <AlertCircle size={12} className="mr-1" /> Vencida
      </Badge>
    );
  return (
    <Badge className="bg-amber-100 text-amber-700 border-amber-300">
      <Clock size={12} className="mr-1" /> Pendiente
    </Badge>
  );
};

const fmt = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0,
});

export default function Facturas() {
  const { user } = useAuth();
  const [facturas, setFacturas] = useState<Factura[]>([]);
  const [tarjetas, setTarjetas] = useState<Tarjeta[]>([]);
  const [loading, setLoading] = useState(true);
  const [generando, setGenerando] = useState(false);
  const [pagando, setPagando] = useState<number | null>(null);

  const [pagoModal, setPagoModal] = useState<{
    open: boolean;
    factura: Factura | null;
  }>({ open: false, factura: null });
  const [metodoPago, setMetodoPago] = useState<"tarjeta" | "saldo">("saldo");
  const [tarjetaSeleccionada, setTarjetaSeleccionada] = useState<string>("");

  const cargar = async () => {
    if (!user) return;
    try {
      const [fRes, tRes] = await Promise.all([
        fetch(`${API}/api/facturas`, {
          headers: { "X-User-Documento": user.numDocumento },
        }),
        fetch(`${API}/api/tarjetas`, {
          headers: { "X-User-Documento": user.numDocumento },
        }),
      ]);
      const fData = await fRes.json();
      const tData = await tRes.json();
      setFacturas(Array.isArray(fData) ? fData : []);
      // Solo tarjetas activas para el pago
      const activas = Array.isArray(tData)
        ? tData.filter((t: Tarjeta) => t.estadoId === 1)
        : [];
      setTarjetas(activas);
    } catch {
      toast({
        title: "Error",
        description: "No se pudieron cargar los datos",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargar();
  }, [user]);

  const generarFacturas = async () => {
    if (!user) return;
    setGenerando(true);
    try {
      const res = await fetch(`${API}/api/facturas/generar-prueba`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Documento": user.numDocumento,
        },
        body: JSON.stringify({ tipoDocumentoId: 1 }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
      toast({
        title: "Facturas generadas",
        description: "Se generaron facturas de prueba de servicios",
      });
      cargar();
    } catch (e: any) {
      toast({ title: "Error", description: e.message, variant: "destructive" });
    } finally {
      setGenerando(false);
    }
  };

  const abrirPago = (factura: Factura) => {
    setPagoModal({ open: true, factura });
    setMetodoPago("saldo");
    setTarjetaSeleccionada(tarjetas[0]?.id?.toString() || "");
  };

  const confirmarPago = async () => {
    if (!user || !pagoModal.factura) return;
    setPagando(pagoModal.factura.id);
    try {
      const body: Record<string, any> = {};
      if (metodoPago === "tarjeta") {
        if (!tarjetaSeleccionada) throw new Error("Selecciona una tarjeta");
        body.tarjetaId = parseInt(tarjetaSeleccionada);
      } else {
        body.numeroCuenta = user.numeroCuenta;
      }

      const res = await fetch(
        `${API}/api/facturas/${pagoModal.factura.id}/pagar`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-User-Documento": user.numDocumento,
          },
          body: JSON.stringify(body),
        },
      );
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
      toast({
        title: "¡Pago exitoso!",
        description: `${pagoModal.factura.tipoServicio} — ${fmt.format(pagoModal.factura.monto)} pagado con ${metodoPago === "tarjeta" ? "tarjeta" : "saldo"}`,
      });
      setPagoModal({ open: false, factura: null });
      cargar();
    } catch (e: any) {
      toast({
        title: "Error en el pago",
        description: e.message,
        variant: "destructive",
      });
    } finally {
      setPagando(null);
    }
  };

  const pendientes = facturas.filter((f) => f.estado === "PENDIENTE");
  const pagadas = facturas.filter((f) => f.estado === "PAGADA");

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold">Pago de Facturas</h1>
          <p className="text-muted-foreground text-sm mt-1">
            Paga tus servicios con saldo de cuenta o tarjeta
          </p>
        </div>
        <Button
          variant="outline"
          onClick={generarFacturas}
          disabled={generando}
        >
          {generando ? "Generando..." : "Generar facturas de prueba"}
        </Button>
      </div>

      {/* KPI */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
        <Card>
          <CardContent className="pt-4">
            <p className="text-xs text-muted-foreground">Total facturas</p>
            <p className="text-2xl font-bold">{facturas.length}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <p className="text-xs text-muted-foreground">Pendientes</p>
            <p className="text-2xl font-bold text-amber-600">
              {pendientes.length}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-4">
            <p className="text-xs text-muted-foreground">Total pendiente</p>
            <p className="text-xl font-bold text-red-500">
              {fmt.format(pendientes.reduce((a, f) => a + f.monto, 0))}
            </p>
          </CardContent>
        </Card>
      </div>

      {loading ? (
        <div className="text-center py-12 text-muted-foreground">
          Cargando facturas...
        </div>
      ) : facturas.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center py-16 gap-3 text-center">
            <Building2 size={48} className="text-muted-foreground/40" />
            <p className="text-muted-foreground">
              No tienes facturas registradas
            </p>
            <Button onClick={generarFacturas} disabled={generando}>
              Generar facturas de prueba
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {pendientes.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-2">
                Pendientes
              </h2>
              <div className="space-y-3">
                {pendientes.map((f) => (
                  <Card key={f.id} className="border-l-4 border-l-amber-400">
                    <CardContent className="flex items-center justify-between py-4 flex-wrap gap-3">
                      <div className="flex items-center gap-3">
                        {servicioIcono[f.tipoServicio] || (
                          <Building2 size={20} />
                        )}
                        <div>
                          <p className="font-medium text-sm">{f.descripcion}</p>
                          <p className="text-xs text-muted-foreground">
                            Ref: {f.referencia}
                          </p>
                          <p className="text-xs text-muted-foreground">
                            Vence:{" "}
                            {new Date(f.fechaVencimiento).toLocaleDateString(
                              "es-CO",
                            )}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        {estadoBadge(f.estado)}
                        <span className="font-semibold">
                          {fmt.format(f.monto)}
                        </span>
                        <Button size="sm" onClick={() => abrirPago(f)}>
                          Pagar
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {pagadas.length > 0 && (
            <div>
              <h2 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide mb-2">
                Pagadas
              </h2>
              <div className="space-y-3">
                {pagadas.map((f) => (
                  <Card key={f.id} className="opacity-70">
                    <CardContent className="flex items-center justify-between py-4 flex-wrap gap-3">
                      <div className="flex items-center gap-3">
                        {servicioIcono[f.tipoServicio] || (
                          <Building2 size={20} />
                        )}
                        <div>
                          <p className="font-medium text-sm">{f.descripcion}</p>
                          <p className="text-xs text-muted-foreground">
                            Pagado:{" "}
                            {f.fechaPago
                              ? new Date(f.fechaPago).toLocaleDateString(
                                  "es-CO",
                                )
                              : "-"}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        {estadoBadge(f.estado)}
                        <span className="font-semibold">
                          {fmt.format(f.monto)}
                        </span>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Modal de pago */}
      <Dialog
        open={pagoModal.open}
        onOpenChange={(v) => setPagoModal((p) => ({ ...p, open: v }))}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Confirmar pago</DialogTitle>
          </DialogHeader>
          {pagoModal.factura && (
            <div className="space-y-4">
              <div className="p-4 bg-muted rounded-lg">
                <p className="font-medium">{pagoModal.factura.descripcion}</p>
                <p className="text-2xl font-bold mt-1">
                  {fmt.format(pagoModal.factura.monto)}
                </p>
              </div>

              <div>
                <Label>Método de pago</Label>
                <Select
                  value={metodoPago}
                  onValueChange={(v: any) => setMetodoPago(v)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="saldo">
                      <div className="flex items-center gap-2">
                        <Building2 size={14} /> Saldo de cuenta bancaria
                      </div>
                    </SelectItem>
                    <SelectItem
                      value="tarjeta"
                      disabled={tarjetas.length === 0}
                    >
                      <div className="flex items-center gap-2">
                        <CreditCard size={14} /> Tarjeta{" "}
                        {tarjetas.length === 0 ? "(sin tarjetas)" : ""}
                      </div>
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {metodoPago === "tarjeta" && tarjetas.length > 0 && (
                <div>
                  <Label>Seleccionar tarjeta</Label>
                  <Select
                    value={tarjetaSeleccionada}
                    onValueChange={setTarjetaSeleccionada}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {tarjetas.map((t) => (
                        <SelectItem key={t.id} value={t.id.toString()}>
                          {t.marca} ****{t.ultimosCuatro} ({t.tipoTarjeta}){" "}
                          {t.tipoTarjeta === "CREDITO"
                            ? `| Disponible: ${fmt.format(t.creditoDisponible)}`
                            : `| Saldo: ${fmt.format(t.saldoTarjeta)}`}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {tarjetaSeleccionada &&
                    (() => {
                      const t = tarjetas.find(
                        (x) => x.id.toString() === tarjetaSeleccionada,
                      );
                      if (!t) return null;
                      const balance =
                        t.tipoTarjeta === "CREDITO"
                          ? t.creditoDisponible
                          : t.saldoTarjeta;
                      const label =
                        t.tipoTarjeta === "CREDITO"
                          ? "Crédito disponible"
                          : "Saldo";
                      return (
                        <p className="text-xs text-muted-foreground mt-1">
                          {label}: <strong>{fmt.format(balance)}</strong>
                        </p>
                      );
                    })()}
                </div>
              )}

              {metodoPago === "saldo" && (
                <p className="text-sm text-muted-foreground">
                  Se descontará de tu cuenta{" "}
                  <strong>{user?.numeroCuenta}</strong>
                </p>
              )}

              <Button
                className="w-full"
                onClick={confirmarPago}
                disabled={pagando === pagoModal.factura.id}
              >
                {pagando === pagoModal.factura.id
                  ? "Procesando..."
                  : `Pagar ${fmt.format(pagoModal.factura.monto)}`}
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
