package com.example.tienda.pedidos;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"com.example.tienda.pedidos", "com.example.tienda.common"})
public class PedidosApplication {
    public static void main(String[] args) { SpringApplication.run(PedidosApplication.class, args); }
}
