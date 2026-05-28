# Reporte de Problemas Críticos del Backend - ShopWave

## Fecha: 27 de Mayo de 2026
## Reportado por: Equipo Frontend
## Severidad: CRÍTICA - Bloquea funcionalidad principal

---

## RESUMEN EJECUTIVO

El backend tiene **cuatro problemas críticos** que impiden el funcionamiento correcto del panel administrativo:

1. **GET /products no devuelve todos los productos** - Solo retorna el producto ID 1, ignorando productos creados después
2. **PUT /admin/products/{id}/update no actualiza correctamente** - Solo actualiza `description` y `quantity`, ignorando el resto de campos
3. **Error de serialización de Hibernate** - Las respuestas incluyen errores de `ByteBuddyInterceptor` mezclados con JSON válido
4. **Error de categorías duplicadas** - No se pueden crear productos con categorías vacías debido a duplicados en la base de datos

Estos problemas **bloquean la funcionalidad principal** del panel administrativo y requieren corrección inmediata.

---

## PROBLEMA #1: GET /products no devuelve todos los productos

### Descripción
El endpoint `GET /products` solo devuelve el producto con ID 1, ignorando completamente los productos creados posteriormente (IDs 6, 7, 8, 10, etc.).

### Evidencia

#### 1.1 Crear producto nuevo
```bash
curl -X POST http://localhost:8080/admin/products/ \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Producto Test Creacion",
    "description": "Producto creado para verificar que aparece en la lista",
    "price": 299.99,
    "discountedPrice": 249.99,
    "discountPersent": 17,
    "quantity": 50,
    "brand": "TestBrand",
    "color": "rojo",
    "size": [{"name": "M", "quantity": 25}, {"name": "L", "quantity": 25}],
    "imageUrl": "https://example.com/test-creacion.jpg",
    "topLevelCategory": "Ropa",
    "secondLevelCategory": "Camisas",
    "thirdLevelCategory": "Formales"
  }'
```

**Respuesta (éxito):**
```json
{
  "id": 10,
  "title": "Producto Test Creacion",
  "description": "Producto creado para verificar que aparece en la lista",
  "price": 299,
  "discountedPrice": 249,
  "discountPersent": 17,
  "quantity": 50,
  "brand": "TestBrand",
  "color": "rojo",
  "sizes": [{"name": "M", "quantity": 25}, {"name": "L", "quantity": 25}],
  "imageUrl": "https://example.com/test-creacion.jpg",
  "category": {
    "id": 23,
    "name": "Formales",
    "parentCategory": {
      "id": 13,
      "name": "Camisas",
      "parentCategory": {
        "id": 12,
        "name": "Ropa",
        "level": 1
      },
      "level": 2
    },
    "level": 3
  },
  "createdAt": "2026-05-27T19:22:03.504234946"
}
```

#### 1.2 Verificar que el producto existe (GET por ID)
```bash
curl http://localhost:8080/products/10
```

**Respuesta (éxito):**
```json
{
  "id": 10,
  "title": "Producto Test Creacion",
  "description": "Producto creado para verificar que aparece en la lista",
  "price": 299,
  "discountedPrice": 249,
  "discountPersent": 17,
  "quantity": 50,
  "brand": "TestBrand",
  "color": "rojo",
  "sizes": [{"name": "L", "quantity": 25}, {"name": "M", "quantity": 25}],
  "imageUrl": "https://example.com/test-creacion.jpg",
  "category": {
    "id": 23,
    "name": "Formales",
    "parentCategory": {
      "id": 13,
      "name": "Camisas",
      "parentCategory": {
        "id": 12,
        "name": "Ropa",
        "level": 1
      },
      "level": 2
    },
    "level": 3
  },
  "createdAt": "2026-05-27T19:22:03.504235"
}
```

#### 1.3 Listar todos los productos (FALLA)
```bash
curl http://localhost:8080/products
```

**Respuesta (INCORRECTA - solo devuelve ID 1):**
```json
[
  {
    "id": 1,
    "title": "PRUEBA",
    "description": "prueba de producto",
    "price": 100,
    "discountedPrice": 1,
    "discountPersent": 99,
    "quantity": 10,
    "brand": "nike",
    "color": "blanco",
    "sizes": [],
    "imageUrl": "https://st.perplexity.ai/...",
    "ratings": [...]
  }
]
{"error":"Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]","details":"uri=/products","timestamp":"2026-05-27T19:22:42.606511462"}
```

**Problemas observados:**
1. Solo devuelve el producto ID 1, ignorando IDs 6, 7, 8, 10
2. La respuesta incluye un **error de serialización de Hibernate** mezclado con el JSON válido
3. El error indica un problema con proxies de Hibernate: `ByteBuddyInterceptor`

### Posibles causas

1. **Problema de serialización de Hibernate**: El error `ByteBuddyInterceptor` sugiere que Hibernate está intentando serializar proxies lazy-loaded y Jackson no puede manejarlos correctamente.

2. **Filtro incorrecto en la query**: La query JPA puede tener un filtro WHERE que excluye productos creados después de cierta fecha o con ciertas características.

