package com.example.tienda.productos;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication(scanBasePackages = {"com.example.tienda.productos", "com.example.tienda.common"})
public class ProductosApplication {
    public static void main(String[] args) { SpringApplication.run(ProductosApplication.class, args); }
}
