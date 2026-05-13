import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { isAdminRole } from "@/lib/roles";
import { mapEstadoToStatus } from "@/lib/utils";
import {
  actualizarEstadoTransaccion,
  obtenerTodasTransacciones,
  obtenerTransaccionesPendientes,
  TransaccionResponse,
} from "@/services/transaccionService";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { toast } from "sonner";

const API = import.meta.env.VITE_API_URL || "http://localhost:8080";

interface TarjetaPendiente {
  id: number;
  numDocumento: string;
  tipoTarjeta: string;
  marca: string;
  ultimosCuatro: string;
  nombreTitular: string;
  fechaExpiracion: string;
  estadoId: number;
  estadoNombre?: string;
  fechaCreacion: string;
}

const AdminPage = () => {
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState<TransaccionResponse[]>([]);
  const [all, setAll] = useState<TransaccionResponse[]>([]);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const [tarjetasPendientes, setTarjetasPendientes] = useState<TarjetaPendiente[]>([]);
  const [loadingTarjetas, setLoadingTarjetas] = useState(true);
  const [aprobarModal, setAprobarModal] = useState<{ open: boolean; tarjeta: TarjetaPendiente | null }>({
    open: false,
    tarjeta: null,
  });
  const [limiteCredito, setLimiteCredito] = useState("");
  const [procesandoTarjeta, setProcesandoTarjeta] = useState<number | null>(null);

  const adminDocumento = user?.numDocumento || "";

  const loadData = async () => {
    if (!adminDocumento) return;
    try {
      setLoading(true);
      const [pendientes, todas] = await Promise.all([
        obtenerTransaccionesPendientes(adminDocumento),
        obtenerTodasTransacciones(adminDocumento),
      ]);
      setPending(pendientes);
      setAll(todas);
    } catch (error) {
      const message = error instanceof Error ? error.message : "Error al cargar módulo de administración";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  const loadTarjetasPendientes = async () => {
    try {
      setLoadingTarjetas(true);
      const res = await fetch(`${API}/api/tarjetas/admin/pendientes`);
      const data = await res.json();
      setTarjetasPendientes(Array.isArray(data) ? data : []);
    } catch {
      toast.error("No se pudieron cargar las solicitudes de tarjetas");
    } finally {
      setLoadingTarjetas(false);
    }
  };

  useEffect(() => {
    if (!user || !isAdminRole(user.rol)) return;
    void loadData();
    void loadTarjetasPendientes();
  }, [user]);

  const handleCambiarEstado = async (id: number, estadoNombre: "APROBADA" | "RECHAZADA") => {
    try {
      setUpdatingId(id);
      await actualizarEstadoTransaccion(id, estadoNombre, adminDocumento);
      toast.success(estadoNombre === "APROBADA" ? "Transferencia aprobada" : "Transferencia rechazada");
      await loadData();
    } catch (error) {
      const message = error instanceof Error ? error.message : "No se pudo actualizar la transferencia";
      toast.error(message);
    } finally {
      setUpdatingId(null);
    }
  };

  const ejecutarAccionTarjeta = async (
    tarjetaId: number,
    endpoint: string,
    body: object,
    successMsg: string,
  ) => {
    setProcesandoTarjeta(tarjetaId);
    try {
      const res = await fetch(`${API}/api/tarjetas/${tarjetaId}/${endpoint}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Error");
      toast.success(successMsg);
      setAprobarModal({ open: false, tarjeta: null });
      setLimiteCredito("");
      void loadTarjetasPendientes();
    } catch (e: any) {
      toast.error(e.message);
    } finally {
      setProcesandoTarjeta(null);
    }
  };

  const handleAprobarTarjeta = async () => {
    if (!aprobarModal.tarjeta) return;
    const tarjeta = aprobarModal.tarjeta;
    const limite = tarjeta.tipoTarjeta === "CREDITO" ? parseFloat(limiteCredito) : undefined;
    if (tarjeta.tipoTarjeta === "CREDITO" && (!limite || limite <= 0)) {
      toast.error("Ingresa un límite de crédito válido");
      return;
    }
    await ejecutarAccionTarjeta(
      tarjeta.id,
      "aprobar",
      { limiteCredito: limite },
      `Tarjeta aprobada: ${tarjeta.marca} ****${tarjeta.ultimosCuatro}`,
    );
  };

  const handleRechazarTarjeta = (tarjeta: TarjetaPendiente) =>
    ejecutarAccionTarjeta(
      tarjeta.id,
      "rechazar",
      { motivo: "Solicitud rechazada por el administrador" },
      "Solicitud rechazada",
    );

  const stats = useMemo(() => ({
    pendientes: pending.length,
    aprobadas: all.filter((t) => t.estadoNombre === "APROBADA").length,
    rechazadas: all.filter((t) => t.estadoNombre === "RECHAZADA").length,
  }), [pending, all]);

  if (!user || !isAdminRole(user.rol)) {
    return <div className="text-sm text-muted-foreground">No autorizado.</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Administración</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Valida transferencias y solicitudes de tarjetas.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="rounded-xl bg-card border border-border p-4">
          <p className="text-xs text-muted-foreground">Transferencias pendientes</p>
          <p className="text-2xl font-bold">{stats.pendientes}</p>
        </div>
        <div className="rounded-xl bg-card border border-border p-4">
          <p className="text-xs text-muted-foreground">Transferencias aprobadas</p>
          <p className="text-2xl font-bold">{stats.aprobadas}</p>
        </div>
        <div className="rounded-xl bg-card border border-border p-4">
          <p className="text-xs text-muted-foreground">Solicitudes tarjetas</p>
          <p className="text-2xl font-bold">{tarjetasPendientes.length}</p>
        </div>
      </div>

      <TarjetasPendientesSection
        tarjetas={tarjetasPendientes}
        loading={loadingTarjetas}
        procesandoId={procesandoTarjeta}
        onRefresh={loadTarjetasPendientes}
        onAprobar={(t) => { setAprobarModal({ open: true, tarjeta: t }); setLimiteCredito(""); }}
        onRechazar={handleRechazarTarjeta}
      />

      <Dialog
        open={aprobarModal.open}
        onOpenChange={(o) => setAprobarModal((m) => ({ ...m, open: o }))}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Aprobar solicitud de tarjeta</DialogTitle>
          </DialogHeader>
          {aprobarModal.tarjeta && (
            <div className="space-y-4 pt-2">
              <p className="text-sm text-muted-foreground">
                <strong>{aprobarModal.tarjeta.nombreTitular}</strong> —{" "}
                {aprobarModal.tarjeta.tipoTarjeta} {aprobarModal.tarjeta.marca}{" "}
                ****{aprobarModal.tarjeta.ultimosCuatro}
              </p>
              {aprobarModal.tarjeta.tipoTarjeta === "CREDITO" && (
                <div>
                  <Label>Límite de crédito (COP)</Label>
                  <Input
                    type="number"
                    min="0"
                    placeholder="Ej: 5000000"
                    value={limiteCredito}
                    onChange={(e) => setLimiteCredito(e.target.value)}
                  />
                </div>
              )}
              {aprobarModal.tarjeta.tipoTarjeta === "DEBITO" && (
                <p className="text-sm text-muted-foreground">
                  Esta es una tarjeta débito. El usuario podrá recargar saldo
                  desde su cuenta bancaria.
                </p>
              )}
              <Button
                className="w-full"
                onClick={handleAprobarTarjeta}
                disabled={procesandoTarjeta === aprobarModal.tarjeta.id}
              >
                {procesandoTarjeta === aprobarModal.tarjeta.id ? "Aprobando..." : "Confirmar aprobación"}
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>

      <TransferenciasPendientesSection
        transferencias={pending}
        loading={loading}
        updatingId={updatingId}
        onRefresh={loadData}
        onCambiarEstado={handleCambiarEstado}
      />

      <TodasTransferenciasSection transferencias={all} />
    </div>
  );
};

export default AdminPage;

// ── Sub-componentes de sección ────────────────────────────────────────────────

interface TarjetasSectionProps {
  tarjetas: TarjetaPendiente[];
  loading: boolean;
  procesandoId: number | null;
  onRefresh: () => void;
  onAprobar: (t: TarjetaPendiente) => void;
  onRechazar: (t: TarjetaPendiente) => void;
}

function TarjetasPendientesSection({ tarjetas, loading, procesandoId, onRefresh, onAprobar, onRechazar }: TarjetasSectionProps) {
  return (
    <section className="bg-card rounded-2xl border border-border overflow-hidden">
      <div className="px-5 py-4 border-b border-border flex items-center justify-between">
        <h2 className="font-semibold">Solicitudes de Tarjetas Pendientes</h2>
        <Button variant="outline" onClick={onRefresh} disabled={loading}>Actualizar</Button>
      </div>
      {loading ? (
        <div className="p-8 text-center text-muted-foreground">Cargando solicitudes...</div>
      ) : tarjetas.length === 0 ? (
        <div className="p-8 text-center text-muted-foreground">No hay solicitudes de tarjetas pendientes.</div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Titular</TableHead>
              <TableHead>Documento</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead>Marca</TableHead>
              <TableHead>Últimos 4</TableHead>
              <TableHead>Expiración</TableHead>
              <TableHead>Fecha solicitud</TableHead>
              <TableHead className="text-center">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {tarjetas.map((t) => (
              <TableRow key={t.id}>
                <TableCell>{t.nombreTitular}</TableCell>
                <TableCell>{t.numDocumento}</TableCell>
                <TableCell><Badge variant="outline">{t.tipoTarjeta}</Badge></TableCell>
                <TableCell>{t.marca}</TableCell>
                <TableCell className="font-mono">****{t.ultimosCuatro}</TableCell>
                <TableCell>{t.fechaExpiracion}</TableCell>
                <TableCell>
                  {t.fechaCreacion ? new Date(t.fechaCreacion).toLocaleDateString("es-ES") : "-"}
                </TableCell>
                <TableCell className="text-center space-x-2">
                  <Button size="sm" disabled={procesandoId === t.id} onClick={() => onAprobar(t)}>
                    Aprobar
                  </Button>
                  <Button size="sm" variant="destructive" disabled={procesandoId === t.id} onClick={() => onRechazar(t)}>
                    Rechazar
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </section>
  );
}

interface TransferenciasPendientesSectionProps {
  transferencias: TransaccionResponse[];
  loading: boolean;
  updatingId: number | null;
  onRefresh: () => void;
  onCambiarEstado: (id: number, estado: "APROBADA" | "RECHAZADA") => void;
}

function TransferenciasPendientesSection({ transferencias, loading, updatingId, onRefresh, onCambiarEstado }: TransferenciasPendientesSectionProps) {
  return (
    <section className="bg-card rounded-2xl border border-border overflow-hidden">
      <div className="px-5 py-4 border-b border-border flex items-center justify-between">
        <h2 className="font-semibold">Transferencias pendientes de validación</h2>
        <Button variant="outline" onClick={onRefresh} disabled={loading}>Actualizar</Button>
      </div>
      {loading ? (
        <div className="p-8 text-center text-muted-foreground">Cargando pendientes...</div>
      ) : transferencias.length === 0 ? (
        <div className="p-8 text-center text-muted-foreground">No hay transferencias pendientes.</div>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Fecha</TableHead>
              <TableHead>Origen</TableHead>
              <TableHead>Destino</TableHead>
              <TableHead>Tipo</TableHead>
              <TableHead className="text-right">Monto</TableHead>
              <TableHead className="text-center">Acciones</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {transferencias.map((txn) => (
              <TableRow key={txn.id}>
                <TableCell>
                  {txn.fechaCreacion ? new Date(txn.fechaCreacion).toLocaleString("es-ES") : "-"}
                </TableCell>
                <TableCell>{txn.cuentaOrigenId}</TableCell>
                <TableCell>{txn.cuentaDestinoId}</TableCell>
                <TableCell>
                  <span className="inline-flex items-center rounded-full bg-secondary px-2 py-0.5 text-xs font-medium">
                    {txn.tipoTransaccionNombre ?? "-"}
                  </span>
                </TableCell>
                <TableCell className="text-right font-semibold">
                  ${txn.monto.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
                </TableCell>
                <TableCell className="text-center space-x-2">
                  <Button size="sm" disabled={updatingId === txn.id} onClick={() => onCambiarEstado(txn.id, "APROBADA")}>
                    Aprobar
                  </Button>
                  <Button size="sm" variant="destructive" disabled={updatingId === txn.id} onClick={() => onCambiarEstado(txn.id, "RECHAZADA")}>
                    Rechazar
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </section>
  );
}

function TodasTransferenciasSection({ transferencias }: { transferencias: TransaccionResponse[] }) {
  return (
    <section className="bg-card rounded-2xl border border-border overflow-hidden">
      <div className="px-5 py-4 border-b border-border">
        <h2 className="font-semibold">Todas las transferencias</h2>
      </div>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Fecha</TableHead>
            <TableHead>Origen</TableHead>
            <TableHead>Destino</TableHead>
            <TableHead>Tipo</TableHead>
            <TableHead className="text-right">Monto</TableHead>
            <TableHead className="text-center">Estado</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {transferencias.map((txn) => (
            <TableRow key={`all-${txn.id}`}>
              <TableCell>
                {txn.fechaCreacion ? new Date(txn.fechaCreacion).toLocaleString("es-ES") : "-"}
              </TableCell>
              <TableCell>{txn.cuentaOrigenId}</TableCell>
              <TableCell>{txn.cuentaDestinoId}</TableCell>
              <TableCell>
                <span className="inline-flex items-center rounded-full bg-secondary px-2 py-0.5 text-xs font-medium">
                  {txn.tipoTransaccionNombre ?? "-"}
                </span>
              </TableCell>
              <TableCell className="text-right font-semibold">
                ${txn.monto.toLocaleString("es-MX", { minimumFractionDigits: 2 })}
              </TableCell>
              <TableCell className="text-center">
                <StatusBadge status={mapEstadoToStatus(txn.estadoNombre)} />
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </section>
  );
}
