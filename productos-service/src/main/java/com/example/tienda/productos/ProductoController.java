package com.example.tienda.productos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/productos")
public class ProductoController {
    private final InventarioService service;
    private final ProductoRepository repository;
    public ProductoController(InventarioService service, ProductoRepository repository) { this.service=service; this.repository=repository; }
    @GetMapping
    public List<Producto> listar(@RequestParam(required=false) String categoria, @RequestParam(required=false) String buscar) {
        return repository.findByActivoTrueOrderByIdAsc().stream()
            .filter(p -> categoria==null || p.categoria.equalsIgnoreCase(categoria))
            .filter(p -> buscar==null || (p.nombre+" "+p.marca+" "+p.sku).toLowerCase(Locale.ROOT).contains(buscar.toLowerCase(Locale.ROOT))).toList();
    }
    @GetMapping("/{id}") public Producto obtener(@PathVariable Long id) { return service.buscar(id); }
    @PostMapping public ResponseEntity<Producto> crear(@Valid @RequestBody ProductoRequest request) {
        Producto p=service.guardar(null,request);
        return ResponseEntity.created(URI.create("/api/productos/"+p.id)).body(p);
    }
    @PutMapping("/{id}") public Producto editar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) { return service.guardar(id,request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { service.eliminar(id); return ResponseEntity.noContent().build(); }
}
