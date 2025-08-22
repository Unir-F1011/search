## Troubleshooting

### Problemas Comunes

#### El índice no se crea
1. Verificar conexión a Elasticsearch
2. Revisar credenciales en variables de entorno
3. Comprobar logs: `docker logs ms-search`

#### Búsquedas básicas no funcionan correctamente
1. Verificar que el mapping sea correcto: `GET /items/_mapping`
2. Confirmar que los datos se están indexando
3. Revisar logs de las consultas

#### Fuzzy Search no encuentra resultados esperados
1. **Verificar nivel de fuzziness**: Prueba con diferentes valores (0, 1, 2, AUTO)
2. **Revisar longitud mínima**: Fuzzy requiere al menos 1 carácter exacto
3. **Comprobar el campo de búsqueda**: Asegúrate de que el término esté en algún campo indexado

```bash
# Probar diferentes niveles de fuzziness
curl "localhost:8081/v1/search?q=iphne&fuzziness=0"  # Sin tolerancia
curl "localhost:8081/v1/search?q=iphne&fuzziness=1"  # 1 carácter de diferencia
curl "localhost:8081/v1/search?q=iphne&fuzziness=AUTO"  # Automático
```

#### Autocompletado no genera sugerencias
1. **Verificar campo search_as_you_type**: Confirmar que el campo `product` tenga el tipo correcto
2. **Longitud mínima del prefijo**: Intenta con al menos 2 caracteres
3. **Comprobar límites**: Aumenta el parámetro `limit` para más resultados

```bash
# Verificar mapping del campo product
curl "localhost:9200/items/_mapping" | grep -A5 "product"

# Probar con diferentes longitudes
curl "localhost:8081/v1/suggest?q=i"     # 1 carácter
curl "localhost:8081/v1/suggest?q=iP"    # 2 caracteres
curl "localhost:8081/v1/suggest?q=iPh"   # 3 caracteres
```

#### Búsqueda avanzada devuelve errores 400
1. **Validar formato de precios**: Usar números válidos para minPrice/maxPrice
2. **Verificar parámetros**: Confirmar que los nombres de categoría/fabricante existan
3. **Revisar rangos de precio**: Asegurar que minPrice <= maxPrice

```bash
# Ejemplos de parámetros válidos
curl "localhost:8081/v1/search/advanced?minPrice=100&maxPrice=1000"  # ✅ Correcto
curl "localhost:8081/v1/search/advanced?minPrice=abc"                # ❌ Error: precio inválido
curl "localhost:8081/v1/search/advanced?minPrice=1000&maxPrice=100"  # ❌ Error: rango inválido
```

#### Errores de query en campos keyword
**Error:** `Can only use phrase prefix queries on text fields - not on [category] which is of type [keyword]`

**Solución:** Este error indica uso incorrecto de queries en campos keyword. Los campos keyword requieren:
- `TermQuery` para búsquedas exactas
- `PrefixQuery` para búsquedas por prefijo
- **NO** `PhrasePrefix` o `MultiMatch` de tipo `PHRASE_PREFIX`

#### Performance Issues
1. **Limitar resultados**: Usar paginación apropiada
2. **Optimizar fuzziness**: Valores más bajos (0-1) son más rápidos que AUTO
3. **Filtrar antes de buscar**: Usar filtros estructurados cuando sea posible

```bash
# Búsqueda optimizada: filtros + texto
curl "localhost:8081/v1/search/advanced?category=Electronics&q=phone"  # ✅ Rápido
curl "localhost:8081/v1/search?q=phone"                                # ⚠️ Más lento
```

### Errores de conexión
1. Verificar que Elasticsearch esté ejecutándose
2. Comprobar configuración de red en Docker
3. Validar credenciales de acceso

### Debugging Avanzado

#### Habilitar logging detallado
```properties
# application.properties
logging.level.search.com.search=DEBUG
logging.level.org.elasticsearch=DEBUG
```

#### Consultas directas a Elasticsearch

# Ver todas las queries ejecutadas
docker logs ms-search | grep "curl -iX POST"

# Ejecutar query directamente en Elasticsearch
curl -X POST "localhost:9200/items/_search" \
  -H "Content-Type: application/json" \
  -# Microservicio de Búsqueda - Items

