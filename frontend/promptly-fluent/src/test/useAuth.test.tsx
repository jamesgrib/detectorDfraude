import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { render, screen, act, waitFor } from "@testing-library/react";
import { AuthProvider, useAuth } from "../hooks/useAuth";

// Componente auxiliar para exponer el contexto en tests
function TestConsumer() {
  const { user, isAuthenticated, loading } = useAuth();
  return (
    <div>
      <span data-testid="loading">{String(loading)}</span>
      <span data-testid="authenticated">{String(isAuthenticated)}</span>
      <span data-testid="user">{user ? user.nombreCompleto : "null"}</span>
    </div>
  );
}

describe("useAuth", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it("estado inicial: no autenticado, sin usuario", async () => {
    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading").textContent).toBe("false");
    });

    expect(screen.getByTestId("authenticated").textContent).toBe("false");
    expect(screen.getByTestId("user").textContent).toBe("null");
  });

  it("recupera sesión guardada en localStorage", async () => {
    const savedUser = {
      id: 1,
      nombreCompleto: "Juan Pérez",
      username: "juan@test.com",
      numDocumento: "12345678",
      rol: "USER",
      saldo: 500000,
      numeroCuenta: "ACC-001",
    };
    localStorage.setItem("user_session", JSON.stringify(savedUser));

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading").textContent).toBe("false");
    });

    expect(screen.getByTestId("authenticated").textContent).toBe("true");
    expect(screen.getByTestId("user").textContent).toBe("Juan Pérez");
  });

  it("login exitoso guarda usuario y marca autenticado", async () => {
    const mockResponse = {
      success: true,
      nombre: "María García",
      email: "maria@test.com",
      rol: "USER",
      saldo: "1000000",
      numeroCuenta: "ACC-002",
    };

    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: async () => mockResponse,
    } as Response);

    let loginFn: (doc: string, pass: string) => Promise<boolean>;

    function LoginCapture() {
      const { login } = useAuth();
      loginFn = login;
      return <TestConsumer />;
    }

    render(
      <AuthProvider>
        <LoginCapture />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading").textContent).toBe("false");
    });

    let result: boolean;
    await act(async () => {
      result = await loginFn!("87654321", "pass123");
    });

    expect(result!).toBe(true);
    expect(screen.getByTestId("authenticated").textContent).toBe("true");
    expect(screen.getByTestId("user").textContent).toBe("María García");
    expect(localStorage.getItem("user_session")).not.toBeNull();
  });

  it("login fallido retorna false y no guarda sesión", async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ success: false, mensaje: "Contraseña incorrecta" }),
    } as Response);

    let loginFn: (doc: string, pass: string) => Promise<boolean>;

    function LoginCapture() {
      const { login } = useAuth();
      loginFn = login;
      return <TestConsumer />;
    }

    render(
      <AuthProvider>
        <LoginCapture />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading").textContent).toBe("false");
    });

    let result: boolean;
    await act(async () => {
      result = await loginFn!("12345678", "wrong");
    });

    expect(result!).toBe(false);
    expect(screen.getByTestId("authenticated").textContent).toBe("false");
    expect(localStorage.getItem("user_session")).toBeNull();
  });

  it("login con error de red retorna false", async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new Error("Network error"));

    let loginFn: (doc: string, pass: string) => Promise<boolean>;

    function LoginCapture() {
      const { login } = useAuth();
      loginFn = login;
      return <TestConsumer />;
    }

    render(
      <AuthProvider>
        <LoginCapture />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading").textContent).toBe("false");
    });

    let result: boolean;
    await act(async () => {
      result = await loginFn!("12345678", "pass");
    });

    expect(result!).toBe(false);
  });

  it("logout limpia usuario y localStorage", async () => {
    const savedUser = {
      id: 1,
      nombreCompleto: "Juan Pérez",
      username: "juan@test.com",
      numDocumento: "12345678",
      rol: "USER",
      saldo: 500000,
      numeroCuenta: "ACC-001",
    };
    localStorage.setItem("user_session", JSON.stringify(savedUser));

    let logoutFn: () => void;

    function LogoutCapture() {
      const { logout } = useAuth();
      logoutFn = logout;
      return <TestConsumer />;
    }

    render(
      <AuthProvider>
        <LogoutCapture />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("authenticated").textContent).toBe("true");
    });

    act(() => {
      logoutFn!();
    });

    expect(screen.getByTestId("authenticated").textContent).toBe("false");
    expect(screen.getByTestId("user").textContent).toBe("null");
    expect(localStorage.getItem("user_session")).toBeNull();
  });

  it("useAuth fuera de AuthProvider lanza error", () => {
    // Silenciar el error de consola esperado
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    function BareConsumer() {
      useAuth();
      return null;
    }

    expect(() => render(<BareConsumer />)).toThrow(
      "useAuth must be used within AuthProvider"
    );

    consoleSpy.mockRestore();
  });
});