3. **Cache de primer nivel de Hibernate**: La sesión de Hibernate puede estar cacheando resultados antiguos y no reflejando cambios recientes.

4. **Problema con FetchType.LAZY**: Las relaciones `@OneToMany` o `@ManyToOne` pueden estar configuradas como LAZY y causando problemas de serialización.

### Solución sugerida

#### Opción 1: Corregir serialización de Hibernate (Recomendado)
```java
// En la entidad Product
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    // ... campos
}
```

O en la configuración de Jackson:
```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            builder.modules(new Hibernate5Module());
        };
    }
}
```

#### Opción 2: Usar DTOs en lugar de entidades
```java
@GetMapping("/products")
public List<ProductDTO> getAllProducts() {
    return productService.findAll().stream()
        .map(ProductDTO::fromEntity)
        .collect(Collectors.toList());
}
```

#### Opción 3: Forzar carga eager de relaciones
```java
@Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.sizes")
List<Product> findAll();
```

---

## PROBLEMA #2: PUT /admin/products/{id}/update no actualiza correctamente

### Descripción
El endpoint `PUT /admin/products/{id}/update` solo actualiza los campos `description` y `quantity`, ignorando completamente el resto de campos como `title`, `price`, `brand`, `color`, `sizes`, `category`, `imageUrl`.

### Evidencia

#### 2.1 Intentar actualizar todos los campos
```bash
curl -X PUT http://localhost:8080/admin/products/6/update \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Frontend Format UPDATED",
    "description": "Producto de prueba actualizado",
    "price": 150,
    "discountedPrice": 120,
    "discountPersent": 20,
    "quantity": 20,
    "brand": "TestBrandUpdated",
    "color": "azul",
    "size": [{"name": "S", "quantity": 10}, {"name": "XL", "quantity": 10}],
    "imageUrl": "https://example.com/test-updated.jpg",
    "topLevelCategory": "Electrónica",
    "secondLevelCategory": "Audio",
    "thirdLevelCategory": "Auriculares"
  }'
```

**Respuesta (INCORRECTA - solo actualiza description y quantity):**
```json
{
  "id": 6,
  "title": "Test Frontend Format",  // ❌ NO cambió
  "description": "Producto de prueba actualizado",  // ✅ Cambió
  "price": 100,  // ❌ NO cambió
  "discountedPrice": 90,  // ❌ NO cambió
  "discountPersent": 10,  // ❌ NO cambió
  "quantity": 20,  // ✅ Cambió
  "brand": "TestBrand",  // ❌ NO cambió
  "color": "rojo",  // ❌ NO cambió
  "sizes": [{"name": "M", "quantity": 5}, {"name": "L", "quantity": 5}],  // ❌ NO cambió
  "imageUrl": "https://example.com/test.jpg",  // ❌ NO cambió
  "category": {
    "id": 14,
    "name": "Manga Larga",  // ❌ NO cambió
    "parentCategory": {...}
  }
}
```

### Posibles causas

1. **Método de actualización incompleto**: El servicio puede estar actualizando solo ciertos campos manualmente en lugar de usar `BeanUtils.copyProperties()` o similar.

2. **Validación incorrecta**: Puede haber lógica condicional que solo actualiza campos si cumplen ciertas condiciones.

3. **Problema con el mapeo del DTO**: El DTO de actualización puede no incluir todos los campos o tener nombres incorrectos.

### Solución sugerida

#### Opción 1: Actualización completa con BeanUtils
```java
public Product updateProduct(Long id, UpdateProductRequest request) {
    Product existing = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    
    // Actualizar campos básicos
    existing.setTitle(request.getTitle());
    existing.setDescription(request.getDescription());
    existing.setPrice(request.getPrice());
    existing.setDiscountedPrice(request.getDiscountedPrice());
    existing.setDiscountPersent(request.getDiscountPersent());
    existing.setQuantity(request.getQuantity());
    existing.setBrand(request.getBrand());
    existing.setColor(request.getColor());
    existing.setImageUrl(request.getImageUrl());
    
    // Actualizar tallas
    if (request.getSizes() != null) {
        existing.setSizes(request.getSizes().stream()
            .map(sizeDto -> new Size(sizeDto.getName(), sizeDto.getQuantity()))
            .collect(Collectors.toList()));
    }
    
    // Actualizar categoría
    if (request.getTopLevelCategory() != null) {
        Category category = categoryService.findOrCreateCategoryHierarchy(
            request.getTopLevelCategory(),
            request.getSecondLevelCategory(),
            request.getThirdLevelCategory()
        );
        existing.setCategory(category);
    }
    
    return productRepository.save(existing);
}
```

#### Opción 2: Usar MapStruct para mapeo automático
```java
@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "sizes", ignore = true)
    void updateProductFromDto(UpdateProductRequest dto, @MappingTarget Product entity);
}
```

---

## PROBLEMA #3: Error de serialización de Hibernate en respuestas

### Descripción
Las respuestas del backend incluyen un error de serialización de Hibernate mezclado con el JSON válido, causando respuestas malformadas.

