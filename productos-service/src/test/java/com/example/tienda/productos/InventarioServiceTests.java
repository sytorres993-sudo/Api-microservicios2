package com.example.tienda.productos;

import com.example.tienda.common.ApiException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:inventario-test;DB_CLOSE_DELAY=-1")
class InventarioServiceTests {
    @Autowired InventarioService service;
    @Autowired ProductoRepository productos;

    private Producto producto(int stock) {
        return service.guardar(null,new ProductoRequest(UUID.randomUUID().toString(),"Portatil","Lenovo","Computadores",
            "Prueba",new BigDecimal("1250.50"),stock));
    }
    private ReservaRequest solicitud(Producto p,int qty) {
        return new ReservaRequest(List.of(new ReservaRequest.Item(p.id,qty)));
    }
    @Test void reservaYCancelacionSonIdempotentes() {
        Producto p=producto(10);
        String id=UUID.randomUUID().toString();
        Reserva r=service.reservar(id,solicitud(p,2));
        assertThat(r.items.get(0).precioUnitario).isEqualByComparingTo("1250.50");
        service.reservar(id,solicitud(p,2));
        assertThat(service.buscar(p.id).stock).isEqualTo(8);
        assertThatThrownBy(() -> service.reservar(id,solicitud(p,3))).isInstanceOf(ApiException.class);
        service.liberar(id); service.liberar(id);
        assertThat(service.buscar(p.id).stock).isEqualTo(10);
        assertThatThrownBy(() -> service.reservar(id,solicitud(p,2))).isInstanceOf(ApiException.class);
    }
    @Test void falloDeUnProductoRevierteTodaLaReserva() {
        Producto disponible=producto(10), agotado=producto(0);
        ReservaRequest request=new ReservaRequest(List.of(new ReservaRequest.Item(disponible.id,2),new ReservaRequest.Item(agotado.id,1)));
        assertThatThrownBy(() -> service.reservar(UUID.randomUUID().toString(),request)).isInstanceOf(ApiException.class);
        assertThat(service.buscar(disponible.id).stock).isEqualTo(10);
    }
    @Test void dosComprasConcurrentesNoVendenLaMismaUnidad() throws Exception {
        Producto p=producto(1);
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch ready=new CountDownLatch(2), start=new CountDownLatch(1);
        Callable<Boolean> compra=() -> {
            ready.countDown(); start.await();
            try { service.reservar(UUID.randomUUID().toString(),solicitud(p,1)); return true; }
            catch (ApiException e) { return false; }
        };
        try {
            Future<Boolean> a=executor.submit(compra), b=executor.submit(compra);
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(Stream.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS)).filter(Boolean::booleanValue).count()).isEqualTo(1);
            assertThat(service.buscar(p.id).stock).isZero();
        } finally { start.countDown(); executor.shutdownNow(); }
    }
    @Test void cancelacionAnticipadaBloqueaReservasRetrasadas() {
        Producto p=producto(5); String id=UUID.randomUUID().toString();
        service.liberar(id);
        assertThatThrownBy(() -> service.reservar(id,solicitud(p,1))).isInstanceOf(ApiException.class);
        assertThat(service.buscar(p.id).stock).isEqualTo(5);
    }
    @Test void sumaLineasRepetidasYPermiteDevolverProductoDadoDeBaja() {
        Producto p=producto(10); String id=UUID.randomUUID().toString();
        service.reservar(id,new ReservaRequest(List.of(new ReservaRequest.Item(p.id,2),new ReservaRequest.Item(p.id,3))));
        assertThat(service.buscar(p.id).stock).isEqualTo(5);
        service.eliminar(p.id); service.liberar(id);
        assertThat(productos.findById(p.id).orElseThrow().stock).isEqualTo(10);
        assertThatThrownBy(() -> service.buscar(p.id)).isInstanceOf(ApiException.class);
    }
}
