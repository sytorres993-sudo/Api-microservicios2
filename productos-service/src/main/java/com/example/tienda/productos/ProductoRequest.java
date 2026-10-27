package com.example.tienda.productos;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ProductoRequest(
    @NotBlank @Size(max=60) String sku,
    @NotBlank @Size(max=120) String nombre,
    @NotBlank @Size(max=80) String marca,
    @NotBlank @Size(max=80) String categoria,
    @Size(max=1000) String descripcion,
    @NotNull @DecimalMin("0.01") @Digits(integer=12, fraction=2) BigDecimal precio,
    @NotNull @Min(0) @Max(1000000) Integer stock) {}