### Evidencia
```json
[{"id":1,...}]{"error":"Type definition error: [simple type, class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor]","details":"uri=/products","timestamp":"2026-05-27T19:22:42.606511462"}
```

### Causa
Hibernate crea proxies para entidades con relaciones lazy-loaded. Cuando Jackson intenta serializar estos proxies, falla porque no puede acceder a las propiedades del proxy `ByteBuddyInterceptor`.

### Solución sugerida

#### Opción 1: Agregar dependencia de Jackson Hibernate Module
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate5</artifactId>
</dependency>
```

```java
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            Hibernate5Module hibernateModule = new Hibernate5Module();
            hibernateModule.configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, false);
            hibernateModule.configure(Hibernate5Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
            builder.modules(hibernateModule);
        };
    }
}
```

#### Opción 2: Ignorar propiedades de Hibernate en entidades
```java
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    // ... campos
}
```

#### Opción 3: Usar DTOs (Recomendado)
Crear DTOs específicos para cada endpoint y mapear entidades a DTOs antes de serializar.

---

## PROBLEMA #4: Error de categorías duplicadas al crear productos

### Descripción
Al intentar crear un producto con campos de categoría vacíos, el backend devuelve un error "query did not return a unique result: 2", indicando que hay múltiples categorías con nombre vacío en la base de datos.

### Evidencia

```bash
curl -X POST http://localhost:8080/admin/products/ \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Producto Simple",
    "description": "Producto sin categoría",
    "price": 50,
    "discountedPrice": 40,
    "discountPersent": 20,
    "quantity": 10,
    "brand": "SimpleBrand",
    "color": "gris",
    "size": [],
    "imageUrl": "https://example.com/simple.jpg",
    "topLevelCategory": "",
    "secondLevelCategory": "",
    "thirdLevelCategory": ""
  }'
```

**Respuesta (error):**
```json
{
  "error": "query did not return a unique result: 2",
  "details": "uri=/admin/products/",
  "timestamp": "2026-05-27T19:23:38.164610184"
}
```

### Causa
El servicio de categorías está buscando una categoría por nombre sin verificar que el nombre no esté vacío, y hay múltiples categorías con nombre vacío en la base de datos (probablemente creadas por intentos anteriores).

### Solución sugerida

#### Opción 1: Validar que el nombre no esté vacío
```java
public Category findOrCreateCategory(String name, Category parent) {
    if (name == null || name.trim().isEmpty()) {
        return null; // O lanzar excepción si la categoría es requerida
    }
    
    return categoryRepository.findByNameAndParent(name, parent)
        .orElseGet(() -> {
            Category newCategory = new Category();
            newCategory.setName(name);
            newCategory.setParentCategory(parent);
            return categoryRepository.save(newCategory);
        });
}
```

#### Opción 2: Agregar constraint UNIQUE en la base de datos
```sql
ALTER TABLE categories ADD CONSTRAINT unique_name_parent UNIQUE (name, parent_category_id);
```

#### Opción 3: Limpiar categorías huérfanas
```sql
DELETE FROM categories WHERE name = '' OR name IS NULL;
```

---

## IMPACTO EN EL FRONTEND

### Funcionalidades bloqueadas

1. **Listado de productos**: El panel administrativo no puede mostrar productos creados recientemente
2. **Edición de productos**: Los cambios realizados no se guardan correctamente (excepto description y quantity)
3. **Búsqueda de productos**: Los endpoints de búsqueda también tienen el mismo problema

### Workarounds implementados en el frontend

El frontend ha implementado los siguientes workarounds temporales:

1. **Cache busting**: Agregado `cache: 'no-store'` en todas las peticiones GET
2. **Auto-refresh**: La página de productos se recarga automáticamente al recibir foco
3. **router.refresh()**: Se llama `router.refresh()` después de crear/actualizar productos

**Sin embargo, estos workarounds NO resuelven el problema raíz**, ya que el backend sigue sin devolver los datos correctos.

---

## PRIORIDAD Y TIEMPO ESTIMADO

### Prioridad: CRÍTICA
Estos problemas bloquean la funcionalidad principal del panel administrativo y deben ser corregidos antes de la entrega.

### Tiempo estimado de corrección
- **Problema #1 (GET /products)**: 2-4 horas
- **Problema #2 (PUT /update)**: 2-3 horas
- **Problema #3 (Serialización Hibernate)**: 1-2 horas
- **Problema #4 (Categorías duplicadas)**: 1-2 horas

**Total estimado: 6-11 horas**

---

## RECOMENDACIONES

1. **Usar DTOs**: Crear DTOs específicos para cada endpoint en lugar de serializar entidades directamente
2. **Agregar tests**: Crear tests de integración que verifiquen que los endpoints devuelven los datos correctos
3. **Revisar configuración de Hibernate**: Verificar que las relaciones lazy-loaded estén configuradas correctamente
4. **Monitorear logs**: Revisar los logs del backend para identificar errores de serialización

---

## CONTACTO

Para preguntas o aclaraciones, contactar al equipo de frontend.

**Última actualización:** 27 de Mayo de 2026, 19:23
