package com.example.tienda.productos;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {
    private final InventarioService service;
    public ReservaController(InventarioService service) { this.service=service; }
    @PutMapping("/{id}") public Reserva reservar(@PathVariable UUID id, @Valid @RequestBody ReservaRequest request) { return service.reservar(id.toString(),request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> liberar(@PathVariable UUID id) { service.liberar(id.toString()); return ResponseEntity.noContent().build(); }
}
