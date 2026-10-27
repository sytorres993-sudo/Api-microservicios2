package com.example.tienda.clientes;
import jakarta.persistence.*;
@Entity
public class Cliente {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,length=120) public String nombre;
    @Column(nullable=false,unique=true,length=160) public String email;
    @Column(nullable=false,length=30) public String telefono;
    @Column(nullable=false,length=250) public String direccion;
    public boolean activo=true;
}
