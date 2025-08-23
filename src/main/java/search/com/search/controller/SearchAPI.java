package search.com.search.controller;

import java.util.HashMap;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import search.com.search.model.dto.FacetsResponse;
import search.com.search.model.dto.ItemsDto;
import search.com.search.service.InnerSearch;
import search.com.search.model.dto.ResponseItems;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class SearchAPI {

    private final InnerSearch search;


    @PostMapping("/v1/items")
    public ResponseEntity<Object> addItems(@RequestBody ItemsDto itemDto) {
        log.info("Received POST request to create item: {}", itemDto);
        try {
            log.info("Calling search.addItem...");
            this.search.addItem(itemDto);
            log.info("Item added successfully, creating response");
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "Item added successful!");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (IllegalArgumentException i) {
            log.error("IllegalArgumentException in addItems: {}", i.getMessage(), i);

            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", i.getMessage());
            errorResponse.put("status", "400");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/v1/items/{itemId}")
    public ResponseEntity<Object> deleteItems(@PathVariable String itemId) {
        try {
            this.search.deleteItem(itemId);
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "Item deleted successful!");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (IllegalArgumentException i) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

    }

    @PatchMapping("/v1/items/{itemId}")
    public ResponseEntity<Object> updateItems(@RequestBody ItemsDto itemDto, @PathVariable String itemId) {
        try {
            this.search.updateItem(itemDto, itemId);
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "Item updated successful!");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (IllegalArgumentException i) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }


    }

    @GetMapping("/v1/items")
    public ResponseEntity<ResponseItems> getItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String product,
            @RequestParam(required = false, defaultValue = "1") String page
    ) {

        try {
            ResponseItems response = this.search.getItems(category, manufacturer, product, page);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (IllegalArgumentException i) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Búsqueda full-text avanzada con fuzzy matching
     * Busca en múltiples campos usando multi_match con tolerancia a errores tipográficos
     *
     * @param q         Término de búsqueda
     * @param fuzziness Nivel de tolerancia a errores (opcional, default: "AUTO")
     * @param page      Número de página (opcional, default: "1")
     * @return Items que coincidan con la búsqueda
     */

    @GetMapping("/v1/search")
    public ResponseEntity<ResponseItems> searchItems(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "AUTO") String fuzziness,
            @RequestParam(required = false, defaultValue = "1") String page) {

        try {
            log.info("Full-text search request: q='{}', fuzziness='{}', page='{}'", q, fuzziness, page);

            // Validar parámetro de búsqueda
            if (q == null || q.trim().isEmpty()) {
                log.warn("Empty search query provided");
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Bad Request");
                errorResponse.put("message", "Search query 'q' cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            ResponseItems response = this.search.searchFullText(q.trim(), fuzziness, page);
            log.info("Full-text search completed: found {} items", response.getItems().size());

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid search parameters: q='{}', fuzziness='{}', page='{}'", q, fuzziness, page, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Error during full-text search: q='{}', fuzziness='{}', page='{}'", q, fuzziness, page, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint de sugerencias/autocompletado
     * Utiliza el campo search_as_you_type para búsquedas por prefijo
     *
     * @param q     Prefijo o término parcial para autocompletar
     * @param limit Número máximo de sugerencias (opcional, default: 5)
     * @return Lista de sugerencias basadas en el prefijo
     */
    @GetMapping("/v1/suggest")
    public ResponseEntity<Object> getSuggestions(
            @RequestParam String q,
            @RequestParam(required = false, defaultValue = "5") String limit) {

        try {
            log.info("Autocomplete request: q='{}', limit='{}'", q, limit);

            // Validar parámetros
            if (q == null || q.trim().isEmpty()) {
                log.warn("Empty suggestion query provided");
                HashMap<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Bad Request");
                errorResponse.put("message", "Suggestion query 'q' cannot be empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            int limitInt;
            try {
                limitInt = Integer.parseInt(limit);
                if (limitInt <= 0 || limitInt > 20) {
                    limitInt = 5; // Default fallback
                }
            } catch (NumberFormatException e) {
                limitInt = 5;
            }

            List<String> suggestions = this.search.getAutocompleteSuggestions(q.trim(), limitInt);
            log.info("Autocomplete completed: found {} suggestions", suggestions.size());

            HashMap<String, Object> response = new HashMap<>();
            response.put("query", q.trim());
            response.put("suggestions", suggestions);
            response.put("count", suggestions.size());

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid suggestion parameters: q='{}', limit='{}'", q, limit, e);
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Error during suggestion search: q='{}', limit='{}'", q, limit, e);
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "Error processing suggestion request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Búsqueda híbrida que combina filtros estructurados con búsqueda full-text
     *
     * @param q            Término de búsqueda full-text (opcional)
     * @param category     Filtro por categoría (opcional)
     * @param manufacturer Filtro por fabricante (opcional)
     * @param minPrice     Precio mínimo (opcional)
     * @param maxPrice     Precio máximo (opcional)
     * @param page         Número de página (opcional, default: "1")
     * @return Items que coincidan con los criterios combinados
     */
    @GetMapping("/v1/search/advanced")
    public ResponseEntity<ResponseItems> advancedSearch(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String manufacturer,
            @RequestParam(required = false) String minPrice,
            @RequestParam(required = false) String maxPrice,
            @RequestParam(required = false, defaultValue = "1") String page) {

        try {
            log.info("Advanced search request: q='{}', category='{}', manufacturer='{}', minPrice='{}', maxPrice='{}', page='{}'",
                    q, category, manufacturer, minPrice, maxPrice, page);

            ResponseItems response = this.search.advancedSearch(q, category, manufacturer, minPrice, maxPrice, page);
            log.info("Advanced search completed: found {} items", response.getItems().size());

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid advanced search parameters", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Error during advanced search", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint de facetas/agregaciones para filtros dinámicos
     * Devuelve agregaciones por categoría, fabricante y rangos de precio
     *
     * @param q Filtro opcional de texto para generar facetas contextuales
     * @param category Filtro opcional por categoría para facetas cruzadas
     * @param manufacturer Filtro opcional por fabricante para facetas cruzadas
     * @return FacetsResponse con todas las agregaciones y estadísticas
     */
    @GetMapping("/v1/facets")
    public ResponseEntity<FacetsResponse> getFacets(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String manufacturer) {

        try {
            log.info("Facets request: q='{}', category='{}', manufacturer='{}'", q, category, manufacturer);

            FacetsResponse response = this.search.getFacets(q, category, manufacturer);

            log.info("Facets response generated: {} total documents, {} categories, {} manufacturers, {} price ranges",
                    response.getTotalDocuments(),
                    response.getCategories().size(),
                    response.getManufacturers().size(),
                    response.getPriceRanges().size());

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid facets parameters: q='{}', category='{}', manufacturer='{}'", q, category, manufacturer, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            log.error("Error getting facets: q='{}', category='{}', manufacturer='{}'", q, category, manufacturer, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
