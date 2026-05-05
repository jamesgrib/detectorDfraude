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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final CuentaRepository cuentaRepository;
    private final RolRepository rolRepository;

    public UsuarioService(UsuarioRepository repository, CuentaRepository cuentaRepository,
            RolRepository rolRepository) {
        this.repository = repository;
        this.cuentaRepository = cuentaRepository;
        this.rolRepository = rolRepository;
    }

    public List<Usuario> getAllUsuarios() {
        return repository.findAll();
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Usuario usuario = repository.findByNumDocumento(loginRequest.getNumDocumento())
                .orElse(null);

        if (usuario == null) {
            return LoginResponse.builder()
                    .success(false)
                    .mensaje("Usuario no encontrado")
                    .build();
        }

        // Comparar password (por ahora, comparación directa)
        if (!usuario.getPasswordHash().equals(loginRequest.getPassword())) {
            return LoginResponse.builder()
                    .success(false)
                    .mensaje("Contraseña incorrecta")
                    .build();
        }

        // Obtener información de la cuenta
        Cuenta cuenta = cuentaRepository.findByNumDocumento(loginRequest.getNumDocumento())
                .orElse(null);

        // Login exitoso
        return LoginResponse.builder()
                .success(true)
                .mensaje("Login exitoso")
                .email(usuario.getEmail())
                .nombre(usuario.getNombre())
                .saldo(cuenta != null ? cuenta.getSaldo() : null)
                .numeroCuenta(cuenta != null ? cuenta.getNumeroCuenta() : loginRequest.getNumDocumento())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }

    public boolean esAdministrador(String numDocumento) {
        if (numDocumento == null || numDocumento.isBlank()) {
            return false;
        }

        return repository.findByNumDocumento(numDocumento.trim())
                .map(Usuario::getRol)
                .map(rol -> rol != null ? rol.getNombre() : null)
                .map(this::esRolAdmin)
                .orElse(false);
    }

    private boolean esRolAdmin(String rolNombre) {
        if (rolNombre == null || rolNombre.isBlank()) {
            return false;
        }

        String normalizado = rolNombre.trim().toUpperCase();
        return normalizado.equals("ADMIN") || normalizado.equals("ADMINISTRADOR");
    }

    public LoginResponse register(RegisterRequest request) {
        // Verificar que el número de documento no exista
        if (repository.findByNumDocumento(request.getNumDocumento()).isPresent()) {
            return LoginResponse.builder()
                    .success(false)
                    .mensaje("El número de documento ya está registrado")
                    .build();
        }

        // Obtener el rol USER
        Rol rolUser = rolRepository.findByNombre("USER")
                .orElseThrow(() -> new RuntimeException("Rol USER no encontrado en la base de datos"));

        // Crear usuario
        UsuarioId id = new UsuarioId(request.getNumDocumento());
        Usuario usuario = Usuario.builder()
                .id(id)
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .rolId(rolUser.getId())
                .estadoId(1)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        repository.save(usuario);

        // Generar número de cuenta único
        String numeroCuenta;
        do {
            numeroCuenta = "ACC-" + String.format("%06d", (int) (Math.random() * 1_000_000));
        } while (cuentaRepository.findById(numeroCuenta).isPresent());

        // Crear cuenta con saldo inicial 0
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
}