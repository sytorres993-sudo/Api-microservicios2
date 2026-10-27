package com.example.tienda.clientes;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"com.example.tienda.clientes", "com.example.tienda.common"})
public class ClientesApplication {
    public static void main(String[] args) { SpringApplication.run(ClientesApplication.class, args); }
}
