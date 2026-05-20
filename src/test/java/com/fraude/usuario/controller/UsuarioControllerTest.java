package com.fraude.usuario.controller;

import com.fraude.usuario.dto.LoginRequest;
import com.fraude.usuario.dto.LoginResponse;
import com.fraude.usuario.dto.RegisterRequest;
import com.fraude.usuario.model.Usuario;
import com.fraude.usuario.model.UsuarioId;
import com.fraude.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController controller;

    @Test
    void getAll_retornaListaDeUsuarios() {
        Usuario u = Usuario.builder().id(new UsuarioId("12345678")).nombre("Juan").build();
        when(usuarioService.getAllUsuarios()).thenReturn(List.of(u));

        List<Usuario> result = controller.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Juan");
    }

    @Test
    void login_exitoso_retornaResponse() {
        LoginResponse resp = LoginResponse.builder()
                .success(true).mensaje("Login exitoso").nombre("Juan")
                .email("juan@test.com").saldo(BigDecimal.valueOf(500_000))
                .numeroCuenta("ACC-001").rol("USER").build();
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(resp);

        LoginResponse result = controller.login(new LoginRequest("12345678", "pass123"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getNombre()).isEqualTo("Juan");
    }

    @Test
    void login_fallido_retornaSuccessFalse() {
        LoginResponse resp = LoginResponse.builder()
                .success(false).mensaje("Contraseña incorrecta").build();
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(resp);

        LoginResponse result = controller.login(new LoginRequest("12345678", "wrong"));

        assertThat(result.isSuccess()).isFalse();
    }

    // CP-001-01: Registro exitoso → HTTP 201 Created
    @Test
    void register_exitoso_retorna201() {
        LoginResponse resp = LoginResponse.builder()
                .success(true).mensaje("Cuenta creada exitosamente")
                .nombre("Nuevo").email("nuevo@test.com")
                .numeroCuenta("ACC-123456").saldo(BigDecimal.ZERO).rol("USER").build();
        when(usuarioService.register(any(RegisterRequest.class))).thenReturn(resp);

        ResponseEntity<LoginResponse> response = controller.register(RegisterRequest.builder()
                .numDocumento("55555555").nombre("Nuevo").apellido("Usuario")
                .email("nuevo@test.com").password("pass123").build());

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getNumeroCuenta()).isEqualTo("ACC-123456");
    }

    // CP-001-02: Documento duplicado → HTTP 409 Conflict
    @Test
    void register_documentoDuplicado_retorna409() {
        LoginResponse resp = LoginResponse.builder()
                .success(false).mensaje("El número de documento ya está registrado").build();
        when(usuarioService.register(any(RegisterRequest.class))).thenReturn(resp);

        ResponseEntity<LoginResponse> response = controller.register(RegisterRequest.builder()
                .numDocumento("12345678").nombre("Otro").apellido("Usuario")
                .email("otro@test.com").password("pass").build());

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMensaje()).contains("ya está registrado");
    }

    // CP-002-01: Asignación exitosa de rol → HTTP 200 OK
    @Test
    void asignarRol_adminValido_retorna200() {
        LoginResponse resp = LoginResponse.builder()
                .success(true).mensaje("Rol asignado exitosamente")
                .nombre("Juan").rol("ADMIN").build();
        when(usuarioService.asignarRol(anyString(), anyString(), anyString())).thenReturn(resp);

        ResponseEntity<LoginResponse> response = controller.asignarRol(
                "12345678", Map.of("rol", "ADMIN"), "99999999");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getRol()).isEqualTo("ADMIN");
    }
}
