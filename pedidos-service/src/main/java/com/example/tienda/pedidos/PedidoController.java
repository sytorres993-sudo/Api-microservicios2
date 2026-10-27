package com.example.tienda.pedidos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService service;
    private final PedidoRepository repository;
    public PedidoController(PedidoService service,PedidoRepository repository) { this.service=service; this.repository=repository; }
    @GetMapping public List<Pedido> listar(@RequestParam(required=false) Long clienteId) {
        return repository.findAllByOrderByFechaDesc().stream().filter(p -> clienteId==null || p.clienteId.equals(clienteId)).toList();
    }
    @GetMapping("/{id}") public Pedido obtener(@PathVariable UUID id) { return service.obtener(id.toString()); }
    @PostMapping public ResponseEntity<Pedido> crear(@Valid @RequestBody PedidoRequest request) {
        Pedido p=service.crear(request); return ResponseEntity.created(URI.create("/api/pedidos/"+p.id)).body(p);
    }
    @PostMapping("/{id}/confirmar") public Pedido confirmar(@PathVariable UUID id) { return service.confirmar(id.toString()); }
    @PostMapping("/{id}/cancelar") public Pedido cancelar(@PathVariable UUID id) { return service.cancelar(id.toString()); }
}
