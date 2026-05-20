# ShopWaveFusion Backend - Entorno Dockerizado

Backend del proyecto ShopWaveFusion configurado para ejecutarse en Docker.

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop) instalado y ejecutandose

## Instalacion y Ejecucion

1. Abrir una terminal en la carpeta `shopwave-entorno`
2. Ejecutar:

```bash
docker-compose up -d
```

3. Esperar ~60 segundos a que MySQL este listo y el backend arranque
4. Acceder a http://localhost:8080

## Endpoints

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Root: http://localhost:8080/

## Servicios

| Servicio | Puerto | Descripcion |
|----------|--------|-------------|
| backend  | 8080   | API Spring Boot |
| mysql    | 3306   | Base de datos MySQL 8.0 |

## Detener

```bash
docker-compose down
```

## Reiniciar (limpio)

```bash
docker-compose down -v
docker-compose up --build -d
```