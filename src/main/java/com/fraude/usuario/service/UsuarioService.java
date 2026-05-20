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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final CuentaRepository cuentaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public UsuarioService(UsuarioRepository repository, CuentaRepository cuentaRepository,
            RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.cuentaRepository = cuentaRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> getAllUsuarios() {
        return repository.findAll();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Usuario usuario = repository.findByNumDocumento(loginRequest.getNumDocumento())
                .orElse(null);

        if (usuario == null) {
            return LoginResponse.builder().success(false).mensaje("Usuario no encontrado").build();
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPasswordHash())) {
            return LoginResponse.builder().success(false).mensaje("Contraseña incorrecta").build();
        }

        Cuenta cuenta = cuentaRepository.findByNumDocumento(loginRequest.getNumDocumento()).orElse(null);
        return construirRespuestaExitosa(usuario, cuenta, loginRequest.getNumDocumento());
    }

    private LoginResponse construirRespuestaExitosa(Usuario usuario, Cuenta cuenta, String numDocumento) {
        return LoginResponse.builder()
                .success(true)
                .mensaje("Login exitoso")
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .saldo(cuenta != null ? cuenta.getSaldo() : null)
                .numeroCuenta(cuenta != null ? cuenta.getNumeroCuenta() : numDocumento)
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }

    public boolean esAdministrador(String numDocumento) {
        if (numDocumento == null || numDocumento.isBlank()) return false;
        return repository.findByNumDocumento(numDocumento.trim())
                .map(u -> u.getRol() != null ? u.getRol().getNombre() : null)
                .map(this::esRolAdmin)
                .orElse(false);
    }

    private boolean esRolAdmin(String rolNombre) {
        if (rolNombre == null || rolNombre.isBlank()) return false;
        return java.util.Set.of("ADMIN", "ADMINISTRADOR").contains(rolNombre.trim().toUpperCase());
    }

    public LoginResponse register(RegisterRequest request) {        if (repository.findByNumDocumento(request.getNumDocumento()).isPresent()) {
            return LoginResponse.builder()
                    .success(false)
                    .mensaje("El número de documento ya está registrado")
                    .build();
        }

        Rol rolUser = rolRepository.findByNombre("USER")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol USER no encontrado en la base de datos"));

        UsuarioId id = new UsuarioId(request.getNumDocumento());
        Usuario usuario = Usuario.builder()
                .id(id)
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rolId(rolUser.getId())
                .estadoId(1)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        repository.save(usuario);

        // Usar nextInt() en lugar de Math.random() para generar número de cuenta
        String numeroCuenta;
        do {
            numeroCuenta = "ACC-" + String.format("%06d", secureRandom.nextInt(1_000_000));
        } while (cuentaRepository.findById(numeroCuenta).isPresent());

        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(numeroCuenta)
                .saldo(BigDecimal.ZERO)
                .numDocumento(request.getNumDocumento())
                .build();
        cuentaRepository.save(cuenta);

        return LoginResponse.builder()
                .success(true)
                .mensaje("Cuenta creada exitosamente")
                .nombre(request.getNombre())
                .email(request.getEmail())
                .numeroCuenta(numeroCuenta)
                .saldo(BigDecimal.ZERO)
                .rol("USER")
                .build();
    }

    /**
     * CP-002: Asigna un rol a un usuario existente.
     * Solo puede ser ejecutado por un administrador.
     */
    public LoginResponse asignarRol(String numDocumento, String nuevoRol, String adminDocumento) {
        if (!esAdministrador(adminDocumento)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Solo un administrador puede asignar roles");
        }

        Usuario usuario = repository.findByNumDocumento(numDocumento)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado: " + numDocumento));

        Rol rol = rolRepository.findByNombre(nuevoRol.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rol no encontrado: " + nuevoRol));

        usuario.setRolId(rol.getId());
        repository.save(usuario);

        return LoginResponse.builder()
                .success(true)
                .mensaje("Rol asignado exitosamente")
                .nombre(usuario.getNombre())
                .rol(rol.getNombre())
                .build();
    }
}
