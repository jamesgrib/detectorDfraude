import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import { AuthProvider } from "@/hooks/useAuth";
import Login from "./pages/Login";
import Register from "./pages/Register";
import AppLayout from "./components/AppLayout";
import Dashboard from "./pages/Dashboard";
import Transfer from "./pages/Transfer";
import HistoryPage from "./pages/History";
import AdminPage from "./pages/Admin";
import ReportsPage from "./pages/Reports";
import TarjetasPage from "./pages/Tarjetas";
import FacturasPage from "./pages/Facturas";
import NotFound from "./pages/NotFound";
import { useAuth } from "./hooks/useAuth";
import { isAdminRole } from "./lib/roles";

const queryClient = new QueryClient();

const AdminRoute = () => {
  const { user } = useAuth();

  if (!user) {
    return <Navigate to="/" replace />;
  }

  return isAdminRole(user.rol) ? (
    <AdminPage />
  ) : (
    <Navigate to="/dashboard" replace />
  );
};

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/transfer" element={<Transfer />} />
              <Route path="/history" element={<HistoryPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/tarjetas" element={<TarjetasPage />} />
              <Route path="/facturas" element={<FacturasPage />} />
              <Route path="/admin" element={<AdminRoute />} />
            </Route>
            <Route path="*" element={<NotFound />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
