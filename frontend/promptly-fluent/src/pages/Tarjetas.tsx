import { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { toast } from "@/hooks/use-toast";
import {
  CreditCard,
  Plus,
  Trash2,
  Wifi,
  Clock,
  CheckCircle2,
  XCircle,
  PlusCircle,
} from "lucide-react";

const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

interface Tarjeta {
  id: number;
  tipoTarjeta: string;
  marca: string;
  ultimosCuatro: string;
  nombreTitular: string;
  fechaExpiracion: string;
  estadoId: number;
  limiteCredito: number;
  creditoDisponible: number;
  saldoTarjeta: number;
  numDocumento?: string;
}

const marcaColor: Record<string, string> = {
  VISA: "bg-blue-600",
  MASTERCARD: "bg-red-600",
  AMEX: "bg-green-600",
  UNKNOWN: "bg-gray-600",
};

const fmt = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0,
});

function estadoBadge(estadoId: number) {
  if (estadoId === 1)
    return (
      <Badge className="bg-green-100 text-green-700 border-green-300 gap-1">
        <CheckCircle2 size={11} /> Activa
      </Badge>
    );
  if (estadoId === 2)
    return (
      <Badge className="bg-amber-100 text-amber-700 border-amber-300 gap-1">
        <Clock size={11} /> Pendiente aprobación
      </Badge>
    );
  if (estadoId === 4)
    return (
      <Badge className="bg-red-100 text-red-700 border-red-300 gap-1">
        <XCircle size={11} /> Rechazada
      </Badge>
    );
  return null;
}