## Descripción
Microservicio encargado de la gestión y búsqueda de items/productos utilizando Elasticsearch como motor de búsqueda.

## Tecnologías
- **Spring Boot** - Framework principal
- **Elasticsearch** - Motor de búsqueda y almacenamiento
- **Docker** - Containerización
- **Lombok** - Reducción de código boilerplate

## Configuración de Elasticsearch

### Mapping de Campos

El microservicio utiliza Elasticsearch con los siguientes tipos de campos optimizados para diferentes tipos de búsqueda:

| Campo | Tipo Elasticsearch | Propósito | Uso en Búsquedas |
|-------|-------------------|-----------|-------------------|
| `id` | `keyword` | Identificador único | Búsquedas exactas por ID |
| `product` | `search_as_you_type` | Nombre del producto | Autocompletado y búsqueda mientras escribes |
| `color` | `text` | Color del producto | Búsqueda de texto completo |
| `category` | `keyword` | Categoría del producto | Filtros exactos |
| `manufacturer` | `keyword` | Fabricante | Filtros exactos |
| `price` | `double` | Precio | Rangos numéricos, ordenamiento |
| `total` | `integer` | Stock/inventario | Cantidades enteras |

### Tipos de Búsqueda Soportados

#### 1. **Search As You Type** - Campo `product`
```java
@Field(type = FieldType.Search_As_You_Type, name = "product")
```
- **Propósito**: Autocompletado y búsqueda mientras el usuario escribe
- **Características**:
    - Genera automáticamente n-gramas (2gram, 3gram)
    - Soporte para búsqueda por prefijos
    - Ideal para experiencias de búsqueda en tiempo real
- **Ejemplo**: Al escribir "iPh" encuentra "iPhone", "iPad"

#### 2. **Keyword** - Campos `category`, `manufacturer`, `id`
```java
@Field(type = FieldType.Keyword, name = "category")
```
- **Propósito**: Búsquedas exactas y filtros
- **Características**:
    - No analizado (no se divide en tokens)
    - Sensible a mayúsculas y minúsculas
    - Perfecto para filtros y agregaciones
- **Ejemplo**: `category = "Electronics"` (búsqueda exacta)

#### 3. **Text** - Campo `color`
```java
@Field(type = FieldType.Text, name = "color")
```
- **Propósito**: Búsqueda de texto completo
- **Características**:
    - Analizado (se divide en tokens)
    - Búsqueda fuzzy y tolerante a errores
    - Soporte para sinónimos
- **Ejemplo**: "azul" puede encontrar variaciones del color

#### 4. **Numéricos** - Campos `price`, `total`
```java
@Field(type = FieldType.Double, name = "price")
@Field(type = FieldType.Integer, name = "total")
```
- **Propósito**: Búsquedas por rangos y ordenamiento
- **Características**: Soporte para operaciones matemáticas
- **Ejemplo**: `price >= 100 AND price <= 500`

## API Endpoints

### Endpoints Básicos

#### Crear Item
```http
POST /v1/items
Content-Type: application/json

{
  "product": "iPhone 15 Pro",
  "color": "Azul Titanio",
  "category": "Electronics",
  "price": 1199.99,
  "manufacturer": "Apple",
  "total": 50
}
```

#### Buscar Items (Filtros Básicos)
```http
GET /v1/items?category=Electronics&manufacturer=Apple&product=iPhone&page=1
```

**Parámetros de búsqueda:**
- `category` (opcional) - Filtro exacto por categoría
- `manufacturer` (opcional) - Filtro exacto por fabricante
- `product` (opcional) - Búsqueda con autocompletado en nombre
- `page` (opcional) - Número de página (default: 1, tamaño: 10 items)

#### Actualizar Item
```http
PATCH /v1/items/{itemId}
Content-Type: application/json

{
  "product": "iPhone 15 Pro Max",
  "total": 45
}
```

#### Eliminar Item
```http
DELETE /v1/items/{itemId}
```

### Endpoints Avanzados de Búsqueda

#### 🔍 Búsqueda Full-Text con Fuzzy
```http
GET /v1/search?q=iphne&fuzziness=AUTO&page=1
```

**Características:**
- **Multi-match** en múltiples campos (product, color, category, manufacturer)
- **Fuzzy matching** para corrección de errores tipográficos
- **Pesos diferenciados** por relevancia de campo
- **Tolerancia configurable** a errores

