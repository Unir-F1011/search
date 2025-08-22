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
```bash
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

#### Collection JSON para Búsquedas Avanzadas:

<details>
<summary>Click para ver la Postman Collection de Búsquedas Avanzadas</summary>

```json
{
  "info": {
    "name": "Microservicio Search - Búsquedas Avanzadas",
    "description": "Collection completa para probar fuzzy search, autocompletado y búsquedas híbridas",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8081"
    }
  ],
  "item": [
    {
      "name": "0. Setup - Crear Datos de Prueba",
      "item": [
        {
          "name": "Health Check",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items"
          }
        },
        {
          "name": "Crear iPhone 15 Pro",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"iPhone 15 Pro\",\n  \"color\": \"Azul Titanio\",\n  \"category\": \"Electronics\",\n  \"price\": 1199.99,\n  \"manufacturer\": \"Apple\",\n  \"total\": 50\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        },
        {
          "name": "Crear Samsung Galaxy S24",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"Samsung Galaxy S24\",\n  \"color\": \"Negro\",\n  \"category\": \"Electronics\",\n  \"price\": 899.99,\n  \"manufacturer\": \"Samsung\",\n  \"total\": 30\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        },
        {
          "name": "Crear iPad Air",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"iPad Air\",\n  \"color\": \"Gris Espacial\",\n  \"category\": \"Tablets\",\n  \"price\": 599.99,\n  \"manufacturer\": \"Apple\",\n  \"total\": 25\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        },
        {
          "name": "Crear MacBook Pro 14",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"MacBook Pro 14\",\n  \"color\": \"Plata\",\n  \"category\": \"Computers\",\n  \"price\": 2399.99,\n  \"manufacturer\": \"Apple\",\n  \"total\": 15\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        },
        {
          "name": "Crear Nintendo Switch",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"Nintendo Switch OLED\",\n  \"color\": \"Blanco\",\n  \"category\": \"Gaming\",\n  \"price\": 349.99,\n  \"manufacturer\": \"Nintendo\",\n  \"total\": 20\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        }
      ]
    },
    {
      "name": "1. Búsqueda Full-Text con Fuzzy",
      "item": [
        {
          "name": "🔥 Fuzzy Search - 'iphne' (error tipográfico)",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search?q=iphne"
          }
        },
        {
          "name": "🔥 Fuzzy Search - 'samsyng' (error tipográfico)",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search?q=samsyng"
          }
        },
        {
          "name": "Búsqueda exacta - 'iPhone'",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search?q=iPhone"
          }
        },
        {
          "name": "Búsqueda en Color - 'azul'",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search?q=azul"
          }
        },
        {
          "name": "Configurar Fuzziness - fuzziness=1",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search?q=iphne&fuzziness=1"
          }
        }
      ]
    },
    {
      "name": "2. Autocompletado / Sugerencias",
      "item": [
        {
          "name": "🔍 Autocompletado - 'iP'",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/suggest?q=iP"
          }
        },
        {
          "name": "🔍 Autocompletado - 'Mac'",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/suggest?q=Mac"
          }
        },
        {
          "name": "🔍 Autocompletado - 'App' (fabricante)",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/suggest?q=App"
          }
        },
        {
          "name": "🔍 Autocompletado - 'Elect' (categoría)",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/suggest?q=Elect"
          }
        },
        {
          "name": "Límite de sugerencias - limit=3",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/suggest?q=a&limit=3"
          }
        }
      ]
    },
    {
      "name": "3. Búsqueda Avanzada (Híbrida)",
      "item": [
        {
          "name": "🎯 Híbrida - Texto + Categoría",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search/advanced?q=phone&category=Electronics"
          }
        },
        {
          "name": "🎯 Filtro de Precio - Rango",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search/advanced?minPrice=100&maxPrice=1000"
          }
        },
        {
          "name": "🎯 Búsqueda Completa",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search/advanced?q=Apple&category=Electronics&manufacturer=Apple&minPrice=500&maxPrice=2000"
          }
        },
        {
          "name": "Solo Fabricante - Gaming",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search/advanced?category=Gaming"
          }
        }
      ]
    },
    {
      "name": "4. Casos Edge y Validación",
      "item": [
        {
          "name": "❌ Query vacío - Search",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search?q="
          }
        },
        {
          "name": "❌ Query vacío - Suggest",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/suggest?q="
          }
        },
        {
          "name": "Búsqueda sin resultados",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search?q=inexistentproduct12345"
          }
        },
        {
          "name": "Precio inválido",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/search/advanced?minPrice=invalid"
          }
        }
      ]
    }
  ]
}
```

</details>

#### Pruebas Destacadas:

🔥 **Fuzzy Search (Más Emocionante):**
- `GET /v1/search?q=iphne` → Encuentra "iPhone 15 Pro" ¡con error tipográfico!
- `GET /v1/search?q=samsyng` → Encuentra "Samsung Galaxy S24"

🔍 **Autocompletado Inteligente:**
- `GET /v1/suggest?q=iP` → `["iPhone 15 Pro", "iPad Air"]`
- `GET /v1/suggest?q=Mac` → `["MacBook Pro 14"]`

🎯 **Búsqueda Híbrida:**
- `GET /v1/search/advanced?q=phone&category=Electronics&minPrice=500&maxPrice=1500`

```json
{
  "info": {
    "name": "Microservicio Search - Mapping Tests",
    "description": "Collection completa para probar el mapping de Elasticsearch",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:8081"
    },
    {
      "key": "elasticsearch_url", 
      "value": "http://localhost:9200"
    }
  ],
  "item": [
    {
      "name": "0. Setup y Verificación",
      "item": [
        {
          "name": "Health Check - Servicio",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Service is running', function () {",
                  "    pm.response.to.have.status(200);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "Verificar Mapping de Elasticsearch",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{elasticsearch_url}}/items/_mapping"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Mapping exists', function () {",
                  "    pm.response.to.have.status(200);",
                  "    const mapping = pm.response.json();",
                  "    pm.expect(mapping.items.mappings.properties).to.exist;",
                  "});",
                  "",
                  "pm.test('Field types are correct', function () {",
                  "    const properties = pm.response.json().items.mappings.properties;",
                  "    pm.expect(properties.product.type).to.eql('search_as_you_type');",
                  "    pm.expect(properties.category.type).to.eql('keyword');",
                  "    pm.expect(properties.manufacturer.type).to.eql('keyword');",
                  "    pm.expect(properties.color.type).to.eql('text');",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    },
    {
      "name": "1. Crear Datos de Prueba",
      "item": [
        {
          "name": "Crear iPhone 15 Pro",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"iPhone 15 Pro\",\n  \"color\": \"Azul Titanio\",\n  \"category\": \"Electronics\",\n  \"price\": 1199.99,\n  \"manufacturer\": \"Apple\",\n  \"total\": 50\n}"
            },
            "url": "{{base_url}}/v1/items"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Item created successfully', function () {",
                  "    pm.response.to.have.status(202);",
                  "    const response = pm.response.json();",
                  "    pm.expect(response.message).to.include('successful');",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "Crear Samsung Galaxy S24",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"Samsung Galaxy S24\",\n  \"color\": \"Negro\",\n  \"category\": \"Electronics\",\n  \"price\": 899.99,\n  \"manufacturer\": \"Samsung\",\n  \"total\": 30\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        },
        {
          "name": "Crear iPad Air",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"iPad Air\",\n  \"color\": \"Gris Espacial\",\n  \"category\": \"Tablets\",\n  \"price\": 599.99,\n  \"manufacturer\": \"Apple\",\n  \"total\": 25\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        },
        {
          "name": "Crear MacBook Pro 14",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"product\": \"MacBook Pro 14\",\n  \"color\": \"Plata\",\n  \"category\": \"Computers\",\n  \"price\": 2399.99,\n  \"manufacturer\": \"Apple\",\n  \"total\": 15\n}"
            },
            "url": "{{base_url}}/v1/items"
          }
        }
      ]
    },
    {
      "name": "2. Validar Mapping - Keyword Fields",
      "item": [
        {
          "name": "Filtro exacto por Categoría (keyword)",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/v1/items?category=Electronics",
              "host": ["{{base_url}}"],
              "path": ["v1", "items"],
              "query": [{"key": "category", "value": "Electronics"}]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Keyword filter works', function () {",
                  "    pm.response.to.have.status(200);",
                  "    const items = pm.response.json().items;",
                  "    pm.expect(items.length).to.be.greaterThan(0);",
                  "    items.forEach(item => {",
                  "        pm.expect(item.category).to.eql('Electronics');",
                  "    });",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "Filtro exacto por Fabricante (keyword)",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{base_url}}/v1/items?manufacturer=Apple",
              "query": [{"key": "manufacturer", "value": "Apple"}]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Manufacturer filter works', function () {",
                  "    const items = pm.response.json().items;",
                  "    items.forEach(item => {",
                  "        pm.expect(item.manufacturer).to.eql('Apple');",
                  "    });",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "Case Sensitive Test (keyword)",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items?category=electronics"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Keyword is case sensitive', function () {",
                  "    const items = pm.response.json().items;",
                  "    pm.expect(items.length).to.eql(0);",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    },
    {
      "name": "3. Validar Search As You Type",
      "item": [
        {
          "name": "Búsqueda completa - iPhone",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items?product=iPhone"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Full product search works', function () {",
                  "    const items = pm.response.json().items;",
                  "    pm.expect(items.length).to.be.greaterThan(0);",
                  "    items.forEach(item => {",
                  "        pm.expect(item.product.toLowerCase()).to.include('iphone');",
                  "    });",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "Autocompletado - 'iP'",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items?product=iP"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Autocomplete works with partial text', function () {",
                  "    const items = pm.response.json().items;",
                  "    pm.expect(items.length).to.be.greaterThan(0);",
                  "    // Debe encontrar iPhone e iPad",
                  "    const products = items.map(item => item.product.toLowerCase());",
                  "    const hasIPhone = products.some(p => p.includes('iphone'));",
                  "    const hasIPad = products.some(p => p.includes('ipad'));",
                  "    pm.expect(hasIPhone || hasIPad).to.be.true;",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "Autocompletado - 'Mac'",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items?product=Mac"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('MacBook found with Mac prefix', function () {",
                  "    const items = pm.response.json().items;",
                  "    pm.expect(items.length).to.be.greaterThan(0);",
                  "    const foundMacBook = items.some(item => ",
                  "        item.product.toLowerCase().includes('macbook')",
                  "    );",
                  "    pm.expect(foundMacBook).to.be.true;",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    },
    {
      "name": "4. Validar Text Field",
      "item": [
        {
          "name": "Búsqueda en Color (text field)",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Color field is indexed as text', function () {",
                  "    const items = pm.response.json().items;",
                  "    pm.expect(items.length).to.be.greaterThan(0);",
                  "    const colors = items.map(item => item.color);",
                  "    pm.expect(colors).to.include.oneOf(['Azul Titanio', 'Negro', 'Gris Espacial', 'Plata']);",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    },
    {
      "name": "5. Búsquedas Combinadas",
      "item": [
        {
          "name": "Apple + Electronics",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items?manufacturer=Apple&category=Electronics"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Combined filters work', function () {",
                  "    const items = pm.response.json().items;",
                  "    items.forEach(item => {",
                  "        pm.expect(item.manufacturer).to.eql('Apple');",
                  "        pm.expect(item.category).to.eql('Electronics');",
                  "    });",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "Triple combinada",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items?manufacturer=Apple&category=Electronics&product=iPhone"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Triple filter works', function () {",
                  "    const items = pm.response.json().items;",
                  "    items.forEach(item => {",
                  "        pm.expect(item.manufacturer).to.eql('Apple');",
                  "        pm.expect(item.category).to.eql('Electronics');",
                  "        pm.expect(item.product.toLowerCase()).to.include('iphone');",
                  "    });",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    },
    {
      "name": "6. Paginación",
      "item": [
        {
          "name": "Página 1",
          "request": {
            "method": "GET",
            "header": [],
            "url": "{{base_url}}/v1/items?page=1"
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test('Pagination works', function () {",
                  "    pm.response.to.have.status(200);",
                  "    const items = pm.response.json().items;",
                  "    pm.expect(items.length).to.be.at.most(10);",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    }
  ]
}
```

</details>

#### Cómo usar la Postman Collection:

1. **Importar Collection:**
    - Abre Postman
    - Click "Import"
    - Pega el JSON de arriba
    - Click "Import"

2. **Configurar Variables:**
    - Ve a la Collection → Variables
    - Configura `base_url` = `http://localhost:8081`
    - Configura `elasticsearch_url` = `http://localhost:9200`

3. **Ejecutar Tests en Orden:**
    - **Setup y Verificación**: Verifica que todo esté funcionando
    - **Crear Datos**: Inserta items de prueba
    - **Validar Mapping**: Prueba cada tipo de campo
    - **Búsquedas Combinadas**: Tests avanzados

4. **Ejecutar Collection Completa:**
    - Click en la Collection → "Run"
    - Selecciona todos los requests
    - Click "Run Microservicio Search"

#### Tests Automáticos Incluidos:

- ✅ **Verificación de Mapping**: Confirma tipos de campos en Elasticsearch
- ✅ **Keyword Fields**: Prueba filtros exactos y case sensitivity
- ✅ **Search As You Type**: Valida autocompletado y búsquedas parciales
- ✅ **Text Fields**: Verifica análisis de texto
- ✅ **Búsquedas Combinadas**: Tests de múltiples filtros
- ✅ **Paginación**: Verificación de límites de resultados

### Pruebas Manuales con cURL
```bash
# Verificar mapping
curl -X GET "localhost:9200/items/_mapping?pretty"

# Crear item de prueba
curl -X POST "localhost:8081/v1/items" \
  -H "Content-Type: application/json" \
  -d '{"product":"Test Product","color":"Red","category":"Test","price":1.0,"manufacturer":"Test","total":1}'

# Buscar items
curl -X GET "localhost:8081/v1/items?product=Test"
```

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