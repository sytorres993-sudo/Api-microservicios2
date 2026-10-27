package com.example.tienda.productos;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
public class Producto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true, length = 60) public String sku;
    @Column(nullable = false, length = 120) public String nombre;
    @Column(nullable = false, length = 80) public String marca;
    @Column(nullable = false, length = 80) public String categoria;
    @Column(length = 1000) public String descripcion;
    @Column(nullable = false, precision = 14, scale = 2) public BigDecimal precio;
    @Column(nullable = false) public int stock;
    public boolean activo = true;
}