export default function Tarjetas() {
  const { user } = useAuth();
  const [tarjetas, setTarjetas] = useState<Tarjeta[]>([]);
  const [loading, setLoading] = useState(true);
  const [openSolicitud, setOpenSolicitud] = useState(false);
  const [guardando, setGuardando] = useState(false);

  // Recarga debito
  const [recargarModal, setRecargarModal] = useState<{
    open: boolean;
    tarjeta: Tarjeta | null;
  }>({
    open: false,
    tarjeta: null,
  });
  const [montoRecarga, setMontoRecarga] = useState("");
  const [recargando, setRecargando] = useState(false);

  const [form, setForm] = useState({
    tipoTarjeta: "DEBITO",
    nombreTitular: "",
  });

  const cargarTarjetas = async () => {
    if (!user) return;
    try {
      const res = await fetch(`${API}/api/tarjetas`, {
        headers: { "X-User-Documento": user.numDocumento },
      });
      const data = await res.json();
      // Excluir tarjetas eliminadas (estadoId=3)
      const activas = Array.isArray(data)
        ? data.filter((t: Tarjeta) => t.estadoId !== 3)
        : [];
      setTarjetas(activas);
    } catch {
      toast({
        title: "Error",
        description: "No se pudieron cargar las tarjetas",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user) {
      setForm((f) => ({ ...f, nombreTitular: user.nombreCompleto }));
      cargarTarjetas();
    }
  }, [user]);

  const solicitarTarjeta = async () => {
    if (!user) return;
    if (!form.nombreTitular) {
      toast({
        title: "Campos incompletos",
        description: "Ingresa el nombre del titular",
        variant: "destructive",
      });
      return;
    }
    setGuardando(true);
    try {
      const res = await fetch(`${API}/api/tarjetas`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-User-Documento": user.numDocumento,
        },
        body: JSON.stringify({
          nombreTitular: form.nombreTitular,
          tipoTarjeta: form.tipoTarjeta,
        }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Error al solicitar tarjeta");
      toast({
        title: "Solicitud enviada",
        description:
          "Tu tarjeta está pendiente de aprobación por el administrador.",
      });
      setOpenSolicitud(false);
      setForm({
        tipoTarjeta: "DEBITO",
        nombreTitular: user.nombreCompleto,
      });
      cargarTarjetas();
    } catch (e: any) {
      toast({ title: "Error", description: e.message, variant: "destructive" });
    } finally {
      setGuardando(false);
    }
  };

  const eliminarTarjeta = async (id: number) => {
    if (!user) return;
    if (!confirm("¿Deseas cancelar / eliminar esta tarjeta?")) return;
    try {
      const res = await fetch(`${API}/api/tarjetas/${id}`, {
        method: "DELETE",
        headers: { "X-User-Documento": user.numDocumento },
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
      toast({ title: "Tarjeta eliminada" });
      cargarTarjetas();
    } catch (e: any) {
      toast({ title: "Error", description: e.message, variant: "destructive" });
    }
  };

  const recargarDebito = async () => {
    if (!user || !recargarModal.tarjeta) return;
    const monto = parseFloat(montoRecarga);
    if (!monto || monto <= 0) {
      toast({
        title: "Monto inválido",
        description: "Ingresa un monto mayor a 0",
        variant: "destructive",
      });
      return;
    }
    setRecargando(true);
    try {
      const res = await fetch(
        `${API}/api/tarjetas/${recargarModal.tarjeta.id}/recargar`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-User-Documento": user.numDocumento,
          },
          body: JSON.stringify({ monto, numeroCuenta: user.numeroCuenta }),
        },
      );
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Error al recargar");
      toast({
        title: "Recarga exitosa",
        description: `Nuevo saldo: ${fmt.format(data.nuevoSaldo)}`,
      });
      setRecargarModal({ open: false, tarjeta: null });
      setMontoRecarga("");
      cargarTarjetas();
    } catch (e: any) {
      toast({ title: "Error", description: e.message, variant: "destructive" });
    } finally {
      setRecargando(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Mis Tarjetas</h1>
          <p className="text-muted-foreground text-sm mt-1">
            Solicita tarjetas de crédito o débito. Un administrador las
            aprobará.
          </p>
        </div>
        <Dialog open={openSolicitud} onOpenChange={setOpenSolicitud}>
          <DialogTrigger asChild>
            <Button className="gap-2">
              <Plus size={16} /> Solicitar tarjeta
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Solicitar nueva tarjeta</DialogTitle>
            </DialogHeader>
            <div className="space-y-4 pt-2">
              <p className="text-sm text-muted-foreground">
                Tu solicitud será revisada por un administrador. Recibirás la
                aprobación en la sección de tarjetas.
              </p>
              <div>
                <Label>Tipo de tarjeta</Label>
                <Select
                  value={form.tipoTarjeta}
                  onValueChange={(v) =>
                    setForm((f) => ({ ...f, tipoTarjeta: v }))
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DEBITO">Débito</SelectItem>
                    <SelectItem value="CREDITO">Crédito</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label>Nombre del titular</Label>
                <Input
                  value={form.nombreTitular}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, nombreTitular: e.target.value }))
                  }
                />
              </div>
              <Button
                className="w-full"
                onClick={solicitarTarjeta}
                disabled={guardando}
              >
                {guardando ? "Enviando solicitud..." : "Enviar solicitud"}
              </Button>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      {loading ? (
        <div className="text-center py-12 text-muted-foreground">
          Cargando tarjetas...
        </div>
      ) : tarjetas.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center py-16 text-center gap-3">
            <CreditCard size={48} className="text-muted-foreground/40" />
            <p className="text-muted-foreground">
              No tienes tarjetas registradas
            </p>
            <Button variant="outline" onClick={() => setOpenSolicitud(true)}>
              <Plus size={14} className="mr-1" /> Solicitar primera tarjeta
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {tarjetas.map((t) => (
            <div key={t.id} className="relative">
              {/* Card visual */}
              <div
                className={`relative rounded-2xl p-5 text-white shadow-lg overflow-hidden ${
                  t.estadoId === 1
                    ? marcaColor[t.marca] || "bg-gray-700"
                    : t.estadoId === 4
                      ? "bg-gray-400"
                      : "bg-gray-500"
                }`}
              >
                <div className="absolute -top-6 -right-6 w-32 h-32 bg-white/10 rounded-full" />
                <div className="absolute -bottom-8 -right-2 w-24 h-24 bg-white/10 rounded-full" />

                <div className="flex justify-between items-start">
                  <div className="flex gap-2 items-center">
                    <Badge
                      variant="outline"
                      className="text-white border-white/50 text-xs"
                    >
                      {t.tipoTarjeta}
                    </Badge>
                    {estadoBadge(t.estadoId)}
                  </div>
                  <Wifi size={18} className="opacity-70 rotate-90" />
                </div>

                {t.estadoId === 2 && (
                  <div className="mt-3 text-xs bg-white/20 rounded px-2 py-1 text-white/90">
                    En espera de aprobación por el administrador
                  </div>
                )}
                {t.estadoId === 4 && (
                  <div className="mt-3 text-xs bg-white/20 rounded px-2 py-1 text-white/90">
                    Solicitud rechazada
                  </div>
                )}

                <div className="mt-4 font-mono text-lg tracking-widest">
                  **** **** **** {t.ultimosCuatro}
                </div>

                {/* Saldo/crédito — solo tarjetas activas */}
                {t.estadoId === 1 && (
                  <div className="mt-2 text-xs bg-white/15 rounded px-2 py-1">
                    {t.tipoTarjeta === "CREDITO" ? (
                      <span>
                        Límite: {fmt.format(t.limiteCredito)} &nbsp;|&nbsp;
                        Disponible:{" "}
                        <strong>{fmt.format(t.creditoDisponible)}</strong>
                      </span>
                    ) : (
                      <span>
                        Saldo: <strong>{fmt.format(t.saldoTarjeta)}</strong>
                      </span>
                    )}
                  </div>
                )}

                <div className="mt-3 flex justify-between items-end">
                  <div>
                    <p className="text-xs opacity-70">Titular</p>
                    <p className="text-sm font-semibold">{t.nombreTitular}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs opacity-70">Exp.</p>
                    <p className="text-sm">{t.fechaExpiracion}</p>
                  </div>
                  <p className="text-sm font-bold tracking-wide">{t.marca}</p>
                </div>

                {/* Botón recargar débito activa */}
                {t.estadoId === 1 && t.tipoTarjeta === "DEBITO" && (
                  <button
                    onClick={() => {
                      setRecargarModal({ open: true, tarjeta: t });
                      setMontoRecarga("");
                    }}
                    className="absolute bottom-3 left-3 flex items-center gap-1 text-xs bg-white/20 hover:bg-white/30 px-2 py-1 rounded transition"
                    title="Recargar saldo"
                  >
                    <PlusCircle size={12} /> Recargar
                  </button>
                )}

                <button
                  onClick={() => eliminarTarjeta(t.id)}
                  className="absolute top-3 right-3 p-1 rounded-full hover:bg-white/20 transition"
                  title="Eliminar tarjeta"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal recarga débito */}
      <Dialog
        open={recargarModal.open}
        onOpenChange={(o) => setRecargarModal((m) => ({ ...m, open: o }))}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Recargar tarjeta débito</DialogTitle>
          </DialogHeader>
          {recargarModal.tarjeta && (
            <div className="space-y-4 pt-2">
              <p className="text-sm text-muted-foreground">
                Tarjeta {recargarModal.tarjeta.marca} ****
                {recargarModal.tarjeta.ultimosCuatro}
                <br />
                Saldo actual:{" "}
                <strong>
                  {fmt.format(recargarModal.tarjeta.saldoTarjeta)}
                </strong>
              </p>
              <p className="text-sm text-muted-foreground">
                El dinero se deducirá de tu cuenta bancaria (
                {user?.numeroCuenta}).
              </p>
              <div>
                <Label>Monto a recargar (COP)</Label>
                <Input
                  type="number"
                  min="1000"
                  placeholder="Ej: 200000"
                  value={montoRecarga}
                  onChange={(e) => setMontoRecarga(e.target.value)}
                />
              </div>
              <Button
                className="w-full"
                onClick={recargarDebito}
                disabled={recargando}
              >
                {recargando ? "Recargando..." : "Confirmar recarga"}
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
