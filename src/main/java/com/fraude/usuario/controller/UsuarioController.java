package com.fraude.usuario.controller;

import com.fraude.usuario.dto.LoginRequest;
import com.fraude.usuario.dto.LoginResponse;
import com.fraude.usuario.dto.RegisterRequest;
import com.fraude.usuario.model.Usuario;
import com.fraude.usuario.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = { "http://localhost:8081", "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:5174", "http://127.0.0.1:5174" })
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Usuario> getAll() {
        return service.getAllUsuarios();
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return service.login(loginRequest);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request) {
        LoginResponse response = service.register(request);
        if (!response.isSuccess()) {
            // CP-001-02: documento duplicado → 409 Conflict
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(response);
        }
        // CP-001-01: registro exitoso → 201 Created
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    /**
     * CP-002: Asignar rol a un usuario.
     * Requiere header X-Admin-Documento con el documento del administrador.
     */
    @PutMapping("/{numDocumento}/rol")
    public ResponseEntity<LoginResponse> asignarRol(
            @PathVariable String numDocumento,
            @RequestBody java.util.Map<String, String> body,
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento) {
        String nuevoRol = body.get("rol");
        LoginResponse response = service.asignarRol(numDocumento, nuevoRol, adminDocumento);
        return ResponseEntity.ok(response);
    }
}