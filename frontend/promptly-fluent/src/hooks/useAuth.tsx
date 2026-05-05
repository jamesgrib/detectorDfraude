import React, {
  createContext,
  useContext,
  useState,
  ReactNode,
  useEffect,
} from "react";

// 1. Ajustamos la interfaz User para que coincida con tu LoginResponse de Java
export interface User {
  id: number; // En tu Java es Integer
  nombreCompleto: string;
  username: string;
  numDocumento: string;
  rol: string;
  saldo: number;
  numeroCuenta: string;
}

interface AuthContextType {
  user: User | null;
  login: (numDocumento: string, password: string) => Promise<boolean>;
  logout: () => void;
  isAuthenticated: boolean;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | null>(null);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // 2. Efecto para recuperar la sesión al recargar la página (F5)
  useEffect(() => {
    const savedUser = localStorage.getItem("user_session");
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
    setLoading(false);
  }, []);

  // 3. Función Login conectada al backend
  const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";
  const login = async (numDocumento: string, password: string): Promise<boolean> => {
    try {
      console.log("📡 Intentando login con:", { numDocumento, password });
      
      const response = await fetch(`${API_BASE}/api/usuarios/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          numDocumento: numDocumento,
          password: password,
        }),
      });

      console.log("📡 Respuesta del servidor:", response.status, response.statusText);
      
      const data = await response.json();
      console.log("📡 Datos recibidos:", data);

      if (data.success) {
        // Crear objeto User con los datos del backend
        const user: User = {
          id: 0,
          nombreCompleto: data.nombre,
          username: data.email,
          numDocumento,
          rol: data.rol || "CLIENTE",
          saldo: data.saldo ? parseFloat(data.saldo) : 0, // Convertir BigDecimal a número
          numeroCuenta: data.numeroCuenta || numDocumento,
        };

        console.log("✅ Login exitoso. Usuario:", user);
        
        // Guardamos en el estado y en el almacenamiento local
        setUser(user);
        localStorage.setItem("user_session", JSON.stringify(user));
        return true;
      } else {
        console.error("❌ Login fallido:", data.mensaje);
        return false;
      }
    } catch (error) {
      console.error("❌ Error de conexión:", error);
      return false;
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem("user_session");
  };

  return (
    <AuthContext.Provider
      value={{ user, login, logout, isAuthenticated: !!user, loading }}
    >
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
