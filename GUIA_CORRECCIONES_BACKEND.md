# Guía de Correcciones del Backend - ShopWave Fusion

> **Fecha:** 28 de Mayo de 2026
> **Elaborado por:** Equipo Frontend
> **Destinatario:** Agente Backend
> **Propósito:** Documento técnico con todos los bugs del backend que bloquean al frontend, con evidencia curl y soluciones sugeridas.

---

## Tabla de Contenidos

1. [Contexto del Proyecto](#contexto-del-proyecto)
2. [Resumen Ejecutivo](#resumen-ejecutivo)
3. [Configuración de Pruebas](#configuración-de-pruebas)
4. [BUGS - Entrega 60% (PRIORIDAD CRÍTICA)](#bugs---entrega-60-prioridad-crítica)
5. [BUGS - Entrega 100% (PRIORIDAD ALTA)](#bugs---entrega-100-prioridad-alta)
6. [Mapa Completo de Endpoints Esperados](#mapa-completo-de-endpoints-esperados)
7. [Modelos de Datos que Espera el Frontend](#modelos-de-datos-que-espera-el-frontend)
8. [Checklist de Verificación](#checklist-de-verificación)

---

## Contexto del Proyecto

**Universidad:** Universidad Católica Boliviana "San Pablo"
**Materia:** Proyecto final de Frontend - E-commerce con Next.js
**Docente:** Ing. Mauricio Alejandro Quezada Bustillo
**Backend base:** https://github.com/DEEPAKKUMARMAHASETH/shopwavefusionbackend

### Entregas

| Entrega | Fecha | Contenido |
|---------|-------|-----------|
| **60%** | **30 de Mayo 2026 (pasado mañana)** | Login, Registro, JWT, Logout, Navbar, Home, Listado productos, Detalle producto, Servicios API, Modelos TS, Guards |
| 100% | ~2 semanas después | Todo lo anterior + Carrito, Checkout, Órdenes, Perfil, Panel Admin, CRUD productos, Validaciones |

### Stack Frontend

- Next.js 16.2.6 (App Router)
- React 19.2.4 + TypeScript strict
- Tailwind CSS v4
- JWT en localStorage (clave: `shopwave_token`)
- Proxy Next.js: `/api/*` → `http://localhost:8080/*`

---

## Resumen Ejecutivo

Se han identificado **8 bugs del backend** que afectan directamente al frontend. Están organizados por la entrega que bloquean:

| # | Bug | Entrega | Severidad | Estado |
|---|-----|---------|-----------|--------|
| B1 | `GET /products` solo devuelve 1 producto + error de serialización | 60% | **CRÍTICA** | Bloqueante |
| B2 | `GET /products` respuesta malformada (JSON + error concatenados) | 60% | **CRÍTICA** | Bloqueante |
| B3 | `GET /products/products/search` mismo error de serialización | 60% | **CRÍTICA** | Bloqueante |
| B4 | `GET /products/{id}` respuesta malformada (JSON + error concatenados) | 60% | **ALTA** | Bloqueante |
| B5 | `GET /ratings/product/{id}` requiere auth (debería ser público) | 60% | **MEDIA** | Bloqueante |
| B6 | `GET /reviews/product/{id}` requiere auth (debería ser público) | 60% | **MEDIA** | Bloqueante |
| B7 | `PUT /admin/products/{id}/update` solo actualiza 2 de 12 campos | 100% | **CRÍTICA** | Bloqueante |
| B8 | `GET /cart/` falla con NullPointerException si no hay carrito | 100% | **ALTA** | Bloqueante |

**Total de bugs: 8** (4 afectan la entrega del 60%, 4 afectan la entrega del 100%)

---

## Configuración de Pruebas

### Obtener token JWT de administrador

```bash
# Login con admin (credenciales: admin@example.com / admin)
curl -s -u "admin@example.com:admin" http://localhost:8080/auth/signin -v 2>&1
```

**Respuesta esperada:**
- Status: `200 OK`
- Header `Authorization`: JWT token (sin prefijo `Bearer`)
- Body: objeto User con `role: "ROLE_ADMIN"`

**Guardar token para pruebas posteriores:**
```bash
# El token se obtiene del header Authorization de la respuesta
# Ejemplo de token obtenido:
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJTaG9wV2F2ZUZ1c2lvbiIsInN1YiI6IkpXVCBUb2tlbiIsInVzZXJuYW1lIjoiYWRtaW5AZXhhbXBsZS5jb20iLCJhdXRob3JpdGllcyI6IlJPTEVfQURNSU4iLCJpYXQiOjE3Nzk5MzUxNzQsImV4cCI6MTc3OTk2NTE3NH0.JVDIvEIdXglZSSjfKwC7Kil7AwHn3SQfdYOlUIuukzQ"
```

### Obtener token JWT de usuario normal

```bash
# Registrar usuario
curl -s -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Test","lastName":"User","email":"testuser@example.com","password":"test1234","mobile":"1234567890"}'

# Login con usuario normal
curl -s -u "testuser@example.com:test1234" http://localhost:8080/auth/signin -v 2>&1
```

---

## BUGS - Entrega 60% (PRIORIDAD CRÍTICA)

> Estos bugs bloquean la entrega del 60% que es el **30 de Mayo de 2026**.

---

### BUG B1: `GET /products` solo devuelve 1 producto y omite todos los demás

**Severidad:** CRÍTICA - Bloquea Home y Listado de Productos
**Afecta:** Página Home (`/`), Página de Productos (`/products`), Panel Admin (`/admin/products`)

#### Descripción

El endpoint `GET /products` solo devuelve el producto con ID 1. Los productos creados posteriormente (IDs 6, 7, 8, 10, 11, etc.) **NO aparecen** en la respuesta, aunque sí existen en la base de datos (se pueden obtener individualmente con `GET /products/{id}`).

#### Evidencia

**Paso 1: Crear un producto nuevo (éxito)**
```bash
curl -s -X POST http://localhost:8080/admin/products/ \
  -H "Authorization: $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Producto Nuevo",
    "description": "Producto recién creado",
    "price": 299,
    "discountedPrice": 249,
    "discountPersent": 17,
    "quantity": 50,
    "brand": "TestBrand",
    "color": "rojo",
    "size": [{"name": "M", "quantity": 25}],
    "imageUrl": "https://example.com/test.jpg",
    "topLevelCategory": "Ropa",
    "secondLevelCategory": "Camisas",
    "thirdLevelCategory": "Formales"
  }'
```
**Respuesta:** `{"id":10, "title":"Producto Nuevo", ...}` (creación exitosa)

**Paso 2: Verificar que existe por ID (éxito)**
```bash
curl -s http://localhost:8080/products/10
```
**Respuesta:** `{"id":10, "title":"Producto Nuevo", ...}` (existe correctamente)

**Paso 3: Listar todos los productos (FALLA)**
```bash
curl -s http://localhost:8080/products
```
**Respuesta actual (INCORRECTA):**
```
[{"id":1,"title":"PRUEBA",...}]{"error":"Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]","details":"uri=/products","timestamp":"2026-05-28T02:26:43.848572842"}
```

**Respuesta esperada (CORRECTA):**
```json
[
  {"id":1, "title":"PRUEBA", ...},
  {"id":6, "title":"Test Frontend Format", ...},
  {"id":7, "title":"Producto Sin Categoría", ...},
  {"id":8, "title":"Producto de Prueba Final", ...},
  {"id":10, "title":"Producto Nuevo", ...}
]
```

#### Impacto en el Frontend

| Página | Componente | Efecto |
|--------|-----------|--------|
| `/` (Home) | `ProductList` con `ProductService.getProducts(0, 8)` | Solo muestra 1 producto en la sección "Productos destacados" |
| `/products` | `useProducts` hook → `ProductService.getFilteredProducts()` | Solo muestra 1 producto, filtros no funcionan |
| `/admin/products` | `AdminProductService.getAll()` | Panel admin no ve productos creados |

#### Causa raíz probable

El error `ByteBuddyInterceptor` indica que Hibernate está intentando serializar un proxy lazy-loaded del producto ID 1 (que tiene `ratings` con relaciones anidadas a `User`). Cuando Jackson falla al serializar este proxy, **la serialización del array completo se interrumpe**, y solo se alcanza a escribir el primer elemento antes del error.

Esto significa que:
1. El producto ID 1 tiene `ratings` con un `User` anidado que es un proxy de Hibernate
2. Al intentar serializar el segundo producto (o al terminar el primero), Jackson falla
3. El resultado es un array truncado + mensaje de error concatenado

#### Solución sugerida

**Opción A (Rápida): Usar `@JsonIgnoreProperties` en las entidades con relaciones**
```java
// En todas las entidades que tengan relaciones @ManyToOne o @OneToMany
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    // ...
}

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    // ...
}

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Rating {
    // ...
}
```

**Opción B (Recomendada): Usar DTOs para las respuestas**
```java
@GetMapping("/products")
public List<ProductDTO> getAllProducts() {
    return productService.findAll().stream()
        .map(ProductDTO::fromEntity)
        .collect(Collectors.toList());
}
```

**Opción C: Forzar carga eager con JOIN FETCH**
```java
@Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes LEFT JOIN FETCH p.ratings")
List<Product> findAllProducts();
```

#### Verificación post-fix

```bash
# Debe devolver TODOS los productos sin errores concatenados
curl -s http://localhost:8080/products | python -m json.tool

# Verificar que el JSON es válido (no debe haber texto después del array)
curl -s http://localhost:8080/products | python -c "import json,sys; json.load(sys.stdin); print('JSON válido')"
```

---

### BUG B2: Respuestas malformadas (JSON válido + error concatenado)

**Severidad:** CRÍTICA - Rompe el parser JSON del frontend
**Afecta:** TODOS los endpoints que devuelven entidades con relaciones Hibernate

#### Descripción

Múltiples endpoints del backend devuelven una respuesta que contiene JSON válido seguido inmediatamente por un objeto JSON de error. Esto produce una respuesta que **no es JSON válido** y requiere un parser personalizado en el frontend para extraer solo la parte útil.

#### Evidencia

**Endpoints afectados (verificados con curl):**

```bash
# GET /products
curl -s http://localhost:8080/products
# Respuesta: [{"id":1,...}]{"error":"Type definition error..."}

# GET /products/1
curl -s http://localhost:8080/products/1
# Respuesta: {"id":1,...}{"error":"Type definition error..."}

# GET /products/products/search?q=PRUEBA
curl -s "http://localhost:8080/products/products/search?q=PRUEBA"
# Respuesta: [{"id":1,...}]{"error":"Type definition error..."}

# GET /ratings/product/1 (con JWT)
curl -s "http://localhost:8080/ratings/product/1" -H "Authorization: $ADMIN_TOKEN"
# Respuesta: [{"id":1,...}]{"error":"Type definition error..."}
```

**Endpoints NO afectados (JSON limpio):**

```bash
# GET /products/10 (producto sin ratings)
curl -s http://localhost:8080/products/10
# Respuesta: {"id":10,...}  ← JSON limpio, sin error

# GET /users/profile (con JWT)
curl -s "http://localhost:8080/users/profile" -H "Authorization: $ADMIN_TOKEN"
# Respuesta: {"id":1,...}  ← JSON limpio, sin error
```

#### Patrón observado

El error aparece **solo cuando la entidad tiene relaciones lazy-loaded que incluyen proxies de Hibernate**:
- Producto ID 1 tiene `ratings` → `User` (proxy) → error
- Producto ID 10 no tiene `ratings` → sin error
- `User` en `/users/profile` no tiene relaciones lazy → sin error

#### Impacto en el Frontend

El frontend implementó un parser personalizado (`parseJsonSafe` en `api.service.ts`) que:
1. Busca el primer `[` o `{` en la respuesta
2. Balancea brackets/braces para extraer solo el JSON válido
3. Reemplaza `"hibernateLazyInitializer"` con `null`
4. Parsea solo la porción extraída

**Este workaround funciona PERO:**
- Es frágil y puede fallar con respuestas más complejas
- Oculta errores reales del backend
- Agrega complejidad innecesaria al frontend

#### Solución sugerida

**Solución definitiva: Corregir la serialización de Hibernate** (ver BUG B1, Opción A o B)

Una vez corregido B1, este bug se resuelve automáticamente ya que la causa raíz es la misma.

#### Verificación post-fix

```bash
# Todos estos comandos deben devolver JSON válido sin texto extra
curl -s http://localhost:8080/products | python -c "import json,sys; json.load(sys.stdin); print('OK')"
curl -s http://localhost:8080/products/1 | python -c "import json,sys; json.load(sys.stdin); print('OK')"
curl -s "http://localhost:8080/products/products/search?q=PRUEBA" | python -c "import json,sys; json.load(sys.stdin); print('OK')"
```

---

### BUG B3: `GET /products/products/search` tiene el mismo error de serialización

**Severidad:** CRÍTICA - Bloquea la búsqueda de productos
**Afecta:** Página de Productos (`/products`) cuando el usuario busca por texto

#### Descripción

El endpoint de búsqueda de productos tiene el mismo error de serialización de Hibernate que `GET /products`.

#### Evidencia

```bash
curl -s "http://localhost:8080/products/products/search?q=PRUEBA"
```

**Respuesta actual:**
```
[{"id":1,...}]{"error":"Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]",...}
```

**Respuesta esperada:**
```json
[{"id":1, "title":"PRUEBA", ...}]
```

#### Impacto en el Frontend

El hook `useProducts` llama a `ProductService.searchProducts(query)` cuando el usuario escribe en el buscador. La búsqueda funciona pero solo devuelve 1 resultado debido al error de serialización.

#### Solución

Misma solución que BUG B1 (corregir serialización de Hibernate).

---

### BUG B4: `GET /products/{id}` respuesta malformada para productos con ratings

**Severidad:** ALTA - Puede romper la página de detalle de producto
**Afecta:** Página de Detalle de Producto (`/products/[id]`)

#### Descripción

Cuando se solicita un producto que tiene ratings (como el ID 1), la respuesta incluye el error de serialización de Hibernate concatenado al JSON.

#### Evidencia

```bash
# Producto CON ratings (FALLA)
curl -s http://localhost:8080/products/1
```
**Respuesta:**
```
{"id":1,"title":"PRUEBA",...,"ratings":[{"id":1,"user":{...,"hibernateLazyInitializer"}}]}{"error":"Type definition error..."}
```

```bash
# Producto SIN ratings (OK)
curl -s http://localhost:8080/products/10
```
**Respuesta:**
```json
{"id":10,"title":"Producto Test Creacion",...,"ratings":[],"reviews":[],"numRatings":0,"category":{...}}
```

#### Impacto en el Frontend

El parser `parseJsonSafe` del frontend logra extraer el JSON válido, pero:
1. El campo `hibernateLazyInitializer` aparece en el objeto `User` anidado dentro de `ratings`
2. El frontend lo reemplaza con `null`, lo cual no causa error pero es dato basura
3. Si el parser falla en algún caso edge, la página de detalle muestra error

#### Solución

Misma solución que BUG B1 (corregir serialización de Hibernate).

Adicionalmente, el objeto `ratings[].user` no debería incluir campos como `addresses`, `paymentInformation`, `hibernateLazyInitializer`. Se recomienda usar un DTO simplificado para el User anidado en ratings:

```java
public class UserSummaryDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
```

---

### BUG B5: `GET /ratings/product/{id}` requiere autenticación (debería ser público)

**Severidad:** MEDIA - Bloquea la visualización de calificaciones en detalle de producto
**Afecta:** Página de Detalle de Producto (`/products/[id]`)

#### Descripción

El endpoint `GET /ratings/product/{id}` devuelve `401 Unauthorized` cuando se accede sin token JWT. Según la documentación del proyecto (Postman collection), este endpoint debería ser **público** (no requiere JWT).

#### Evidencia

```bash
# Sin token (FALLA - debería ser público)
curl -s http://localhost:8080/ratings/product/1 -v 2>&1
```
**Respuesta:**
```
< HTTP/1.1 401
< WWW-Authenticate: Basic realm="Realm"
Content-Length: 0
```

```bash
# Con token (funciona pero con error de serialización)
curl -s "http://localhost:8080/ratings/product/1" -H "Authorization: $ADMIN_TOKEN"
```
**Respuesta:**
```
[{"id":1,"user":{...}}]{"error":"Type definition error..."}
```

#### Impacto en el Frontend

El servicio `RatingService.getByProduct(productId)` envía el JWT (porque `requireAuth = true` en el servicio). Sin embargo, la página de detalle de producto **actualmente NO llama a este servicio** (pasa `averageRating={0}` hardcodeado). Si se quiere mostrar ratings reales, este endpoint debe ser público.

#### Solución sugerida

```java
// En SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/ratings/product/**").permitAll()  // ← Agregar esto
        .requestMatchers("/reviews/product/**").permitAll()  // ← Agregar esto
        // ... resto de configuración
    );
    return http.build();
}
```

#### Verificación post-fix

```bash
# Sin token debe devolver 200
curl -s http://localhost:8080/ratings/product/1 -v 2>&1 | grep "HTTP/"
# Esperado: HTTP/1.1 200
```

---

### BUG B6: `GET /reviews/product/{id}` requiere autenticación (debería ser público)

**Severidad:** MEDIA - Bloquea la visualización de reseñas en detalle de producto
**Afecta:** Página de Detalle de Producto (`/products/[id]`)

#### Descripción

Mismo problema que BUG B5. El endpoint `GET /reviews/product/{id}` devuelve `401 Unauthorized` sin token.

#### Evidencia

```bash
curl -s http://localhost:8080/reviews/product/1 -v 2>&1
```
**Respuesta:**
```
< HTTP/1.1 401
< WWW-Authenticate: Basic realm="Realm"
Content-Length: 0
```

#### Solución

Misma solución que BUG B5 (agregar `/reviews/product/**` a `permitAll()`).

---

## BUGS - Entrega 100% (PRIORIDAD ALTA)

> Estos bugs bloquean la entrega del 100% que es en ~2 semanas.

---

### BUG B7: `PUT /admin/products/{id}/update` solo actualiza `description` y `quantity`

**Severidad:** CRÍTICA - Bloquea la edición de productos en el panel admin
**Afecta:** Panel Admin - Editar Producto (`/admin/products/edit/[id]`)

#### Descripción

El endpoint `PUT /admin/products/{id}/update` solo actualiza los campos `description` y `quantity`. Los demás 10+ campos (`title`, `price`, `discountedPrice`, `discountPersent`, `brand`, `color`, `imageUrl`, `size`, `topLevelCategory`, `secondLevelCategory`, `thirdLevelCategory`) se ignoran completamente.

#### Evidencia

**Paso 1: Crear producto de prueba**
```bash
curl -s -X POST http://localhost:8080/admin/products/ \
  -H "Authorization: $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Original Title",
    "description": "Original Description",
    "price": 100,
    "discountedPrice": 90,
    "discountPersent": 10,
    "quantity": 10,
    "brand": "OriginalBrand",
    "color": "rojo",
    "size": [{"name": "M", "quantity": 5}],
    "imageUrl": "https://example.com/original.jpg",
    "topLevelCategory": "Ropa",
    "secondLevelCategory": "Camisas",
    "thirdLevelCategory": "Formales"
  }'
```
**Respuesta:** `{"id":6, "title":"Original Title", ...}` (creado correctamente)

**Paso 2: Intentar actualizar TODOS los campos**
```bash
curl -s -X PUT http://localhost:8080/admin/products/6/update \
  -H "Authorization: $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "UPDATED Title",
    "description": "UPDATED Description",
    "price": 200,
    "discountedPrice": 180,
    "discountPersent": 10,
    "quantity": 50,
    "brand": "UPDATED Brand",
    "color": "azul",
    "size": [{"name": "XL", "quantity": 25}],
    "imageUrl": "https://example.com/updated.jpg",
    "topLevelCategory": "Electrónica",
    "secondLevelCategory": "Audio",
    "thirdLevelCategory": "Auriculares"
  }'
```

**Respuesta actual (INCORRECTA):**
```json
{
  "id": 6,
  "title": "Original Title",           // ❌ NO cambió
  "description": "UPDATED Description", // ✅ Cambió
  "price": 100,                         // ❌ NO cambió
  "discountedPrice": 90,               // ❌ NO cambió
  "discountPersent": 10,               // ❌ NO cambió
  "quantity": 50,                       // ✅ Cambió
  "brand": "OriginalBrand",            // ❌ NO cambió
  "color": "rojo",                      // ❌ NO cambió
  "sizes": [{"name":"M","quantity":5}], // ❌ NO cambió
  "imageUrl": "https://example.com/original.jpg", // ❌ NO cambió
  "category": {"name":"Formales",...}   // ❌ NO cambió
}
```

**Respuesta esperada (CORRECTA):**
```json
{
  "id": 6,
  "title": "UPDATED Title",             // ✅
  "description": "UPDATED Description", // ✅
  "price": 200,                         // ✅
  "discountedPrice": 180,               // ✅
  "discountPersent": 10,                // ✅
  "quantity": 50,                       // ✅
  "brand": "UPDATED Brand",             // ✅
  "color": "azul",                      // ✅
  "sizes": [{"name":"XL","quantity":25}], // ✅
  "imageUrl": "https://example.com/updated.jpg", // ✅
  "category": {"name":"Auriculares",...}  // ✅
}
```

**Paso 3: Verificar actualización parcial (solo título)**
```bash
curl -s -X PUT http://localhost:8080/admin/products/6/update \
  -H "Authorization: $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Solo Titulo Cambiado"}'
```
**Respuesta:** `{"title":"Original Title",...}` (el título NO cambió, confirmando el bug)

#### Impacto en el Frontend

El formulario de edición de productos (`ProductForm.tsx`) envía todos los campos al backend, pero solo `description` y `quantity` se actualizan. El usuario ve un mensaje de "Producto actualizado exitosamente" pero al volver a editar el producto, los cambios no están guardados.

#### Campos que el frontend envía al actualizar

El frontend envía un objeto `CreateProductRequest` con estos campos:
```typescript
{
  title: string;
  description: string;
  price: number;
  discountedPrice: number;
  discountPersent: number;   // NOTA: typo "Persent" (debe coincidir con backend)
  quantity: number;
  brand: string;
  color: string;
  size: Size[];              // NOTA: "size" (singular) para crear/actualizar
  imageUrl: string;
  topLevelCategory: string;
  secondLevelCategory: string;
  thirdLevelCategory: string;
}
```

#### Solución sugerida

```java
@PutMapping("/admin/products/{id}/update")
public Product updateProduct(@PathVariable Long id, @RequestBody UpdateProductRequest request) {
    Product existing = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    
    // Actualizar TODOS los campos básicos
    if (request.getTitle() != null) existing.setTitle(request.getTitle());
    if (request.getDescription() != null) existing.setDescription(request.getDescription());
    if (request.getPrice() != null) existing.setPrice(request.getPrice());
    if (request.getDiscountedPrice() != null) existing.setDiscountedPrice(request.getDiscountedPrice());
    if (request.getDiscountPersent() != null) existing.setDiscountPersent(request.getDiscountPersent());
    if (request.getQuantity() != null) existing.setQuantity(request.getQuantity());
    if (request.getBrand() != null) existing.setBrand(request.getBrand());
    if (request.getColor() != null) existing.setColor(request.getColor());
    if (request.getImageUrl() != null) existing.setImageUrl(request.getImageUrl());
    
    // Actualizar tallas (campo "size" en el request)
    if (request.getSize() != null) {
        // Limpiar tallas existentes y crear nuevas
        existing.getSizes().clear();
        for (SizeDTO sizeDto : request.getSize()) {
            Size size = new Size();
            size.setName(sizeDto.getName());
            size.setQuantity(sizeDto.getQuantity());
            size.setProduct(existing);
            existing.getSizes().add(size);
        }
    }
    
    // Actualizar categoría jerárquica
    if (request.getTopLevelCategory() != null && !request.getTopLevelCategory().isEmpty()) {
        Category topLevel = categoryService.findOrCreate(request.getTopLevelCategory(), null, 1);
        Category secondLevel = categoryService.findOrCreate(request.getSecondLevelCategory(), topLevel, 2);
        Category thirdLevel = categoryService.findOrCreate(request.getThirdLevelCategory(), secondLevel, 3);
        existing.setCategory(thirdLevel);
    }
    
    return productRepository.save(existing);
}
```

#### Verificación post-fix

```bash
# Actualizar y verificar que TODOS los campos cambian
curl -s -X PUT http://localhost:8080/admin/products/6/update \
  -H "Authorization: $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Nuevo","description":"Nueva","price":999,"brand":"NuevaMarca","color":"verde"}'

# Verificar que los cambios persisten
curl -s http://localhost:8080/products/6 | python -c "
import json,sys
p = json.load(sys.stdin)
assert p['title'] == 'Nuevo', f'title: {p[\"title\"]}'
assert p['price'] == 999, f'price: {p[\"price\"]}'
assert p['brand'] == 'NuevaMarca', f'brand: {p[\"brand\"]}'
assert p['color'] == 'verde', f'color: {p[\"color\"]}'
print('Todos los campos se actualizaron correctamente')
"
```

---

### BUG B8: `GET /cart/` falla con NullPointerException si el usuario no tiene carrito

**Severidad:** ALTA - Bloquea la carga inicial del carrito
**Afecta:** Context del Carrito (`CartContext.tsx`), Navbar (contador del carrito)

#### Descripción

Cuando un usuario autenticado que nunca ha agregado productos al carrito hace `GET /cart/`, el backend devuelve un error 500 con `NullPointerException` en lugar de devolver un carrito vacío o un objeto con `cartItems: []`.

#### Evidencia

```bash
curl -s "http://localhost:8080/cart/" -H "Authorization: $ADMIN_TOKEN"
```
**Respuesta actual:**
```json
{"error":"Cannot invoke \"com.shopwavefusion.modal.Cart.getCartItems()\" because \"cart\" is null","details":"uri=/cart/","timestamp":"2026-05-28T02:27:32.416898533"}
```

**Respuesta esperada:**
```json
{
  "id": null,
  "totalPrice": 0,
  "totalItem": 0,
  "totalDiscountedPrice": 0,
  "discounte": 0,
  "cartItems": []
}
```

O alternativamente, crear automáticamente un carrito vacío al primer acceso.

#### Impacto en el Frontend

El `CartContext` intenta cargar el carrito al iniciar sesión. Si el usuario nunca ha agregado productos, recibe un error 500 que se muestra como "Error al cargar el carrito". El frontend tiene un timeout de 8 segundos que agrava el problema.

#### Solución sugerida

```java
@GetMapping("/cart")
public Cart getCart(@AuthenticationPrincipal User user) {
    Cart cart = cartService.findByUser(user);
    if (cart == null) {
        // Crear carrito vacío automáticamente
        cart = new Cart();
        cart.setUser(user);
        cart.setCartItems(new ArrayList<>());
        cart.setTotalPrice(0);
        cart.setTotalItem(0);
        cart.setTotalDiscountedPrice(0);
        cart.setDiscounte(0);
        cart = cartRepository.save(cart);
    }
    return cart;
}
```

#### Verificación post-fix

```bash
# Con usuario que nunca tuvo carrito
curl -s "http://localhost:8080/cart/" -H "Authorization: $USER_TOKEN"
# Esperado: {"id":X,"totalPrice":0,"totalItem":0,"cartItems":[]}
```

---

## Mapa Completo de Endpoints Esperados

El frontend consume **28 endpoints** del backend. Aquí está el mapa completo con el estado actual de cada uno:

### Autenticación (3 endpoints)

| Método | Endpoint | Auth | Estado | Bug |
|--------|----------|------|--------|-----|
| `GET` | `/auth/signin` | Basic | ✅ Funciona | - |
| `POST` | `/auth/signup` | No | ✅ Funciona | - |
| `GET` | `/users/profile` | JWT | ✅ Funciona | - |

### Productos (5 endpoints)

| Método | Endpoint | Auth | Estado | Bug |
|--------|----------|------|--------|-----|
| `GET` | `/products` | No | ❌ FALLA | B1, B2 |
| `GET` | `/products/{id}` | No | ⚠️ Parcial | B4 |
| `GET` | `/products/products/search?q={q}` | No | ❌ FALLA | B3 |
| `GET` | `/products/by-category?categoryName={n}&page={p}&pageSize={ps}` | No | ⚠️ Devuelve vacío | - |
| `GET` | `/products/all?pageNumber={n}&pageSize={s}&minDiscount={d}` | No | ❌ FALLA | Requiere `colors` y `sizes` |

### Ratings y Reviews (4 endpoints)

| Método | Endpoint | Auth | Estado | Bug |
|--------|----------|------|--------|-----|
| `GET` | `/ratings/product/{id}` | No* | ❌ FALLA | B5 |
| `POST` | `/ratings/create` | JWT | ❓ No probado | - |
| `GET` | `/reviews/product/{id}` | No* | ❌ FALLA | B6 |
| `POST` | `/reviews/create` | JWT | ❓ No probado | - |

*El frontend espera que sean públicos, pero el backend requiere JWT.

### Carrito (4 endpoints)

| Método | Endpoint | Auth | Estado | Bug |
|--------|----------|------|--------|-----|
| `GET` | `/cart` | JWT | ❌ FALLA | B8 |
| `PUT` | `/cart/add` | JWT | ❓ No probado | - |
| `PUT` | `/cart_items/{id}` | JWT | ❓ No probado | - |
| `DELETE` | `/cart_items/{id}` | JWT | ❓ No probado | - |

### Órdenes (3 endpoints)

| Método | Endpoint | Auth | Estado | Bug |
|--------|----------|------|--------|-----|
| `POST` | `/orders` | JWT | ❓ No probado | - |
| `GET` | `/orders/user` | JWT | ✅ Funciona (vacío) | - |
| `GET` | `/orders/{id}` | JWT | ❓ No probado | - |

### Admin - Productos (4 endpoints)

| Método | Endpoint | Auth | Estado | Bug |
|--------|----------|------|--------|-----|
| `POST` | `/admin/products/` | JWT+ADMIN | ✅ Funciona | - |
| `PUT` | `/admin/products/{id}/update` | JWT+ADMIN | ❌ FALLA | B7 |
| `DELETE` | `/admin/products/{id}/delete` | JWT+ADMIN | ❓ No probado | - |
| `POST` | `/admin/products/creates` | JWT+ADMIN | ❓ No probado | - |

### Admin - Órdenes (5 endpoints)

| Método | Endpoint | Auth | Estado | Bug |
|--------|----------|------|--------|-----|
| `GET` | `/admin/orders/` | JWT+ADMIN | ✅ Funciona (vacío) | - |
| `PUT` | `/admin/orders/{id}/confirmed` | JWT+ADMIN | ❓ No probado | - |
| `PUT` | `/admin/orders/{id}/ship` | JWT+ADMIN | ❓ No probado | - |
| `PUT` | `/admin/orders/{id}/deliver` | JWT+ADMIN | ❓ No probado | - |
| `PUT` | `/admin/orders/{id}/cancel` | JWT+ADMIN | ❓ No probado | - |

---

## Modelos de Datos que Espera el Frontend

### Product (respuesta del backend)

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
      "parentCategory": {
        "id": 1,
        "name": "string",
        "parentCategory": null,
        "level": 1
      },
      "level": 2
    },
    "level": 3
  },
  "createdAt": "2026-05-27T19:22:03.504234946",
  "ratings": [],
  "reviews": []
}
```

**Notas importantes:**
- `discountPersent` (con typo) - el backend DEBE usar este nombre exacto
- `sizes` (plural) en la respuesta, pero `size` (singular) al crear/actualizar
- `category` es recursivo con hasta 3 niveles de `parentCategory`
- `ratings` y `reviews` pueden ser arrays vacíos

### CreateProductRequest (envío al backend)

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

**Notas importantes:**
- `size` (singular) al crear, NO `sizes`
- Las categorías se envían como 3 strings separados (topLevel, secondLevel, thirdLevel)
- El backend debe crear la jerarquía de categorías automáticamente

### User (respuesta del backend)

```json
{
  "id": 1,
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "role": "ROLE_ADMIN",
  "mobile": "string",
  "addresses": [],
  "paymentInformation": [],
  "createdAt": "2026-05-20T01:34:56.601287"
}
```

**Notas importantes:**
- `role` debe ser `"ROLE_USER"` o `"ROLE_ADMIN"` (formato Spring Security)
- El JWT debe contener `authorities` como string (ej: `"ROLE_ADMIN"`)

### JWT Payload esperado

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

**Notas importantes:**
- `username` debe contener el email del usuario
- `authorities` debe ser un string (NO un array) que contenga `ROLE_ADMIN` o `ROLE_USER`
- El frontend verifica admin con `authorities.includes('ROLE_ADMIN')`

### Cart (respuesta del backend)

```json
{
  "id": 1,
  "totalPrice": 0,
  "totalItem": 0,
  "totalDiscountedPrice": 0,
  "discounte": 0,
  "cartItems": []
}
```

**Nota:** `discounte` (con typo) - el backend DEBE usar este nombre exacto.

---

## Checklist de Verificación

### Para la entrega del 60% (30 de Mayo)

- [ ] **B1:** `GET /products` devuelve TODOS los productos (no solo 1)
- [ ] **B2:** Las respuestas JSON no tienen errores concatenados
- [ ] **B3:** `GET /products/products/search?q=test` devuelve resultados correctos
- [ ] **B4:** `GET /products/1` devuelve JSON válido sin `hibernateLazyInitializer`
- [ ] **B5:** `GET /ratings/product/1` funciona SIN token JWT
- [ ] **B6:** `GET /reviews/product/1` funciona SIN token JWT

### Para la entrega del 100% (~2 semanas)

- [ ] **B7:** `PUT /admin/products/{id}/update` actualiza TODOS los campos
- [ ] **B8:** `GET /cart/` devuelve carrito vacío en lugar de error 500

### Verificación rápida con curl

```bash
# 1. Login
TOKEN=$(curl -s -u "admin@example.com:admin" http://localhost:8080/auth/signin -D - -o /dev/null 2>&1 | grep -i "authorization:" | sed 's/.*: //' | tr -d '\r')

# 2. Verificar B1: GET /products devuelve más de 1 producto
PRODUCTS_COUNT=$(curl -s http://localhost:8080/products | python -c "import json,sys; print(len(json.load(sys.stdin)))")
echo "Productos: $PRODUCTS_COUNT (debe ser > 1)"

# 3. Verificar B2: JSON válido
curl -s http://localhost:8080/products | python -c "import json,sys; json.load(sys.stdin); print('B2: OK')"

# 4. Verificar B3: Búsqueda
curl -s "http://localhost:8080/products/products/search?q=PRUEBA" | python -c "import json,sys; json.load(sys.stdin); print('B3: OK')"

# 5. Verificar B4: Detalle producto
curl -s http://localhost:8080/products/1 | python -c "import json,sys; json.load(sys.stdin); print('B4: OK')"

# 6. Verificar B5: Ratings público
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/ratings/product/1)
echo "B5: Status $STATUS (debe ser 200)"

# 7. Verificar B6: Reviews público
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/reviews/product/1)
echo "B6: Status $STATUS (debe ser 200)"

# 8. Verificar B7: Actualización completa
curl -s -X PUT "http://localhost:8080/admin/products/6/update" \
  -H "Authorization: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test B7","price":999}' | python -c "
import json,sys
p = json.load(sys.stdin)
assert p.get('title') == 'Test B7', f'B7 FAIL: title={p.get(\"title\")}'
assert p.get('price') == 999, f'B7 FAIL: price={p.get(\"price\")}'
print('B7: OK')
"

# 9. Verificar B8: Carrito vacío
curl -s "http://localhost:8080/cart/" -H "Authorization: $TOKEN" | python -c "
import json,sys
c = json.load(sys.stdin)
assert 'cartItems' in c, 'B8 FAIL: no cartItems'
print('B8: OK')
"
```

---

## Orden de Corrección Recomendado

Para maximizar el impacto en la entrega del 60%:

1. **Primero:** Corregir serialización de Hibernate (resuelve B1, B2, B3, B4 simultáneamente)
2. **Segundo:** Hacer públicos `/ratings/product/**` y `/reviews/product/**` (resuelve B5, B6)
3. **Tercero:** Corregir `PUT /admin/products/{id}/update` (resuelve B7)
4. **Cuarto:** Corregir `GET /cart/` con carrito null (resuelve B8)

### Tiempo estimado total

| Bug | Tiempo | Entrega |
|-----|--------|---------|
| B1+B2+B3+B4 (serialización Hibernate) | 2-4 horas | 60% |
| B5+B6 (endpoints públicos) | 30 min | 60% |
| B7 (update completo) | 2-3 horas | 100% |
| B8 (carrito vacío) | 30 min | 100% |
| **Total** | **5-8 horas** | |

---

**Última actualización:** 28 de Mayo de 2026, 02:30 UTC
