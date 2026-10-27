package com.example.tienda.clientes;
import com.example.tienda.common.ApiException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ClienteService {
    private final ClienteRepository repository;
    public ClienteService(ClienteRepository repository) { this.repository=repository; }
    public Cliente obtener(Long id) {
        return repository.findById(id).filter(c -> c.activo).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Cliente no encontrado"));
    }
    @Transactional
    public Cliente guardar(Long id, ClienteRequest request) {
        Cliente c=id==null ? new Cliente() : repository.bloquear(id).filter(x -> x.activo)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Cliente no encontrado"));
        c.nombre=request.nombre().trim(); c.email=request.email().trim().toLowerCase(Locale.ROOT);
        c.telefono=request.telefono().trim(); c.direccion=request.direccion().trim();
        return repository.saveAndFlush(c);
    }
    @Transactional
    public void eliminar(Long id) {
        Cliente c=repository.bloquear(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"Cliente no encontrado"));
        c.activo=false;
    }
}
