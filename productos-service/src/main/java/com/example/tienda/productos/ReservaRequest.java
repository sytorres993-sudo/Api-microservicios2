package com.example.tienda.productos;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record ReservaRequest(@NotEmpty @Size(max=100) List<@NotNull @Valid Item> items) {
    public record Item(@NotNull @Positive Long productoId, @NotNull @Min(1) @Max(1000000) Integer cantidad) {}
}
