package com.fraude.usuario.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.rol.model.Rol;
import com.fraude.rol.repository.RolRepository;
import com.fraude.usuario.dto.LoginRequest;
import com.fraude.usuario.dto.LoginResponse;
import com.fraude.usuario.dto.RegisterRequest;
import com.fraude.usuario.model.Usuario;
import com.fraude.usuario.model.UsuarioId;
import com.fraude.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioCliente;
    private Usuario usuarioAdmin;
    private Cuenta cuenta;
    private Rol rolUser;
    private Rol rolAdmin;

    @BeforeEach
    void setUp() {
        rolUser = Rol.builder().id(1).nombre("USER").build();
        rolAdmin = Rol.builder().id(2).nombre("ADMIN").build();

        usuarioCliente = Usuario.builder()
                .id(new UsuarioId("12345678"))
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@test.com")
                .passwordHash("pass123")
                .rol(rolUser)
                .rolId(1)
                .estadoId(1)
                .build();

        usuarioAdmin = Usuario.builder()
                .id(new UsuarioId("99999999"))
                .nombre("Admin")
                .apellido("Sistema")
                .email("admin@test.com")
                .passwordHash("admin123")
                .rol(rolAdmin)
                .rolId(2)
                .estadoId(1)
                .build();

        cuenta = Cuenta.builder()
                .numeroCuenta("ACC-001234")
                .saldo(BigDecimal.valueOf(500_000))
                .numDocumento("12345678")
                .build();
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void login_usuarioNoExiste_retornaFallido() {
        when(usuarioRepository.findByNumDocumento("00000000")).thenReturn(Optional.empty());

        LoginResponse resp = usuarioService.login(new LoginRequest("00000000", "pass"));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMensaje()).contains("no encontrado");
    }

    @Test
    void login_passwordIncorrecta_retornaFallido() {
        when(usuarioRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(usuarioCliente));

        LoginResponse resp = usuarioService.login(new LoginRequest("12345678", "wrongpass"));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMensaje()).contains("Contraseña");
    }

    @Test
    void login_credencialesCorrectas_retornaExitoso() {
        when(usuarioRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(usuarioCliente));
        when(cuentaRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(cuenta));

        LoginResponse resp = usuarioService.login(new LoginRequest("12345678", "pass123"));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getNombre()).isEqualTo("Juan");
        assertThat(resp.getEmail()).isEqualTo("juan@test.com");
        assertThat(resp.getNumeroCuenta()).isEqualTo("ACC-001234");
        assertThat(resp.getSaldo()).isEqualByComparingTo(BigDecimal.valueOf(500_000));
    }

    @Test
    void login_credencialesCorrectasSinCuenta_retornaExitosoConDocumentoComoCuenta() {
        when(usuarioRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(usuarioCliente));
        when(cuentaRepository.findByNumDocumento("12345678")).thenReturn(Optional.empty());

        LoginResponse resp = usuarioService.login(new LoginRequest("12345678", "pass123"));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getNumeroCuenta()).isEqualTo("12345678");
        assertThat(resp.getSaldo()).isNull();
    }

    // ─── esAdministrador ─────────────────────────────────────────────────────

    @Test
    void esAdministrador_documentoNulo_retornaFalse() {
        assertThat(usuarioService.esAdministrador(null)).isFalse();
    }

    @Test
    void esAdministrador_documentoVacio_retornaFalse() {
        assertThat(usuarioService.esAdministrador("   ")).isFalse();
    }

    @Test
    void esAdministrador_usuarioNoExiste_retornaFalse() {
        when(usuarioRepository.findByNumDocumento("00000000")).thenReturn(Optional.empty());

        assertThat(usuarioService.esAdministrador("00000000")).isFalse();
    }

    @Test
    void esAdministrador_usuarioConRolUser_retornaFalse() {
        when(usuarioRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(usuarioCliente));

        assertThat(usuarioService.esAdministrador("12345678")).isFalse();
    }

    @Test
    void esAdministrador_usuarioConRolAdmin_retornaTrue() {
        when(usuarioRepository.findByNumDocumento("99999999")).thenReturn(Optional.of(usuarioAdmin));

        assertThat(usuarioService.esAdministrador("99999999")).isTrue();
    }

    @Test
    void esAdministrador_usuarioConRolAdministrador_retornaTrue() {
        Rol rolAdministrador = Rol.builder().id(3).nombre("ADMINISTRADOR").build();
        Usuario u = Usuario.builder()
                .id(new UsuarioId("88888888"))
                .nombre("Super")
                .passwordHash("x")
                .rol(rolAdministrador)
                .build();
        when(usuarioRepository.findByNumDocumento("88888888")).thenReturn(Optional.of(u));

        assertThat(usuarioService.esAdministrador("88888888")).isTrue();
    }

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_documentoYaExiste_retornaFallido() {
        when(usuarioRepository.findByNumDocumento("12345678")).thenReturn(Optional.of(usuarioCliente));

        RegisterRequest req = RegisterRequest.builder()
                .numDocumento("12345678")
                .nombre("Otro")
                .apellido("Usuario")
                .email("otro@test.com")
                .password("pass")
                .build();

        LoginResponse resp = usuarioService.register(req);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMensaje()).contains("ya está registrado");
    }

    @Test
    void register_rolUserNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findByNumDocumento("55555555")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre("USER")).thenReturn(Optional.empty());

        RegisterRequest req = RegisterRequest.builder()
                .numDocumento("55555555")
                .nombre("Nuevo")
                .apellido("Usuario")
                .email("nuevo@test.com")
                .password("pass")
                .build();

        assertThatThrownBy(() -> usuarioService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rol USER no encontrado");
    }

    @Test
    void register_datosValidos_creaUsuarioYCuenta() {
        when(usuarioRepository.findByNumDocumento("55555555")).thenReturn(Optional.empty());
        when(rolRepository.findByNombre("USER")).thenReturn(Optional.of(rolUser));
        when(cuentaRepository.findById(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterRequest req = RegisterRequest.builder()
                .numDocumento("55555555")
                .nombre("Nuevo")
                .apellido("Usuario")
                .email("nuevo@test.com")
                .password("pass123")
                .build();

        LoginResponse resp = usuarioService.register(req);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getNombre()).isEqualTo("Nuevo");
        assertThat(resp.getEmail()).isEqualTo("nuevo@test.com");
        assertThat(resp.getRol()).isEqualTo("USER");
        assertThat(resp.getSaldo()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getNumeroCuenta()).startsWith("ACC-");

        verify(usuarioRepository).save(any(Usuario.class));
        verify(cuentaRepository).save(any(Cuenta.class));
    }

    // ─── getAllUsuarios ───────────────────────────────────────────────────────

    @Test
    void getAllUsuarios_retornaLista() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioCliente, usuarioAdmin));

        List<Usuario> result = usuarioService.getAllUsuarios();

        assertThat(result).hasSize(2);
    }
}
