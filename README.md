# Microservicio de Búsqueda - Items

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

### Crear Item
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

### Buscar Items
```http
GET /v1/items?category=Electronics&manufacturer=Apple&product=iPhone&page=1
```

**Parámetros de búsqueda:**
- `category` (opcional) - Filtro exacto por categoría
- `manufacturer` (opcional) - Filtro exacto por fabricante
- `product` (opcional) - Búsqueda con autocompletado en nombre
- `page` (opcional) - Número de página (default: 1, tamaño: 10 items)

### Actualizar Item
```http
PATCH /v1/items/{itemId}
Content-Type: application/json

{
  "product": "iPhone 15 Pro Max",
  "total": 45
}
```

### Eliminar Item
```http
DELETE /v1/items/{itemId}
```

## Ejemplos de Búsqueda

### Búsqueda Simple
```http
GET /v1/items?product=iPhone
```
Encuentra todos los productos que contengan "iPhone"

### Búsqueda con Autocompletado
```http
GET /v1/items?product=iP
```
Encuentra "iPhone", "iPad", etc.

### Filtros Combinados
```http
GET /v1/items?category=Electronics&manufacturer=Apple
```
Encuentra productos de Apple en la categoría Electronics

### Búsqueda Completa
```http
GET /v1/items?category=Electronics&manufacturer=Apple&product=iPhone&page=1
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

## Troubleshooting

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

### Postman Collection

Para facilitar las pruebas, puedes importar esta collection de Postman que incluye todos los endpoints y casos de prueba:

#### Configuración de Variables
Configura estas variables en Postman:
- `base_url`: `http://localhost:8081`

#### Collection JSON para importar:

<details>
<summary>Click para ver la Postman Collection completa</summary>

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