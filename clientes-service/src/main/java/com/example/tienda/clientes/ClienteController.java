package com.example.tienda.clientes;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    private final ClienteService service;
    private final ClienteRepository repository;
    public ClienteController(ClienteService service, ClienteRepository repository) { this.service=service; this.repository=repository; }
    @GetMapping public List<Cliente> listar() { return repository.findByActivoTrueOrderByIdAsc(); }
    @GetMapping("/{id}") public Cliente obtener(@PathVariable Long id) { return service.obtener(id); }
    @PostMapping public ResponseEntity<Cliente> crear(@Valid @RequestBody ClienteRequest request) {
        Cliente c=service.guardar(null,request);
        return ResponseEntity.created(URI.create("/api/clientes/"+c.id)).body(c);
    }
    @PutMapping("/{id}") public Cliente editar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) { return service.guardar(id,request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> eliminar(@PathVariable Long id) { service.eliminar(id); return ResponseEntity.noContent().build(); }
}
