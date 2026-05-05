import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { isAdminRole } from "@/lib/roles";
import { motion } from "framer-motion";
import {
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from "recharts";
import {
  TrendingUp,
  Users,
  Wallet,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Clock,
  BarChart2,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";

const BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

interface Resumen {
  totalTransacciones: number;
  aprobadas: number;
  rechazadas: number;
  pendientes: number;
  montoAprobado: number;
  montoRechazado: number;
  montoPendiente: number;
  totalClientes: number;
  totalCuentas: number;
  tasaFraudePorc: number;
}

interface DistribucionItem {
  estado: string;
  cantidad: number;
  color: string;
}

interface TopCuenta {
  cuenta: string;
  totalTransacciones: number;
  montoTotal: number;
}

interface ReporteCuenta {
  numeroCuenta: string;
  totalOperaciones: number;
  transaccionesEnviadas: number;
  transaccionesRecibidas: number;
  montoEnviado: number;
  montoRecibido: number;
}

const fmt = (n: number) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(n);

export default function Reports() {
  const { user } = useAuth();
  const isAdmin = isAdminRole(user?.rol);

  const [resumen, setResumen] = useState<Resumen | null>(null);
  const [distribucion, setDistribucion] = useState<DistribucionItem[]>([]);
  const [topCuentas, setTopCuentas] = useState<TopCuenta[]>([]);
  const [reporteCuenta, setReporteCuenta] = useState<ReporteCuenta | null>(
    null,
  );
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const cargar = async () => {
      setLoading(true);
      try {
        if (isAdmin) {
          const headers = { "X-Admin-Documento": user!.numDocumento };

          const [resRes, distRes, topRes] = await Promise.all([
            fetch(`${BASE}/api/reportes/resumen`, { headers }),
            fetch(`${BASE}/api/reportes/distribucion`, { headers }),
            fetch(`${BASE}/api/reportes/top-cuentas`, { headers }),
          ]);

          if (resRes.ok) setResumen(await resRes.json());
          if (distRes.ok) setDistribucion(await distRes.json());
          if (topRes.ok) setTopCuentas(await topRes.json());
        } else if (user?.numeroCuenta) {
          const res = await fetch(
            `${BASE}/api/reportes/cuenta/${user.numeroCuenta}`,
          );
          if (res.ok) setReporteCuenta(await res.json());
        }
      } catch {
        toast.error("Error al cargar reportes");
      } finally {
        setLoading(false);
      }
    };
    if (user) cargar();
  }, [user, isAdmin]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary" />
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35 }}
      className="p-6 space-y-6 max-w-6xl mx-auto"
    >
      <div>
        <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
          <BarChart2 className="h-6 w-6 text-primary" />
          Reportes
        </h1>
        <p className="text-muted-foreground text-sm mt-1">
          {isAdmin
            ? "Panel de actividad del sistema bancario"
            : "Actividad de tu cuenta"}
        </p>
      </div>

      {/* ── VISTA ADMIN ─────────────────────────────────────────────────────── */}
      {isAdmin && resumen && (
        <>
          {/* KPI Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <KpiCard
              icon={<TrendingUp className="h-5 w-5 text-blue-500" />}
              label="Total transacciones"
              value={resumen.totalTransacciones.toString()}
              bg="bg-blue-50 dark:bg-blue-950/30"
            />
            <KpiCard
              icon={<CheckCircle className="h-5 w-5 text-emerald-500" />}
              label="Aprobadas"
              value={`${resumen.aprobadas} (${fmt(resumen.montoAprobado)})`}
              bg="bg-emerald-50 dark:bg-emerald-950/30"
            />
            <KpiCard
              icon={<XCircle className="h-5 w-5 text-red-500" />}
              label="Rechazadas"
              value={`${resumen.rechazadas} (${fmt(resumen.montoRechazado)})`}
              bg="bg-red-50 dark:bg-red-950/30"
            />
            <KpiCard
              icon={<Clock className="h-5 w-5 text-amber-500" />}
              label="Pendientes"
              value={`${resumen.pendientes} (${fmt(resumen.montoPendiente)})`}
              bg="bg-amber-50 dark:bg-amber-950/30"
            />
            <KpiCard
              icon={<Users className="h-5 w-5 text-violet-500" />}
              label="Clientes registrados"
              value={resumen.totalClientes.toString()}
              bg="bg-violet-50 dark:bg-violet-950/30"
            />
            <KpiCard
              icon={<Wallet className="h-5 w-5 text-sky-500" />}
              label="Cuentas activas"
              value={resumen.totalCuentas.toString()}
              bg="bg-sky-50 dark:bg-sky-950/30"
            />
            <KpiCard
              icon={<AlertTriangle className="h-5 w-5 text-orange-500" />}
              label="Tasa de fraude"
              value={`${resumen.tasaFraudePorc}%`}
              bg="bg-orange-50 dark:bg-orange-950/30"
            />
          </div>

          {/* Gráficos */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Pie: distribución por estado */}
            <Card>
              <CardHeader>
                <CardTitle className="text-base">
                  Distribución por estado
                </CardTitle>
              </CardHeader>
              <CardContent>
                {distribucion.every((d) => d.cantidad === 0) ? (
                  <p className="text-muted-foreground text-sm text-center py-8">
                    Sin datos aún
                  </p>
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie
                        data={distribucion}
                        dataKey="cantidad"
                        nameKey="estado"
                        cx="50%"
                        cy="50%"
                        outerRadius={90}
                        label={({ estado, percent }) =>
                          percent > 0
                            ? `${estado} ${(percent * 100).toFixed(0)}%`
                            : ""
                        }
                      >
                        {distribucion.map((entry, i) => (
                          <Cell key={i} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        formatter={(v: number) => [`${v} transacciones`]}
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            {/* Bar: top cuentas */}
            <Card>
              <CardHeader>
                <CardTitle className="text-base">
                  Top cuentas por actividad (aprobadas)
                </CardTitle>
              </CardHeader>
              <CardContent>
                {topCuentas.length === 0 ? (
                  <p className="text-muted-foreground text-sm text-center py-8">
                    Sin datos aún
                  </p>
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart
                      data={topCuentas}
                      margin={{ top: 4, right: 8, left: 8, bottom: 4 }}
                    >
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="hsl(var(--border))"
                      />
                      <XAxis dataKey="cuenta" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} />
                      <Tooltip
                        formatter={(v: number, name: string) =>
                          name === "montoTotal"
                            ? [fmt(v), "Monto total"]
                            : [v, "Transacciones"]
                        }
                      />
                      <Legend />
                      <Bar
                        dataKey="totalTransacciones"
                        fill="#6366f1"
                        name="Transacciones"
                        radius={[4, 4, 0, 0]}
                      />
                      <Bar
                        dataKey="montoTotal"
                        fill="#10b981"
                        name="montoTotal"
                        radius={[4, 4, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </div>
        </>
      )}

      {/* ── VISTA USUARIO ───────────────────────────────────────────────────── */}
      {!isAdmin && reporteCuenta && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="grid grid-cols-2 gap-4">
            <KpiCard
              icon={<TrendingUp className="h-5 w-5 text-blue-500" />}
              label="Total operaciones"
              value={reporteCuenta.totalOperaciones.toString()}
              bg="bg-blue-50 dark:bg-blue-950/30"
            />
            <KpiCard
              icon={<CheckCircle className="h-5 w-5 text-emerald-500" />}
              label="Recibidas"
              value={`${reporteCuenta.transaccionesRecibidas}`}
              bg="bg-emerald-50 dark:bg-emerald-950/30"
            />
            <KpiCard
              icon={<XCircle className="h-5 w-5 text-red-500" />}
              label="Enviadas"
              value={`${reporteCuenta.transaccionesEnviadas}`}
              bg="bg-red-50 dark:bg-red-950/30"
            />
            <KpiCard
              icon={<Wallet className="h-5 w-5 text-sky-500" />}
              label="Balance neto"
              value={fmt(
                reporteCuenta.montoRecibido - reporteCuenta.montoEnviado,
              )}
              bg="bg-sky-50 dark:bg-sky-950/30"
            />
          </div>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">
                Montos enviados vs recibidos
              </CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={220}>
                <BarChart
                  data={[
                    { tipo: "Enviado", monto: reporteCuenta.montoEnviado },
                    { tipo: "Recibido", monto: reporteCuenta.montoRecibido },
                  ]}
                >
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="hsl(var(--border))"
                  />
                  <XAxis dataKey="tipo" />
                  <YAxis tickFormatter={(v) => `$${(v / 1000).toFixed(0)}k`} />
                  <Tooltip formatter={(v: number) => [fmt(v), "Monto"]} />
                  <Bar dataKey="monto" radius={[6, 6, 0, 0]}>
                    <Cell fill="#ef4444" />
                    <Cell fill="#10b981" />
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>
        </div>
      )}

      {!isAdmin && !reporteCuenta && (
        <p className="text-muted-foreground text-center py-16">
          No hay datos de cuenta disponibles.
        </p>
      )}
    </motion.div>
  );
}

function KpiCard({
  icon,
  label,
  value,
  bg,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  bg: string;
}) {
  return (
    <Card className={`${bg} border-0`}>
      <CardContent className="pt-4 pb-4">
        <div className="flex items-center gap-3">
          <div className="rounded-lg bg-white/60 dark:bg-black/20 p-2">
            {icon}
          </div>
          <div>
            <p className="text-xs text-muted-foreground leading-tight">
              {label}
            </p>
            <p className="font-semibold text-sm text-foreground leading-tight mt-0.5">
              {value}
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
