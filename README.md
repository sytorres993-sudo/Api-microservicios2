# API de tienda de electronicos

Tres aplicaciones Spring Boot 4.1.2, Java 17+ y Maven. Cada microservicio tiene proceso, puerto y base de datos propios. Se comunican por HTTP; no comparten tablas. `common` contiene el formato de errores.

| Servicio | Puerto | Responsabilidad |
| --- | --- | --- |
| productos-service | 8081 | Catalogo, precios, existencias y reservas |
| clientes-service | 8082 | Clientes y datos de contacto |
| pedidos-service | 8083 | Pedidos, totales, confirmacion y cancelacion |

## Ejecutar en Windows

Desde la raiz, compilar los tres ejecutables y ejecutar las pruebas:

```powershell
.\mvnw.cmd clean verify
```

Abrir tres terminales, todas en la raiz del proyecto:

```powershell
java -jar productos-service/target/productos-service-0.0.1-SNAPSHOT.jar
```

```powershell
java -jar clientes-service/target/clientes-service-0.0.1-SNAPSHOT.jar
```

```powershell
java -jar pedidos-service/target/pedidos-service-0.0.1-SNAPSHOT.jar
```

En IntelliJ, recargar el proyecto Maven raiz y ejecutar `ProductosApplication`, `ClientesApplication` y `PedidosApplication`. Al usar terminales, ejecutar los JAR desde la misma raiz permite mantener los archivos H2 en `data/`. En Linux/macOS se puede usar `sh mvnw clean verify`.

Cada servicio expone `GET /actuator/health`. No hay datos iniciales: Postman crea los ejemplos. Las bases H2 se crean automaticamente y conservan los datos al reiniciar.

## Probar con Postman

1. Importar `postman/Tienda-Electronicos.postman_collection.json`.
2. Importar `postman/Local.postman_environment.json` y seleccionar ese entorno.
3. Con los tres servicios iniciados, ejecutar la coleccion completa con Collection Runner, en el orden indicado.

La coleccion guarda automaticamente `clienteId`, `productoId` y `pedidoId`. Incluye CRUD, filtros, compra, calculo del total, verificacion de existencias, cancelacion repetida y errores esperados. Para peticiones individuales, ejecutar primero las peticiones de creacion de cliente y producto. La carpeta final elimina esos registros mediante baja logica.

## Endpoints

| Servicio | Metodo | Ruta |
| --- | --- | --- |
| Productos | GET / POST | /api/productos |
| Productos | GET / PUT / DELETE | /api/productos/{id} |
| Clientes | GET / POST | /api/clientes |
| Clientes | GET / PUT / DELETE | /api/clientes/{id} |
| Pedidos | GET / POST | /api/pedidos |
| Pedidos | GET | /api/pedidos/{uuid} |
| Pedidos | POST | /api/pedidos/{uuid}/confirmar |
| Pedidos | POST | /api/pedidos/{uuid}/cancelar |
| Inventario interno | PUT / DELETE | /api/reservas/{uuid} |

Filtros: `GET /api/productos?categoria=Computadores&buscar=Lenovo` y `GET /api/pedidos?clienteId=1`.

### JSON de producto

```json
{
  "sku": "LAP-001",
  "nombre": "Portatil Lenovo IdeaPad",
  "marca": "Lenovo",
  "categoria": "Computadores",
  "descripcion": "16 GB RAM, SSD 512 GB",
  "precio": 2499900,
  "stock": 10
}
```

### JSON de cliente

```json
{
  "nombre": "Ana Garcia",
  "email": "ana@example.com",
  "telefono": "+57 3001234567",
  "direccion": "Calle 10 # 20-30, Bogota"
}
```

### JSON de pedido

Usar los IDs devueltos por las peticiones anteriores:

```json
{
  "clienteId": 1,
  "items": [
    { "productoId": 1, "cantidad": 2 }
  ]
}
```

El servidor calcula `precioUnitario`, `subtotal` y `total` con BigDecimal y guarda los precios historicos del pedido. Todos los precios representan COP; no incluye impuestos, envio ni conversion de moneda. PUT reemplaza todos los campos editables; `stock` es la cantidad disponible, excluyendo unidades reservadas.

Las creaciones responden 201 con cabecera Location; las bajas de productos y clientes responden 204. Los errores devuelven JSON con `fecha`, `estado`, `mensaje` y `ruta`: 400 para datos invalidos, 404 si el recurso no existe, 409 por duplicados o falta de stock y 503 si una dependencia no responde. Las validaciones incluyen `errores` por campo.

## Consistencia del inventario

Las reservas descuentan todos los productos en una transaccion local con bloqueos de escritura, evitando descuentos parciales y sobreventa. Las cantidades repetidas de un mismo producto se suman. Cada pedido usa su UUID como identificador de reserva: repetir la confirmacion conserva el mismo precio y no descuenta dos veces.

El pedido se guarda primero como PENDIENTE. Si falla la reserva, el error incluye `pedidoId`; consultar ese pedido y usar `POST /api/pedidos/{pedidoId}/confirmar` para reintentar o `/cancelar` para abandonarlo. No repetir POST de creacion para recuperar ese pedido: cada POST crea uno nuevo. Confirmar y cancelar son operaciones idempotentes.

Cancelar devuelve el inventario una sola vez y marca el pedido CANCELADO. Una reserva cancelada impide que una solicitud retrasada vuelva a descontar stock. Si ocurre un corte entre servicios, repetir la operacion con el mismo ID permite recuperar su estado; no hay una transaccion distribuida ni reconciliacion automatica. Los pedidos conservan el nombre del cliente y los detalles de compra aunque se den de baja los registros del catalogo.

## Pruebas de extremo a extremo

Despues de compilar:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify.ps1
```

El script inicia los tres JAR con bases en memoria y puertos 18081-18083, comprueba las peticiones HTTP y detiene sus procesos al terminar. No modifica las bases de desarrollo.

## Ejecutar con Docker

Con Docker Desktop iniciado (contenedores Linux) y Docker Compose v2, ejecutar desde la raiz:

```powershell
docker compose up --build -d --wait
```

Docker compila los modulos y ejecuta sus pruebas con Maven y Java 17 dentro de una etapa de construccion. No hace falta instalar Java o Maven en el equipo ni generar los JAR previamente. La primera construccion descarga las imagenes y dependencias; las siguientes reutilizan la cache.

Los servicios quedan disponibles en los mismos puertos: productos en http://localhost:8081, clientes en http://localhost:8082 y pedidos en http://localhost:8083. Cada contenedor comprueba `/actuator/health`; pedidos espera a que productos y clientes esten saludables. Se puede usar la misma coleccion y entorno de Postman.

Comprobar el estado y consultar los registros:

```powershell
docker compose ps
docker compose logs -f
```

Cada servicio se ejecuta con un usuario sin privilegios y guarda su base H2 en un volumen propio. Los datos de Docker son independientes de la carpeta local `data/` y se conservan al recrear los contenedores o ejecutar `docker compose down`.

Para detener los servicios:

```powershell
docker compose down
```

Para borrar tambien las bases de Docker y empezar de cero: `docker compose down --volumes` (elimina los datos).

## Configuracion

Variables disponibles: `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`. En pedidos: `PRODUCTOS_URL` y `CLIENTES_URL`. Los timeouts HTTP son 3 segundos de conexion y 5 de lectura.

Es una API de demostracion local sin autenticacion. Las rutas de reserva estan pensadas para comunicacion interna. Antes de publicar, se deben incorporar autenticacion y permisos, proteger esas rutas, gestionar migraciones y definir recuperacion automatica de pedidos pendientes.
