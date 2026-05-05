export const isAdminRole = (rol?: string | null): boolean => {
  if (!rol) return false;

  const normalized = rol.trim().toUpperCase();
  return normalized === "ADMIN" || normalized === "ADMINISTRADOR";
};

