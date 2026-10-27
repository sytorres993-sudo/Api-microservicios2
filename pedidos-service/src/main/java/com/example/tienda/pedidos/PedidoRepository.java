package com.example.tienda.pedidos;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface PedidoRepository extends JpaRepository<Pedido,String> {
    List<Pedido> findAllByOrderByFechaDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> bloquear(@Param("id") String id);
}
