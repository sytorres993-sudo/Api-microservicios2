package com.example.tienda.pedidos;
import com.example.tienda.common.ApiException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
@Component
public class TiendaClient {
    private final RestClient productos;
    private final RestClient clientes;
    public TiendaClient(@Value("${services.productos.url}") String productosUrl, @Value("${services.clientes.url}") String clientesUrl) {
        var factory=new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        productos=RestClient.builder().requestFactory(factory).baseUrl(productosUrl).build();
        clientes=RestClient.builder().requestFactory(factory).baseUrl(clientesUrl).build();
    }
    public record Cliente(Long id,String nombre) {}
    public record ReservaItem(Long productoId,String nombre,int cantidad,BigDecimal precioUnitario) {}
    public record Reserva(String id,boolean cancelada,List<ReservaItem> items) {}
    public record Solicitud(List<PedidoRequest.Item> items) {}
    public Cliente cliente(Long id) {
        try {
            return clientes.get().uri("/api/clientes/{id}",id).retrieve().body(Cliente.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value()==404) throw new ApiException(HttpStatus.NOT_FOUND,"Cliente no encontrado");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"No fue posible consultar clientes");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Servicio de clientes no disponible");
        }
    }
    public Reserva reservar(Pedido pedido) {
        try {
            var items=pedido.items.stream().map(i -> new PedidoRequest.Item(i.productoId,i.cantidad)).toList();
            return productos.put().uri("/api/reservas/{id}",pedido.id).body(new Solicitud(items)).retrieve().body(Reserva.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value()==404) throw new ApiException(HttpStatus.NOT_FOUND,"Un producto no existe o no esta disponible",pedido.id);
            if (e.getStatusCode().value()==409) throw new ApiException(HttpStatus.CONFLICT,"Stock insuficiente o conflicto en la reserva",pedido.id);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"No fue posible reservar inventario; reintente confirmar el pedido",pedido.id);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Inventario no disponible; reintente confirmar el pedido",pedido.id);
        }
    }
    public void liberar(String id) {
        try {
            productos.delete().uri("/api/reservas/{id}",id).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"No fue posible devolver inventario; reintente cancelar el pedido",id);
        }
    }
}
