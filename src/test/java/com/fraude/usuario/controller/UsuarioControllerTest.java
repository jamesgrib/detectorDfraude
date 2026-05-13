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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

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
                .success(true)
                .mensaje("Login exitoso")
                .nombre("Juan")
                .email("juan@test.com")
                .saldo(BigDecimal.valueOf(500_000))
                .numeroCuenta("ACC-001")
                .rol("USER")
                .build();
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(resp);

        LoginResponse result = controller.login(new LoginRequest("12345678", "pass123"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getNombre()).isEqualTo("Juan");
        assertThat(result.getRol()).isEqualTo("USER");
    }

    @Test
    void login_fallido_retornaSuccessFalse() {
        LoginResponse resp = LoginResponse.builder()
                .success(false)
                .mensaje("Contraseña incorrecta")
                .build();
        when(usuarioService.login(any(LoginRequest.class))).thenReturn(resp);

        LoginResponse result = controller.login(new LoginRequest("12345678", "wrong"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMensaje()).contains("Contraseña");
    }

    @Test
    void register_exitoso_retorna200() {
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

        ResponseEntity<LoginResponse> response = controller.register(req);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getNumeroCuenta()).isEqualTo("ACC-123456");
    }

    @Test
    void register_documentoDuplicado_retorna400() {
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

        ResponseEntity<LoginResponse> response = controller.register(req);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().isSuccess()).isFalse();
    }
}
