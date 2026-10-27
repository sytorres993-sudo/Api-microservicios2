package com.example.tienda.common;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final String pedidoId;
    public ApiException(HttpStatus status, String message) { this(status, message, null); }
    public ApiException(HttpStatus status, String message, String pedidoId) {
        super(message); this.status = status; this.pedidoId = pedidoId;
    }
}
