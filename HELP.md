# Tienda de electronicos

La documentacion del proyecto esta en [README.md](README.md).

- Productos: http://localhost:8081/api/productos
- Clientes: http://localhost:8082/api/clientes
- Pedidos: http://localhost:8083/api/pedidos
- Coleccion Postman: [Tienda-Electronicos.postman_collection.json](postman/Tienda-Electronicos.postman_collection.json)
- Entorno Postman: [Local.postman_environment.json](postman/Local.postman_environment.json)

Compilar y probar: ` .\mvnw.cmd clean verify `.

Con Docker Desktop iniciado, levantar todo sin compilar previamente: `docker compose up --build -d --wait`.
Ver estado: `docker compose ps`. Ver registros: `docker compose logs -f`.
Detener conservando las bases: `docker compose down`.

