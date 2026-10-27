package com.example.tienda.clientes;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ClienteRepository extends JpaRepository<Cliente,Long> {
    List<Cliente> findByActivoTrueOrderByIdAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> bloquear(@Param("id") Long id);
}
