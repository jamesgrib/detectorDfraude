import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const mapEstadoToStatus = (
  estadoNombre?: string,
): "approved" | "rejected" | "pending" => {
  const map: Record<string, "approved" | "rejected"> = {
    APROBADA: "approved",
    RECHAZADA: "rejected",
  };
  return map[estadoNombre ?? ""] ?? "pending";
};
