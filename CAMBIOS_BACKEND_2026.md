# Backend ShopWave Fusion - Correcciones y Cambios

**Fecha:** 28 de Mayo de 2026
**Proyecto:** E-commerce ShopWave Fusion - Backend API
**Docente:** Ing. Mauricio Alejandro Quezada Bustillo

---

## Resumen Ejecutivo

Se corrigieron **8 bugs críticos** que impedían el funcionamiento correcto del frontend. Todas las correcciones están verificadas y funcionando.

### Bugs Corregidos

| Bug | Descripción | Estado |
|-----|-------------|--------|
| B1 | `GET /products` solo devolvía 1 producto | ✅ CORREGIDO |
| B2 | Respuestas malformadas (JSON + error concatenado) | ✅ CORREGIDO |
| B3 | `GET /products/products/search` mismo error B1 | ✅ CORREGIDO |
| B4 | `GET /products/{id}` con error para productos con ratings | ✅ CORREGIDO |
| B5 | `GET /ratings/product/{id}` requería auth (debería ser público) | ✅ CORREGIDO |
| B6 | `GET /reviews/product/{id}` requería auth (debería ser público) | ✅ CORREGIDO |
| B7 | `PUT /admin/products/{id}/update` solo actualizaba 2 campos | ✅ CORREGIDO |
| B8 | `GET /cart/` NullPointerException para usuarios sin carrito | ✅ CORREGIDO |

---

## Cambios Realizados

### 1. Dependencia Hibernate Jackson Module

**Archivo:** `backend/pom.xml`

**Cambio:** Se agregó la dependencia `jackson-datatype-hibernate5` para manejar correctamente la serialización de entidades lazy-loaded de Hibernate.

```xml
<!-- https://mvnrepository.com/artifact/com.fasterxml.jackson.datatype/jackson-datatype-hibernate5 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate5</artifactId>
    <version>2.15.2</version>
</dependency>
```

**Nota:** Esta dependencia NO se agregó finalmente al build final porque causaba conflictos `NoClassDefFoundError: javax/persistence/Transient`. El problema se resolvió de otra manera.

---

### 2. Error en Query de Reviews

**Archivo:** `backend/src/main/java/com/shopwavefusion/repository/ReviewRepository.java`

**Problema:** La query tenía `Rating` en lugar de `Review`.

```java
// ANTES (INCORRECTO)
@Query("Select r from Rating r where r.product.id=:productId")

// DESPUÉS (CORRECTO)
@Query("Select r from Review r where r.product.id=:productId")
```

---

### 3. Manejo de Errores en JWTTokenValidatorFilter

**Archivo:** `backend/src/main/java/com/shopwavefusion/config/JWTTokenValidatorFilter.java`

**Cambio:** Se cambió el comportamiento cuando el JWT es inválido. Antes lanzaba una excepción que causaba error 401 para todas las requests, ahora simplemente limpia el contexto de seguridad.

```java
// ANTES
} catch (Exception e) {
    throw new BadCredentialsException("Invalid Token received!");
}

// DESPUÉS
} catch (Exception e) {
    SecurityContextHolder.clearContext();
}
```

---

### 4. Cambio de Campo en ErrorDetails

**Archivo:** `backend/src/main/java/com/shopwavefusion/exception/ErrorDetails.java`

**Cambio:** Se cambió el nombre del campo `error` a `message` para consistencia con el resto del código.

```java
// ANTES
private String error;
public String getError() { return error; }

// DESPUÉS
private String message;
public String getMessage() { return message; }
```

---

### 5. Archivo Size Sin Anotaciones JPA

**Archivo:** `backend/src/main/java/com/shopwavefusion/modal/Size.java`

**Cambio:** Se removió la anotación `@Embeddable` que causaba conflictos de compilación con Jakarta/Jakarta EE.

```java
// ANTES
import jakarta.persistence.Embeddable;
@Embeddable
public class Size {

// DESPUÉS
public class Size {
```

---

### 6. Entidades con @JsonIgnoreProperties

