// src/services/authService.js
const API_URL = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/api/usuarios`;

export const login = async (numDocumento, password) => {
  try {
    const response = await fetch(`${API_URL}/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ numDocumento, password }),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || "Error al iniciar sesión");
    }

    return await response.json();
  } catch (error) {
    throw error;
  }
};

export const register = async (data) => {
  const response = await fetch(`${API_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  const body = await response.json();
  if (!response.ok) {
    throw new Error(body.mensaje || "Error al registrar usuario");
  }
  return body;
};

export const logout = () => {
  localStorage.removeItem("user");
};
