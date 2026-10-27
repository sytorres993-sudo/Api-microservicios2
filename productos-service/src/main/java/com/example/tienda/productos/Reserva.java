package com.example.tienda.productos;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;
@Entity
public class Reserva {
    @Id public String id;
    @Version private Long version;
    @Column(nullable=false) public boolean cancelada;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="reserva_lineas", joinColumns=@JoinColumn(name="reserva_id"))
    @OrderColumn(name="posicion")
    public List<Linea> items = new ArrayList<>();
    @Embeddable
    public static class Linea {
        public Long productoId;
        public String nombre;
        public int cantidad;
        @Column(precision=14,scale=2) public BigDecimal precioUnitario;
        public Linea() {}
        public Linea(Producto p, int cantidad) {
            this.productoId=p.id; this.nombre=p.nombre; this.cantidad=cantidad; this.precioUnitario=p.precio;
        }
    }
}