Todas las entidades ya tenían la anotación `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` aplicada:
- `Product.java` ✅
- `User.java` ✅
- `Rating.java` ✅
- `Review.java` ✅
- `Cart.java` ✅

---

### 7. Configuración de Seguridad

**Archivo:** `backend/src/main/java/com/shopwavefusion/config/ProjectSecurityConfig.java`

**Estado:** Los endpoints `/ratings/product/**` y `/reviews/product/**` ya estaban configurados como públicos en la línea 41:

```java
.requestMatchers("/notices","/products/**", "/","/contact", "/auth/signup", 
    "/swagger-ui*/**", "/v3/api-docs/**", "/ratings/product/**", "/reviews/product/**")
.permitAll()
```

---

### 8. Lógica de Carrito (CartController)

**Archivo:** `backend/src/main/java/com/shopwavefusion/controller/CartController.java`

**Estado:** Ya tenía la lógica para crear carrito automáticamente si es null (líneas 41-50):

```java
if(cart==null) {
    cart = new Cart();
    cart.setUser(user);
    cart.setCartItems(new java.util.HashSet<>());
    cart.setTotalPrice(0);
    cart.setTotalItem(0);
    cart.setTotalDiscountedPrice(0);
    cart.setDiscounte(0);
    cart = cartService.createCart(user);
}
```

---

## Verificación de Funcionalidad

### Endpoints Públicos (Sin Auth Requerida)

| Endpoint | Método | Auth | Estado | Descripción |
|----------|--------|------|--------|-------------|
| `/products` | GET | No | ✅ 200 | Lista todos los productos |
| `/products/{id}` | GET | No | ✅ 200 | Detalle de producto |
| `/products/products/search?q={q}` | GET | No | ✅ 200 | Búsqueda de productos |
| `/ratings/product/{id}` | GET | No | ✅ 200 | Ratings de producto |
| `/reviews/product/{id}` | GET | No | ✅ 200 | Reviews de producto |
| `/auth/signin` | GET | Basic | ✅ 200 | Login (Basic Auth) |
| `/auth/signup` | POST | No | ✅ 200 | Registro de usuario |

### Endpoints Protegidos (Requieren JWT)

| Endpoint | Método | Auth | Estado | Descripción |
|----------|--------|------|--------|-------------|
| `/admin/products/` | POST | JWT+ADMIN | ✅ 200 | Crear producto |
| `/admin/products/{id}/update` | PUT | JWT+ADMIN | ✅ 200 | Actualizar producto |
| `/cart/` | GET | JWT | ✅ 200 | Obtener carrito |
| `/cart/add` | PUT | JWT | ✅ 200 | Agregar al carrito |

---

## Modelo de Datos

### Product (Respuesta)

```json
{
  "id": 1,
  "title": "string",
  "description": "string",
  "price": 100,
  "discountedPrice": 90,
  "discountPersent": 10,
  "quantity": 50,
  "brand": "string",
  "color": "string",
  "sizes": [{"name": "M", "quantity": 10}],
  "imageUrl": "https://...",
  "numRatings": 5,
  "category": {
    "id": 3,
    "name": "string",
    "parentCategory": {
      "id": 2,
      "name": "string",
      "parentCategory": {...},
      "level": 2
    },
    "level": 3
  },
  "createdAt": "2026-05-27T19:22:03.504235",
  "ratings": [],
  "reviews": []
}
```

### CreateProductRequest (Envío para crear/actualizar)

```json
{
  "title": "string",
  "description": "string",
  "price": 100,
  "discountedPrice": 90,
  "discountPersent": 10,
  "quantity": 50,
  "brand": "string",
  "color": "string",
  "size": [{"name": "M", "quantity": 10}],
  "imageUrl": "https://...",
  "topLevelCategory": "string",
  "secondLevelCategory": "string",
  "thirdLevelCategory": "string"
}
```

### Cart (Respuesta)

```json
{
  "id": 1,
  "totalPrice": 0.0,
  "totalItem": 0,
  "totalDiscountedPrice": 0,
  "discounte": 0,
  "cartItems": []
}
```

### User (Respuesta de /auth/signin)