**Parámetros:**
- `q` (requerido) - Término de búsqueda
- `fuzziness` (opcional) - Nivel de tolerancia: "AUTO", "0", "1", "2" (default: "AUTO")
- `page` (opcional) - Número de página (default: 1)

**Ejemplos:**
```http
# Búsqueda con error tipográfico
GET /v1/search?q=iphne
→ Encuentra "iPhone 15 Pro" a pesar del error

# Búsqueda en múltiples campos
GET /v1/search?q=apple
→ Busca en producto, fabricante, categoría y color

# Configurar tolerancia a errores
GET /v1/search?q=samsyng&fuzziness=1
→ Encuentra "Samsung" con tolerancia de 1 carácter
```

#### 🎯 Autocompletado / Sugerencias
```http
GET /v1/suggest?q=iP&limit=5
```

**Características:**
- **Search-as-you-type** optimizado para prefijos
- **Sugerencias de múltiples campos** (productos, fabricantes, categorías)
- **Sugerencias únicas** sin duplicados
- **Límite configurable** de resultados

**Parámetros:**
- `q` (requerido) - Prefijo para autocompletar
- `limit` (opcional) - Máximo de sugerencias (default: 5, máx: 20)

**Ejemplos:**
```http
# Autocompletado de productos
GET /v1/suggest?q=iP
→ ["iPhone 15 Pro", "iPad Air"]

# Autocompletado de fabricantes
GET /v1/suggest?q=App
→ ["Apple"]

# Autocompletado de categorías
GET /v1/suggest?q=Elect
→ ["Electronics"]

# Limitar resultados
GET /v1/suggest?q=a&limit=3
→ Máximo 3 sugerencias
```

#### 🎯 Búsqueda Avanzada (Híbrida)
```http
GET /v1/search/advanced?q=phone&category=Electronics&manufacturer=Apple&minPrice=500&maxPrice=2000&page=1
```

**Características:**
- **Combina** búsqueda full-text con filtros estructurados
- **Rangos de precio** con operadores gte/lte
- **Filtros exactos** por categoría y fabricante
- **Búsqueda flexible** opcional por texto

**Parámetros:**
- `q` (opcional) - Término de búsqueda full-text con fuzzy
- `category` (opcional) - Filtro exacto por categoría
- `manufacturer` (opcional) - Filtro exacto por fabricante
- `minPrice` (opcional) - Precio mínimo
- `maxPrice` (opcional) - Precio máximo
- `page` (opcional) - Número de página (default: 1)

**Ejemplos:**
```http
# Solo filtros estructurados
GET /v1/search/advanced?category=Electronics&minPrice=100&maxPrice=1000

# Solo búsqueda de texto
GET /v1/search/advanced?q=smartphone

# Combinación completa
GET /v1/search/advanced?q=apple&category=Electronics&manufacturer=Apple&minPrice=500&maxPrice=2000

# Solo rango de precio
GET /v1/search/advanced?minPrice=1000

# Búsqueda por fabricante específico
GET /v1/search/advanced?manufacturer=Samsung&maxPrice=1000
```

## Ejemplos de Búsqueda

### Búsquedas Básicas (Endpoint Original)

#### Búsqueda Simple
```http
GET /v1/items?product=iPhone
```
Encuentra todos los productos que contengan "iPhone"

#### Búsqueda con Autocompletado
```http
GET /v1/items?product=iP
```
Encuentra "iPhone", "iPad", etc.

#### Filtros Combinados
```http
GET /v1/items?category=Electronics&manufacturer=Apple
```
Encuentra productos de Apple en la categoría Electronics

#### Búsqueda Completa
```http
GET /v1/items?category=Electronics&manufacturer=Apple&product=iPhone&page=1
```

### Búsquedas Avanzadas (Nuevos Endpoints)

#### 🔥 Fuzzy Search - Corrección de Errores Tipográficos
```http
# Error tipográfico en iPhone
GET /v1/search?q=iphne
→ Respuesta: iPhone 15 Pro (corrige automáticamente el error)

# Error tipográfico en Samsung  
GET /v1/search?q=samsyng
→ Respuesta: Samsung Galaxy S24

# Error tipográfico en Nintendo
GET /v1/search?q=nintedo
→ Respuesta: Nintendo Switch OLED

# Búsqueda en colores con error
GET /v1/search?q=azl
→ Respuesta: productos con color "Azul"
```

