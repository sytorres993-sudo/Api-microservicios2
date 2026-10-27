package com.example.tienda.clientes;
import jakarta.validation.constraints.*;
public record ClienteRequest(
    @NotBlank @Size(max=120) String nombre,
    @NotBlank @Email @Size(max=160) String email,
    @NotBlank @Pattern(regexp="[+0-9 ()-]{7,30}", message="Telefono invalido (7 a 30 caracteres)") String telefono,
    @NotBlank @Size(max=250) String direccion) {}
