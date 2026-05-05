import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import {
  ArrowUpRight,
  ArrowDownLeft,
  Wallet,
  TrendingUp,
  AlertCircle,
} from "lucide-react";
import { StatusBadge } from "@/components/StatusBadge";
import { Button } from "@/components/ui/button";
import { motion } from "framer-motion";
import { toast } from "sonner";

// Definimos la interfaz para las transacciones que vienen de Java
interface Transaccion {
  id: number;
  monto: number;
  cuentaOrigenId: string;
  cuentaDestinoId: string;
  nombreDestino: string;
  nombreOrigen: string;
  fechaCreacion: string;
  estadoNombre: string; // PENDIENTE | APROBADA | RECHAZADA
}

const Dashboard = () => {
  const { user } = useAuth();
  const [transacciones, setTransacciones] = useState<Transaccion[]>([]);
  const [saldo, setSaldo] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const getStatus = (
    estadoNombre: string,
  ): "approved" | "rejected" | "pending" => {
    if (estadoNombre === "APROBADA") return "approved";
    if (estadoNombre === "RECHAZADA") return "rejected";
    return "pending";
  };

  // 1. Cargar transacciones y saldo reales desde el backend
  useEffect(() => {
    const fetchDatos = async () => {
      if (!user) {
        setLoading(false);
        return;
      }

      setError(null);
      try {
        console.log(
          "📡 Cargando transacciones para cuenta:",
          user.numeroCuenta,
        );

        // Fetch transacciones
        const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";
        const transResponse = await fetch(
          `${API_BASE}/api/transacciones/cuenta/${user.numeroCuenta}`,
        );

        console.log("📡 Respuesta transacciones:", transResponse.status);

        if (transResponse.ok) {
          const transData = await transResponse.json();
          console.log("📡 Transacciones recibidas:", transData);
          setTransacciones(transData);
        } else {
          console.warn(`Transacciones no disponibles: ${transResponse.status}`);
          setTransacciones([]);
        }

        // Usar saldo del usuario (viene del login)
        console.log("💰 Saldo del usuario:", user.saldo);
        setSaldo(user.saldo || 0);
      } catch (error) {
        console.error("Error al cargar datos:", error);
        setError(
          "No se pudieron cargar las transacciones. Verifica que el backend esté ejecutándose.",
        );
        console.warn("Continuando con datos vacíos...");
        setTransacciones([]);
        setSaldo(user.saldo || 0);
      } finally {
        setLoading(false);
      }
    };

    fetchDatos();
  }, [user]);

  // Mostrar spinner de carga solo si user existe y está cargando
  if (loading)
    return (
      <div className="flex h-96 items-center justify-center">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ repeat: Infinity, duration: 1 }}
          className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full"
        />
      </div>
    );

  if (!user)
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-center space-y-4">
          <AlertCircle className="h-12 w-12 text-red-500 mx-auto" />
          <h2 className="text-xl font-semibold text-foreground">
            No autenticado
          </h2>
          <p className="text-muted-foreground">
            Por favor inicia sesión primero
          </p>
        </div>
      </div>
    );

  // 2. Lógica de filtrado y estadísticas
  const recientes = transacciones.slice(0, 5);
  const pendientesCount = transacciones.filter(
    (t) => t.estadoNombre === "PENDIENTE",
  ).length;

  const stats = [
    {
      label: "Saldo Disponible",
      value: `$${saldo.toLocaleString("es-MX", { minimumFractionDigits: 2 })}`,
      icon: Wallet,
      color: "text-green-600",
      bgColor: "bg-green-100",
    },
    {
      label: "Mis Movimientos",
      value: transacciones.length.toString(),
      icon: ArrowUpRight,
      color: "text-blue-600",
      bgColor: "bg-blue-100",
    },
    {
      label: "En Revisión",
      value: pendientesCount.toString(),
      icon: AlertCircle,
      color: "text-orange-600",
      bgColor: "bg-orange-100",
    },
  ];

  return (
    <div className="space-y-8 p-4">
      {/* Mostrar alerta si hay error de conexión */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 mt-0.5 flex-shrink-0" />
          <div>
            <h3 className="font-semibold text-red-900">
              Advertencia de conexión
            </h3>
            <p className="text-sm text-red-700">{error}</p>
          </div>
        </div>
      )}

      {/* Header Dinámico */}
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-2xl font-bold text-foreground">
            Hola, {user.nombreCompleto.split(" ")[0]} 👋
          </h1>
          <p className="text-muted-foreground text-sm mt-1">
            Cuenta: {user.numeroCuenta}
          </p>
        </div>
        <div className="text-right">
          <span className="text-[10px] font-bold uppercase tracking-wider bg-secondary px-2 py-1 rounded shadow-sm">
            Nivel: {user.rol}
          </span>
        </div>
      </div>

      {/* Grid de Tarjetas con Animación */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        {stats.map((stat, i) => (
          <motion.div
            key={stat.label}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
            className="bg-card rounded-2xl p-6 shadow-sm border border-border hover:shadow-md transition-shadow"
          >
            <div className="flex items-center justify-between mb-4">
              <span className="text-sm text-muted-foreground font-medium">
                {stat.label}
              </span>
              <div
                className={`w-10 h-10 rounded-xl ${stat.bgColor} flex items-center justify-center`}
              >
                <stat.icon className={`h-5 w-5 ${stat.color}`} />
              </div>
            </div>
            <p className="text-2xl font-bold text-foreground">{stat.value}</p>
          </motion.div>
        ))}
      </div>

      {/* Tabla de Movimientos Reales */}
      <div className="bg-card rounded-2xl shadow-sm border border-border overflow-hidden">
        <div className="px-6 py-4 border-b border-border flex justify-between items-center">
          <h2 className="font-semibold text-foreground">Actividad Reciente</h2>
          <button className="text-xs text-primary font-medium hover:underline">
            Ver todo
          </button>
        </div>

        <div className="divide-y divide-border">
          {recientes.length > 0 ? (
            recientes.map((txn) => {
              const isOutgoing = txn.cuentaOrigenId === user.numeroCuenta;
              return (
                <div
                  key={txn.id}
                  className="px-6 py-4 flex items-center gap-4 hover:bg-secondary/20 transition-colors"
                >
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center ${isOutgoing ? "bg-red-50" : "bg-green-50"}`}
                  >
                    {isOutgoing ? (
                      <ArrowUpRight className="h-5 w-5 text-red-500" />
                    ) : (
                      <ArrowDownLeft className="h-5 w-5 text-green-500" />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-semibold text-foreground truncate">
                      {isOutgoing
                        ? `A: ${txn.nombreDestino}`
                        : `De: ${txn.nombreOrigen}`}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {txn.fechaCreacion}
                    </p>
                  </div>
                  <div className="text-right">
                    <p
                      className={`text-sm font-bold ${isOutgoing ? "text-red-600" : "text-green-600"}`}
                    >
                      {isOutgoing ? "-" : "+"}$
                      {txn.monto.toLocaleString("es-MX")}
                    </p>
                    <StatusBadge status={getStatus(txn.estadoNombre)} />
                  </div>
                </div>
              );
            })
          ) : (
            <div className="p-12 text-center text-muted-foreground italic">
              No se encontraron movimientos registrados.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
