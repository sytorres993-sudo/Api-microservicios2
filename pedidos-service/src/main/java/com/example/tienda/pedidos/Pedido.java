package com.example.tienda.pedidos;
import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;
@Entity
public class Pedido {
    public enum Estado { PENDIENTE, CONFIRMADO, CANCELADO }
    @Id public String id;
    @Column(nullable=false) public Long clienteId;
    @Column(nullable=false) public String clienteNombre;
    @Column(nullable=false) public Instant fecha=Instant.now();
    @Enumerated(EnumType.STRING) @Column(nullable=false) public Estado estado=Estado.PENDIENTE;
    @Column(precision=22,scale=2,nullable=false) public BigDecimal total=BigDecimal.ZERO;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="pedido_items",joinColumns=@JoinColumn(name="pedido_id"))
    @OrderColumn(name="posicion")
    public List<Item> items=new ArrayList<>();
    @Embeddable
    public static class Item {
        public Long productoId;
        public String nombre;
        public int cantidad;
        @Column(precision=14,scale=2) public BigDecimal precioUnitario;
        @Column(precision=22,scale=2) public BigDecimal subtotal;
        public Item() {}
        public Item(Long productoId,int cantidad) { this.productoId=productoId; this.cantidad=cantidad; }
    }
}
