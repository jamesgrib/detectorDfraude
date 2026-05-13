package com.fraude.usuario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraude.usuario.dto.LoginRequest;
import com.fraude.usuario.dto.LoginResponse;
import com.fraude.usuario.dto.RegisterRequest;
import com.fraude.usuario.model.Usuario;
import com.fraude.usuario.model.UsuarioId;
import com.fraude.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAll_retornaListaDeUsuarios() throws Exception {
        Usuario u = Usuario.builder().id(new UsuarioId("12345678")).nombre("Juan").build();
        when(usuarioService.getAllUsuarios()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Juan"));
    }

    @Test
    void login_exitoso_retorna200() throws Exception {
        LoginResponse resp = LoginResponse.builder()
                .success(true)
                .mensaje("Login exitoso")
                .nombre("Juan")
                .email("juan@test.com")
                .saldo(BigDecimal.valueOf(500_000))
                .numeroCuenta("ACC-001")
                .rol("USER")
                .build();
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("12345678", "pass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.nombre").value("Juan"))
                .andExpect(jsonPath("$.rol").value("USER"));
    }

    @Test
    void login_fallido_retorna200ConSuccessFalse() throws Exception {
        LoginResponse resp = LoginResponse.builder()
                .success(false)
                .mensaje("Contraseña incorrecta")
                .build();
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("12345678", "wrong"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_exitoso_retorna200() throws Exception {
        LoginResponse resp = LoginResponse.builder()
                .success(true)
                .mensaje("Cuenta creada exitosamente")
                .nombre("Nuevo")
                .email("nuevo@test.com")
                .numeroCuenta("ACC-123456")
                .saldo(BigDecimal.ZERO)
                .rol("USER")
                .build();
        when(usuarioService.register(any(RegisterRequest.class))).thenReturn(resp);

        RegisterRequest req = RegisterRequest.builder()
                .numDocumento("55555555")
                .nombre("Nuevo")
                .apellido("Usuario")
                .email("nuevo@test.com")
                .password("pass123")
                .build();

        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.numeroCuenta").value("ACC-123456"));
    }

    @Test
    void register_documentoDuplicado_retorna400() throws Exception {
        LoginResponse resp = LoginResponse.builder()
                .success(false)
                .mensaje("El número de documento ya está registrado")
                .build();
        when(usuarioService.register(any(RegisterRequest.class))).thenReturn(resp);

        RegisterRequest req = RegisterRequest.builder()
                .numDocumento("12345678")
                .nombre("Otro")
                .apellido("Usuario")
                .email("otro@test.com")
                .password("pass")
                .build();

        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