#### 🎯 Autocompletado Inteligente
```http
# Prefijo de productos Apple
GET /v1/suggest?q=iP
→ Respuesta: ["iPhone 15 Pro", "iPad Air"]

# Prefijo de MacBook
GET /v1/suggest?q=Mac  
→ Respuesta: ["MacBook Pro 14"]

# Prefijo de fabricantes
GET /v1/suggest?q=App
→ Respuesta: ["Apple"]

# Prefijo de categorías
GET /v1/suggest?q=Elect
→ Respuesta: ["Electronics"]

# Múltiples sugerencias
GET /v1/suggest?q=S&limit=5
→ Respuesta: ["Samsung Galaxy S24", "Sony PlayStation 5", "Samsung", "Sony"]
```

#### 🎯 Búsquedas Híbridas Complejas
```http
# Texto + Filtros + Precio
GET /v1/search/advanced?q=gaming&category=Gaming&minPrice=300&maxPrice=600
→ Encuentra consolas de gaming en ese rango de precio

# Solo filtros estructurados
GET /v1/search/advanced?manufacturer=Apple&minPrice=1000
→ Productos Apple de más de $1000

# Búsqueda flexible en múltiples campos
GET /v1/search/advanced?q=pro
→ Encuentra "iPhone 15 Pro", "MacBook Pro 14", etc.

# Rango de precio específico
GET /v1/search/advanced?minPrice=500&maxPrice=1500&category=Electronics
→ Electrónicos en rango de precio medio

# Búsqueda por fabricante con texto
GET /v1/search/advanced?q=smartphone&manufacturer=Samsung
→ Smartphones específicamente de Samsung
```

#### 📊 Comparación de Resultados
```http
# Búsqueda básica (exacta)
GET /v1/items?product=iPhone
→ Solo productos que contengan exactamente "iPhone"

# Búsqueda fuzzy (tolerante)  
GET /v1/search?q=iPhone
→ Productos iPhone + productos similares + tolerancia a errores

# Búsqueda avanzada (combinada)
GET /v1/search/advanced?q=iPhone&manufacturer=Apple&minPrice=500
→ iPhones de Apple sobre $500 + búsqueda fuzzy
```

## Configuración

### Variables de Entorno
```properties
# Elasticsearch
elasticsearch.host=your-elasticsearch-host
elasticsearch.credentials.user=your-username
elasticsearch.credentials.password=your-password

# Logging
logging.level.search.com.search=INFO
```

### Docker Compose
```yaml
ms-search:
  image: your-search-service
  ports:
    - "8081:8081"
  environment:
    - elasticsearch.host=elasticsearch-host
    - elasticsearch.credentials.user=elastic
    - elasticsearch.credentials.password=changeme
```

## Estructura del Proyecto

```
src/
├── main/java/search/com/search/
│   ├── config/
│   │   └── ElasticsearchConfig.java     # Configuración de ES
│   ├── controller/
│   │   └── SearchAPI.java               # Endpoints REST
│   ├── model/
│   │   ├── entities/
│   │   │   └── Items.java               # Entidad con mapping
│   │   ├── dto/
│   │   │   ├── ItemsDto.java           # DTO para requests
│   │   │   └── ResponseItems.java       # DTO para responses
│   │   └── consts/
│   │       └── Consts.java             # Constantes de campos
│   ├── repository/
│   │   └── ItemsRepository.java        # Repositorio de ES
│   └── service/
│       └── InnerSearch.java            # Lógica de negocio
```

## Inicialización Automática

El índice de Elasticsearch se crea automáticamente al iniciar la aplicación:

1. **Verificación**: Se comprueba si el índice 'items' existe
2. **Creación**: Si no existe, se crea automáticamente
3. **Mapping**: Se aplica el mapping basado en las anotaciones de `Items.java`
4. **Logs**: Se registra el proceso en los logs de la aplicación

## Funcionalidades Avanzadas de Búsqueda

