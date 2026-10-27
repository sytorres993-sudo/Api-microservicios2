package com.example.tienda.productos;
import com.example.tienda.common.ApiException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class InventarioService {
    private final ProductoRepository productos;
    private final ReservaRepository reservas;
    public InventarioService(ProductoRepository productos, ReservaRepository reservas) {
        this.productos=productos; this.reservas=reservas;
    }
    public Producto buscar(Long id) {
        return productos.findById(id).filter(p -> p.activo).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Producto no encontrado"));
    }
    private Producto bloquear(Long id) {
        return productos.bloquear(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Producto no encontrado: " + id));
    }
    @Transactional
    public Producto guardar(Long id, ProductoRequest r) {
        Producto p = id == null ? new Producto() : bloquear(id);
        if (!p.activo) throw new ApiException(HttpStatus.NOT_FOUND,"Producto no encontrado");
        p.sku=r.sku().trim(); p.nombre=r.nombre().trim(); p.marca=r.marca().trim();
        p.categoria=r.categoria().trim(); p.descripcion=r.descripcion(); p.precio=r.precio(); p.stock=r.stock();
        return productos.saveAndFlush(p);
    }
    @Transactional
    public void eliminar(Long id) {
        Producto p=bloquear(id);
        p.activo=false;
    }
    private SortedMap<Long,Integer> cantidades(ReservaRequest r) {
        SortedMap<Long,Integer> result = new TreeMap<>();
        for (var i : r.items()) {
            int total=result.getOrDefault(i.productoId(),0) + i.cantidad();
            if (total>1000000) throw new ApiException(HttpStatus.BAD_REQUEST,"Cantidad maxima por producto: 1000000");
            result.put(i.productoId(), total);
        }
        return result;
    }
    private Reserva existente(Reserva r, Map<Long,Integer> cantidades) {
        if (r.cancelada) throw new ApiException(HttpStatus.CONFLICT,"La reserva ya fue cancelada");
        Map<Long,Integer> original=new TreeMap<>();
        r.items.forEach(i -> original.put(i.productoId,i.cantidad));
        if (!original.equals(cantidades)) throw new ApiException(HttpStatus.CONFLICT,"El identificador de reserva ya tiene otros productos");
        return r;
    }
    @Transactional
    public Reserva reservar(String id, ReservaRequest request) {
        var cantidades=cantidades(request);
        var anterior=reservas.bloquear(id);
        if (anterior.isPresent()) return existente(anterior.get(),cantidades);
        // Orden estable de bloqueos para evitar sobreventa y deadlocks entre productos.
        Map<Long,Producto> locked=new TreeMap<>();
        cantidades.keySet().forEach(pid -> locked.put(pid,bloquear(pid)));
        // Otra transaccion pudo haber creado la reserva mientras esperabamos el inventario.
        anterior=reservas.bloquear(id);
        if (anterior.isPresent()) return existente(anterior.get(),cantidades);
        Reserva reserva=new Reserva(); reserva.id=id;
        cantidades.forEach((pid,qty) -> {
            Producto producto=locked.get(pid);
            if (!producto.activo) throw new ApiException(HttpStatus.NOT_FOUND,"Producto no disponible: "+pid);
            if (producto.stock<qty) throw new ApiException(HttpStatus.CONFLICT,"Stock insuficiente para producto "+pid);
            producto.stock-=qty;
            reserva.items.add(new Reserva.Linea(producto,qty));
        });
        return reservas.saveAndFlush(reserva);
    }
    @Transactional
    public void liberar(String id) {
        var anterior=reservas.bloquear(id);
        if (anterior.isEmpty()) {
            // Impide que una peticion retrasada reserve despues de la cancelacion.
            Reserva tombstone=new Reserva(); tombstone.id=id; tombstone.cancelada=true;
            reservas.saveAndFlush(tombstone); return;
        }
        Reserva r=anterior.get();
        if (r.cancelada) return;
        r.items.stream().sorted(Comparator.comparing(i -> i.productoId)).forEach(i -> {
            Producto producto=bloquear(i.productoId);
            producto.stock=Math.addExact(producto.stock,i.cantidad);
        });
        r.cancelada=true;
    }
}
