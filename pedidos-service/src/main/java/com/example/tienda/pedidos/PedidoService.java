package com.example.tienda.pedidos;
import com.example.tienda.common.ApiException;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
@Service
public class PedidoService {
    private final PedidoRepository repository;
    private final TiendaClient client;
    private final TransactionTemplate tx;
    public PedidoService(PedidoRepository repository,TiendaClient client,org.springframework.transaction.PlatformTransactionManager manager) {
        this.repository=repository; this.client=client; this.tx=new TransactionTemplate(manager);
    }
    public Pedido obtener(String id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Pedido no encontrado"));
    }
    private Pedido bloquear(String id) {
        return repository.bloquear(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Pedido no encontrado"));
    }
    public Pedido crear(PedidoRequest request) {
        var cliente=client.cliente(request.clienteId());
        SortedMap<Long,Integer> cantidades=new TreeMap<>();
        request.items().forEach(i -> {
            int qty=cantidades.getOrDefault(i.productoId(),0)+i.cantidad();
            if (qty>1000000) throw new ApiException(HttpStatus.BAD_REQUEST,"Cantidad maxima por producto: 1000000");
            cantidades.put(i.productoId(),qty);
        });
        Pedido pedido=new Pedido(); pedido.id=UUID.randomUUID().toString();
        pedido.clienteId=cliente.id(); pedido.clienteNombre=cliente.nombre();
        cantidades.forEach((id,qty) -> pedido.items.add(new Pedido.Item(id,qty)));
        // Guardar primero permite recuperar fallos o timeouts con el mismo ID de reserva.
        tx.executeWithoutResult(status -> repository.saveAndFlush(pedido));
        return confirmar(pedido.id);
    }
    public Pedido confirmar(String id) {
        return tx.execute(status -> {
            Pedido pedido=bloquear(id);
            if (pedido.estado==Pedido.Estado.CANCELADO) throw new ApiException(HttpStatus.CONFLICT,"El pedido esta cancelado",id);
            if (pedido.estado==Pedido.Estado.CONFIRMADO) return pedido;
            TiendaClient.Reserva reserva=client.reservar(pedido);
            pedido.items.clear(); pedido.total=BigDecimal.ZERO;
            for (var linea : reserva.items()) {
                Pedido.Item item=new Pedido.Item(linea.productoId(),linea.cantidad());
                item.nombre=linea.nombre(); item.precioUnitario=linea.precioUnitario();
                item.subtotal=item.precioUnitario.multiply(BigDecimal.valueOf(item.cantidad));
                pedido.total=pedido.total.add(item.subtotal); pedido.items.add(item);
            }
            pedido.estado=Pedido.Estado.CONFIRMADO;
            return repository.saveAndFlush(pedido);
        });
    }
    public Pedido cancelar(String id) {
        return tx.execute(status -> {
            Pedido pedido=bloquear(id);
            if (pedido.estado==Pedido.Estado.CANCELADO) return pedido;
            client.liberar(id);
            pedido.estado=Pedido.Estado.CANCELADO;
            return repository.saveAndFlush(pedido);
        });
    }
}