### 🔍 Multi-Match con Fuzzy Search
El microservicio implementa búsqueda full-text avanzada que:
- **Busca en múltiples campos simultáneamente** (product, color, category, manufacturer)
- **Aplica pesos diferentes** por relevancia de campo
- **Corrige errores tipográficos automáticamente** usando fuzzy matching
- **Configura el nivel de tolerancia** a errores

### 🎯 Autocompletado Inteligente
Sistema de sugerencias que:
- **Utiliza search_as_you_type** optimizado para prefijos
- **Genera sugerencias de múltiples fuentes** (productos, fabricantes, categorías)
- **Elimina duplicados** y ordena por relevancia
- **Limita resultados** configurablemente

### 🎯 Búsqueda Híbrida
Combina lo mejor de ambos mundos:
- **Búsqueda textual fuzzy** para términos generales
- **Filtros estructurados exactos** para criterios específicos
- **Rangos numéricos** para precios
- **Paginación** en todos los tipos de búsqueda

### 📊 Arquitectura de Consultas

#### Endpoint `/v1/search` (Fuzzy)
```
BoolQuery {
  must: MultiMatchQuery {
    fields: ["product^2.0", "color^1.0", "category^1.5", "manufacturer^1.5"]
    type: BEST_FIELDS
    fuzziness: AUTO|0|1|2
    prefixLength: 1
    maxExpansions: 50
  }
}
```

#### Endpoint `/v1/suggest` (Autocompletado)
```
BoolQuery {
  should: [
    MultiMatchQuery {
      fields: ["product", "product._2gram", "product._3gram", "product.prefix"]
      type: BOOL_PREFIX
    },
    PrefixQuery { field: "manufacturer" },
    PrefixQuery { field: "category" },
    MultiMatchQuery {
      fields: ["color"]
      type: PHRASE_PREFIX
    }
  ]
}
```

#### Endpoint `/v1/search/advanced` (Híbrida)
```
BoolQuery {
  must: MultiMatchQuery { /* si q está presente */ }
  filter: [
    TermQuery { field: "category" },     /* si category está presente */
    TermQuery { field: "manufacturer" }, /* si manufacturer está presente */
    RangeQuery { 
      field: "price" 
      gte: minPrice,
      lte: maxPrice
    }  /* si precios están presentes */
  ]
}
```

### El índice no se crea
1. Verificar conexión a Elasticsearch
2. Revisar credenciales en variables de entorno
3. Comprobar logs: `docker logs ms-search`

### Búsquedas no funcionan correctamente
1. Verificar que el mapping sea correcto: `GET /items/_mapping`
2. Confirmar que los datos se están indexando
3. Revisar logs de las consultas

### Errores de conexión
1. Verificar que Elasticsearch esté ejecutándose
2. Comprobar configuración de red en Docker
3. Validar credenciales de acceso

## Testing

### Postman Collection - Búsquedas Avanzadas

Para facilitar las pruebas de las nuevas funcionalidades, puedes importar esta collection extendida:

#### Configuración de Variables
Configura estas variables en Postman:
- `base_url`: `http://localhost:8081`
- `elasticsearch_url`: `http://localhost:9200`


#### Pruebas Destacadas:

🔥 **Fuzzy Search (Más Emocionante):**
- `GET /v1/search?q=iphne` → Encuentra "iPhone 15 Pro" ¡con error tipográfico!
- `GET /v1/search?q=samsyng` → Encuentra "Samsung Galaxy S24"

🔍 **Autocompletado Inteligente:**
- `GET /v1/suggest?q=iP` → `["iPhone 15 Pro", "iPad Air"]`
- `GET /v1/suggest?q=Mac` → `["MacBook Pro 14"]`

🎯 **Búsqueda Híbrida:**
- `GET /v1/search/advanced?q=phone&category=Electronics&minPrice=500&maxPrice=1500`

### Pruebas de Integración
- Tests automáticos que validan el mapping de Elasticsearch
- Verificación de tipos de campos
- Pruebas de búsqueda y filtrado

## Logs y Monitoreo

Los logs incluyen información detallada sobre:
- Creación y actualización de items
- Consultas de búsqueda realizadas
- Errores de conexión con Elasticsearch
- Métricas de rendimiento

```bash
# Ver logs en tiempo real
docker logs -f ms-search

# Filtrar errores
docker logs ms-search | grep ERROR
```