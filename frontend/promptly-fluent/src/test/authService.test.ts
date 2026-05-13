import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { login } from "../services/authService";

describe("authService", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("login exitoso retorna datos del usuario", async () => {
    const mockData = {
      success: true,
      nombre: "Juan Pérez",
      email: "juan@test.com",
      rol: "USER",
      saldo: "500000",
      numeroCuenta: "ACC-001234",
    };

    vi.mocked(fetch).mockResolvedValueOnce({
      ok: true,
      json: async () => mockData,
    } as Response);

    const result = await login("12345678", "pass123");

    expect(result).toEqual(mockData);
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/usuarios/login"),
      expect.objectContaining({
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ numDocumento: "12345678", password: "pass123" }),
      })
    );
  });

  it("login con credenciales incorrectas lanza error", async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      json: async () => ({ message: "Contraseña incorrecta" }),
    } as Response);

    await expect(login("12345678", "wrong")).rejects.toThrow("Contraseña incorrecta");
  });

  it("login con error de red lanza error", async () => {
    vi.mocked(fetch).mockRejectedValueOnce(new Error("Network error"));

    await expect(login("12345678", "pass123")).rejects.toThrow("Network error");
  });

  it("login con respuesta sin mensaje usa mensaje genérico", async () => {
    vi.mocked(fetch).mockResolvedValueOnce({
      ok: false,
      json: async () => ({}),
    } as Response);

    await expect(login("12345678", "pass123")).rejects.toThrow(
      "Error al iniciar sesión"
    );
  });
});
