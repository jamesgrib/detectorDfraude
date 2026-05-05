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
      // Si el backend lanza un error (401, 404, 500), lo capturamos
      const errorData = await response.json();
      throw new Error(errorData.message || "Error al iniciar sesión");
    }

    return await response.json(); // Retorna el objeto Usuario (sin password)
  } catch (error) {
    throw error;
  }
};
