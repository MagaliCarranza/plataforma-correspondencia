package com.magali.correspondencia.repository;

import com.magali.correspondencia.model.Rol;
import com.magali.correspondencia.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByRol(Rol rol);
    boolean existsByRolAndEsDemoFalse(Rol rol);
    boolean existsByAreaId(Long areaId);
    List<Usuario> findByRolAndBloqueadaFalseOrderByNombreCompletoAsc(Rol rol);

    /**
     * Borra todas las cuentas excepto los ADMIN con es_demo=false. Usado por el reset del modo demo.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Usuario u where u.esDemo = true or u.rol <> :rolProtegido")
    int borrarExceptoAdminReal(@Param("rolProtegido") Rol rolProtegido);
}
