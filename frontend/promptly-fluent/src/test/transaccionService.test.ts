import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  crearTransaccion,
  obtenerHistorial,
  obtenerTodasTransacciones,
  obtenerTransaccionesPendientes,
  actualizarEstadoTransaccion,
  obtenerTiposTransaccion,
  obtenerEstadosTransaccion,
  type Transaccion,
} from "../services/transaccionService";

const mockTransaccion = {
  id: 1,
  monto: 100_000,
  cuentaOrigenId: "ACC-001",
  cuentaDestinoId: "ACC-002",
  estadoId: 1,
  estadoNombre: "APROBADA",
  fechaCreacion: "2024-01-01T10:00:00",
};

describe("transaccionService", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  // ─── crearTransaccion ─────────────────────────────────────────────────────

  describe("crearTransaccion", () => {
    it("retorna la transacción creada en caso exitoso", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => mockTransaccion,
      } as Response);

      const body: Transaccion = {
        monto: 100_000,
        cuentaOrigenId: "ACC-001",
        cuentaDestinoId: "ACC-002",
      };

      const result = await crearTransaccion(body);

      expect(result.estadoNombre).toBe("APROBADA");
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/transacciones"),
        expect.objectContaining({ method: "POST" })
      );
    });

    it("lanza error con mensaje del servidor cuando falla", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: "Saldo insuficiente" }),
      } as Response);

      await expect(
        crearTransaccion({ monto: 0, cuentaOrigenId: "ACC-001", cuentaDestinoId: "ACC-002" })
      ).rejects.toThrow("Saldo insuficiente");
    });

    it("lanza error genérico cuando el servidor no devuelve mensaje", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => ({}),
      } as Response);

      await expect(
        crearTransaccion({ monto: 0, cuentaOrigenId: "ACC-001", cuentaDestinoId: "ACC-002" })
      ).rejects.toThrow("Error al crear transacción");
    });

    it("propaga error de red", async () => {
      vi.mocked(fetch).mockRejectedValueOnce(new Error("Network error"));

      await expect(
        crearTransaccion({ monto: 100, cuentaOrigenId: "ACC-001", cuentaDestinoId: "ACC-002" })
      ).rejects.toThrow("Network error");
    });
  });

  // ─── obtenerHistorial ─────────────────────────────────────────────────────

  describe("obtenerHistorial", () => {
    it("retorna lista de transacciones", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => [mockTransaccion],
      } as Response);

      const result = await obtenerHistorial("ACC-001");

      expect(result).toHaveLength(1);
      expect(result[0].estadoNombre).toBe("APROBADA");
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/cuenta/ACC-001"),
        expect.any(Object)
      );
    });

    it("lanza error cuando la respuesta no es ok", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: "Cuenta no encontrada" }),
      } as Response);

      await expect(obtenerHistorial("ACC-999")).rejects.toThrow("Cuenta no encontrada");
    });
  });

  // ─── obtenerTodasTransacciones ────────────────────────────────────────────

  describe("obtenerTodasTransacciones", () => {
    it("envía header X-Admin-Documento y retorna lista", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => [mockTransaccion],
      } as Response);

      const result = await obtenerTodasTransacciones("99999999");

      expect(result).toHaveLength(1);
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/api/transacciones"),
        expect.objectContaining({
          headers: expect.objectContaining({ "X-Admin-Documento": "99999999" }),
        })
      );
    });

    it("lanza error cuando no tiene permisos", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: "Solo un administrador puede ejecutar esta acción" }),
      } as Response);

      await expect(obtenerTodasTransacciones("12345678")).rejects.toThrow(
        "Solo un administrador puede ejecutar esta acción"
      );
    });
  });

  // ─── obtenerTransaccionesPendientes ───────────────────────────────────────

  describe("obtenerTransaccionesPendientes", () => {
    it("retorna transacciones pendientes", async () => {
      const pendiente = { ...mockTransaccion, estadoNombre: "PENDIENTE" };
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => [pendiente],
      } as Response);

      const result = await obtenerTransaccionesPendientes("99999999");

      expect(result[0].estadoNombre).toBe("PENDIENTE");
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/pendientes"),
        expect.any(Object)
      );
    });

    it("lanza error genérico cuando no hay mensaje del servidor", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => { throw new Error("invalid json"); },
      } as unknown as Response);

      await expect(obtenerTransaccionesPendientes("99999999")).rejects.toThrow(
        "Error al obtener transacciones pendientes"
      );
    });
  });

  // ─── actualizarEstadoTransaccion ──────────────────────────────────────────

  describe("actualizarEstadoTransaccion", () => {
    it("aprueba una transacción pendiente", async () => {
      const aprobada = { ...mockTransaccion, estadoNombre: "APROBADA" };
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => aprobada,
      } as Response);

      const result = await actualizarEstadoTransaccion(1, "APROBADA", "99999999");

      expect(result.estadoNombre).toBe("APROBADA");
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining("/1/estado"),
        expect.objectContaining({
          method: "PUT",
          headers: expect.objectContaining({ "X-Admin-Documento": "99999999" }),
          body: JSON.stringify({ estadoNombre: "APROBADA" }),
        })
      );
    });

    it("rechaza una transacción pendiente", async () => {
      const rechazada = { ...mockTransaccion, estadoNombre: "RECHAZADA" };
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => rechazada,
      } as Response);

      const result = await actualizarEstadoTransaccion(1, "RECHAZADA", "99999999");

      expect(result.estadoNombre).toBe("RECHAZADA");
    });

    it("lanza error cuando falla la actualización", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: false,
        json: async () => ({ message: "Transacción no encontrada" }),
      } as Response);

      await expect(
        actualizarEstadoTransaccion(999, "APROBADA", "99999999")
      ).rejects.toThrow("Transacción no encontrada");
    });
  });

  // ─── catálogos ────────────────────────────────────────────────────────────

  describe("obtenerTiposTransaccion", () => {
    it("retorna lista de tipos", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => [{ id: 1, nombre: "TRANSFERENCIA" }],
      } as Response);

      const result = await obtenerTiposTransaccion();

      expect(result[0].nombre).toBe("TRANSFERENCIA");
    });

    it("lanza error cuando falla", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({ ok: false } as Response);

      await expect(obtenerTiposTransaccion()).rejects.toThrow(
        "Error al obtener tipos de transacción"
      );
    });
  });

  describe("obtenerEstadosTransaccion", () => {
    it("retorna lista de estados", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        json: async () => [{ id: 1, nombre: "APROBADA" }],
      } as Response);

      const result = await obtenerEstadosTransaccion();

      expect(result[0].nombre).toBe("APROBADA");
    });

    it("lanza error cuando falla", async () => {
      vi.mocked(fetch).mockResolvedValueOnce({ ok: false } as Response);

      await expect(obtenerEstadosTransaccion()).rejects.toThrow(
        "Error al obtener estados de transacción"
      );
    });
  });
});
