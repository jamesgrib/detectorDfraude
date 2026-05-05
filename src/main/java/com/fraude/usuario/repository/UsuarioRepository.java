package com.fraude.usuario.repository;

import com.fraude.usuario.model.Usuario;
import com.fraude.usuario.model.UsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, UsuarioId> {
    @Query("SELECT u FROM Usuario u WHERE u.id.numDocumento = :numDocumento")
    Optional<Usuario> findByNumDocumento(@Param("numDocumento") String numDocumento);
}