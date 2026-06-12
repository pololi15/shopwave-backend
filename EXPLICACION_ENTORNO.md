# Explicacion Detallada del Entorno Dockerizado ShopWaveFusion

## 1. Estructura de Carpetas

```
shopwave-entorno/
├── backend/                    # Codigo fuente del proyecto
│   ├── pom.xml                # Configuracion Maven
│   ├── src/                   # Codigo fuente Java
│   ├── mvnw, mvnw.cmd        # Scripts Maven wrapper
│   └── .mvn/                 # Configuracion Maven wrapper
├── Dockerfile                  # Multi-stage build para el backend
├── docker-compose.yml          # Orquestacion de contenedores
├── README.md                  # Guia rapida para companeros
└── EXPLICACION_ENTORNO.md     # Este documento
```

El repositorio se clono dentro de `backend/` para que `Dockerfile` pueda accederlo con `COPY backend/pom.xml` y `COPY backend/src`.

## 2. Dockerfile Multi-Stage

```dockerfile
# Stage 1: Build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY backend/pom.xml .
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM amazoncorretto:17
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Etapa 1: Build (Compilacion)
- **Imagen base**: `maven:3.8.5-openjdk-17` - contiene Maven y JDK 17
- **WORKDIR /app**: Directorio de trabajo dentro del contenedor
- **COPY backend/pom.xml .**: Copia solo el pom.xml primero (optimizacion Docker)
- **COPY backend/src ./src**: Copia el codigo fuente
- **RUN mvn clean package -DskipTests**: Compila el proyecto y genera el .jar

### Etapa 2: Run (Ejecucion)
- **FROM amazoncorretto:17**: Imagen ligera con JDK 17 de Amazon (no tiene Maven, reduce tamano)
- **COPY --from=build**: Copia el .jar generado en la etapa anterior
- **EXPOSE 8080**: Documenta que el contenedor usa puerto 8080
- **ENTRYPOINT**: Comando para ejecutar la aplicacion

### Por que Multi-Stage?
- Imagen final mas pequena (solo JDK, sin Maven)
- Seguridad: no se expone el codigo fuente en produccion
- Build separado del runtime

## 3. docker-compose.yml

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: shopwavefusion
    ports:
      - "3306:3306"
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 60s
    volumes:
      - mysql_data:/var/lib/mysql

  backend:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      DB_HOST: mysql
      DB_PORT: "3306"
      DB_NAME: shopwavefusion
      DB_PASSWORD: root
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      SPRING_JPA_SHOW_SQL: "true"
    restart: on-failure:10
    depends_on:
      mysql:
        condition: service_healthy

volumes:
  mysql_data:
```

### Servicio MySQL
- **image: mysql:8.0**: Imagen oficial de MySQL 8
- **environment**: Variables de entorno para configuracion
  - `MYSQL_ROOT_PASSWORD`: Contrasena del root
  - `MYSQL_DATABASE`: Base de datos a crear al iniciar
- **ports**: Mapeo de puertos Host:Contenedor
- **healthcheck**: Verifica que MySQL este listo antes de iniciar el backend
  - `mysqladmin ping`: Prueba de vida de MySQL
  - `start_period: 60s`: Tiempo de espera para que MySQL inicialice
- **volumes**: Persistencia de datos en `mysql_data`

### Servicio Backend
- **build**: Indica que debe construir la imagen desde el Dockerfile
- **context: .**: Contexto de construccion (carpeta actual)
- **ports**: Expone puerto 8080 al host
- **environment**: Variables de entorno para Spring Boot
  - `DB_HOST: mysql`: Nombre del servicio MySQL en la red Docker
  - `DB_PORT`, `DB_NAME`, `DB_PASSWORD`: Configuracion de conexion
- **restart: on-failure:10**: Reinicia automaticamente si falla (max 10 intentos)
- **depends_on + condition: service_healthy**: Espera a que MySQL este healthy

## 4. Conexion entre Backend y Base de Datos

### La "Magia" de Docker Networking

Cuando docker-compose levanta los servicios, crea una **red Docker** llamada `shopwave-entorno_default`. Dentro de esta red:

1. **MySQL** esta disponible como `mysql:3306` (nombre del servicio + puerto)
2. **Backend** se conecta usando ese nombre como host
3. Docker resuelve `mysql` a la IP del contenedor MySQL

### Flujo de Conexion

```
Backend (Spring Boot)
    |
    | JDBC URL: jdbc:mysql://mysql:3306/shopwavefusion
    v
Docker Network (shopwave-entorno_default)
    |
    v
MySQL Container (mysql:3306)
```

### Configuracion de la Base de Datos

El backend usa MySQL cuando encuentra variables de entorno de base de datos. Si no estan disponibles, por ejemplo en Render sin un servicio MySQL externo, arranca con H2 en memoria para no fallar en el startup.

En local con Docker, el flujo sigue usando MySQL:
```properties
spring.datasource.url=jdbc:mysql://mysql:3306/shopwavefusion
spring.datasource.username=root
spring.datasource.password=root
```

Las variables de entorno en `docker-compose.yml` sobrescriben los valores default.

## 5. Healthcheck y Orden de Arranque

El `healthcheck` asegura que MySQL esta completamente inicializado antes de iniciar el backend:

1. docker-compose inicia MySQL
2. Espera a que `start_period: 60s` termine
3. Ejecuta `mysqladmin ping` cada 10s
4. Si MySQL responde, marca como `healthy`
5. Solo entonces inicia el backend
6. `depends_on` con `condition: service_healthy` hace toda la magia

## 6. Persistencia de Datos

El volumen `mysql_data` guarda los datos de MySQL:
- Si ejecutas `docker-compose down`, los datos persisten
- Si ejecutas `docker-compose down -v`, se elimina el volumen

## 7. Comandos Utilizados

```bash
# Iniciar todo
docker-compose up --build -d

# Ver estado
docker ps

# Ver logs
docker logs shopwave-entorno-backend-1
docker logs shopwave-entorno-mysql-1

# Detener
docker-compose down

# Reiniciar limpio
docker-compose down -v
docker-compose up --build -d
```

## 8. Verificacion del Entorno

```bash
# Estado de contenedores
docker ps --format "table {{.Names}}\t{{.Status}}"

# Test endpoint raiz
curl http://localhost:8080/

# Test Swagger
curl http://localhost:8080/swagger-ui/index.html
```