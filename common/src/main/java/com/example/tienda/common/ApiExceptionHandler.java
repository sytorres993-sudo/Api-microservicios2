package com.example.tienda.common;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiExceptionHandler {
    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message, HttpServletRequest request, Map<String, ?> extra) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fecha", Instant.now()); body.put("estado", status.value());
        body.put("mensaje", message); body.put("ruta", request.getRequestURI()); body.putAll(extra);
        return ResponseEntity.status(status).body(body);
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> business(ApiException ex, HttpServletRequest req) {
        return error(ex.status, ex.getMessage(), req, ex.pedidoId == null ? Map.of() : Map.of("pedidoId", ex.pedidoId));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Datos invalidos", req, Map.of("errores", fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class, ConstraintViolationException.class})
    ResponseEntity<?> malformed(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.BAD_REQUEST, "JSON, parametros o identificador invalidos", req, Map.of());
    }
    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    ResponseEntity<?> conflict(Exception ex, HttpServletRequest req) {
        return error(HttpStatus.CONFLICT, "Conflicto de datos: SKU/email duplicado o modificacion concurrente. Consulte los datos y reintente.", req, Map.of());
    }
}