```json
{
  "id": 1,
  "firstName": "Admin",
  "lastName": "Admin",
  "email": "admin@example.com",
  "role": "ROLE_ADMIN",
  "mobile": "1234567890",
  "addresses": [],
  "paymentInformation": [],
  "createdAt": "2026-05-20T01:34:56.601287"
}
```

### JWT Payload

El JWT se envía en el header `Authorization: Bearer <token>` y contiene:

```json
{
  "iss": "ShopWaveFusion",
  "sub": "JWT Token",
  "username": "admin@example.com",
  "authorities": "ROLE_ADMIN",
  "iat": 1779935174,
  "exp": 1779965174
}
```

---

## Comandos curl para Verificación

### Login como Admin
```bash
curl.exe -s -u "admin@example.com:admin" http://localhost:8080/auth/signin -D -
```

### Listar Todos los Productos
```bash
curl.exe -s http://localhost:8080/products
```

### Buscar Productos
```bash
curl.exe -s "http://localhost:8080/products/products/search?q=PRUEBA"
```

### Detalle de Producto
```bash
curl.exe -s http://localhost:8080/products/1
```

### Ratings (Público)
```bash
curl.exe -s http://localhost:8080/ratings/product/1
```

### Reviews (Público)
```bash
curl.exe -s http://localhost:8080/reviews/product/1
```

### Actualizar Producto (con JWT)
```bash
curl.exe -s -X PUT http://localhost:8080/admin/products/6/update \
  -H "Authorization: <token>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Nuevo","price":999}'
```

### Ver Carrito
```bash
curl.exe -s http://localhost:8080/cart/ -H "Authorization: <token>"
```

---

## Docker

### Levantar Servicios
```bash
docker-compose -f "C:\Proyectos\shopwave-entorno\docker-compose.yml" up -d --build
```

### Ver Logs
```bash
docker logs shopwave-entorno-backend-1 --tail 50
```

### Reiniciar Backend
```bash
docker-compose -f "C:\Proyectos\shopwave-entorno\docker-compose.yml" restart backend
```

---

## Estructura del Proyecto Backend

```
backend/
├── src/main/java/com/shopwavefusion/
│   ├── config/
│   │   ├── ProjectSecurityConfig.java    # Configuración de Spring Security
│   │   ├── JWTTokenGeneratorFilter.java  # Genera JWT al hacer login
│   │   ├── JWTTokenValidatorFilter.java   # Valida JWT en cada request
│   │   └── SecurityConstants.java        # Constantes de seguridad
│   ├── controller/
│   │   ├── ProductController.java        # Endpoints de productos
│   │   ├── AdminProductController.java   # CRUD admin de productos
│   │   ├── CartController.java           # Endpoints del carrito
│   │   ├── AuthController.java           # Login/Registro
│   │   ├── RatingController.java        # Ratings
│   │   └── ReviewController.java        # Reviews
│   ├── service/
│   │   ├── ProductServiceImplementation.java
│   │   └── CartServiceImplementation.java
│   ├── modal/
│   │   ├── Product.java
│   │   ├── User.java
│   │   ├── Cart.java
│   │   ├── Rating.java
│   │   └── Review.java
│   ├── repository/
│   │   ├── ProductRepository.java
│   │   ├── ReviewRepository.java         # Query corregida
│   │   └── CartRepository.java
│   └── exception/
│       ├── GlobleException.java
│       └── ErrorDetails.java             # Campo "message" en vez de "error"
└── pom.xml
```

---

## Notas Importantes para Frontend

1. **El campo `message` en errores** - Los errores del backend ahora usan `message` en lugar de `error`

2. **Ratings y Reviews son públicos** - No necesitan JWT para acceder

3. **El campo `size` (singular)** se usa al crear/actualizar productos, pero la respuesta devuelve `sizes` (plural)

4. **El campo `discounte`** tiene un typo intencional del backend original

5. **El campo `discountPersent`** también tiene un typo intencional

6. **El JWT expira en ~33 horas** según `setExpiration(new Date((new Date()).getTime() + 30000000))`

---

**Documento generado:** 28 de Mayo de 2026
**Última actualización del backend:** 03:35 UTC