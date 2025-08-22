package search.com.search.service;

import java.util.*;
import java.util.stream.Collectors;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import search.com.search.model.consts.Consts;
import search.com.search.model.dto.ItemsDto;
import search.com.search.model.dto.ResponseItems;
import search.com.search.model.entities.Items;
import search.com.search.repository.ItemsRepository;

public interface InnerSearch {

    void addItem(ItemsDto items);

    void updateItem(ItemsDto items, String itemId);

    void deleteItem(String itemId);

    ResponseItems getItems(String category, String manufacturer, String product, String page);

    ResponseItems searchFullText(String query, String fuzziness, String page);

    List<String> getAutocompleteSuggestions(String prefix, int limit);

    ResponseItems advancedSearch(String query, String category, String manufacturer,
                                 String minPrice, String maxPrice, String page);
}

@Service
@RequiredArgsConstructor
@Slf4j
class Search implements InnerSearch {

    @Autowired
    private ItemsRepository repository;

    @Autowired
    private ElasticsearchOperations elasticClient;

    @Override
    public void addItem(ItemsDto itemDto) {
        // QUITAMOS LA VALIDACIÓN DEL ID - esa línea causaba el NullPointerException
        if (StringUtils.hasLength(itemDto.getCategory().trim()) &&
                StringUtils.hasLength(itemDto.getColor().trim()) &&
                StringUtils.hasLength(itemDto.getManufacturer().trim()) &&
                StringUtils.hasLength(itemDto.getProduct().trim()) &&
                itemDto.getPrice() != null &&
                itemDto.getTotal() != null) {

            // GENERAR ID AUTOMÁTICAMENTE
            String generatedId = UUID.randomUUID().toString();

            Items item = Items.builder()
                    .id(generatedId)  // ← Usar ID generado
                    .category(itemDto.getCategory().trim())
                    .color(itemDto.getColor().trim())
                    .manufacturer(itemDto.getManufacturer().trim())
                    .price(itemDto.getPrice())
                    .total(itemDto.getTotal())
                    .product(itemDto.getProduct().trim())
                    .build();

            try {
                log.info("Creating new item with ID: {}", generatedId);
                this.repository.save(item);
                log.info("Item created successfully");
            } catch (Exception e) {
                log.error("addItem error", e);
                throw new RuntimeException("Internal error");
            }

        } else {
            log.warn("Invalid item data provided");
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public void updateItem(ItemsDto itemDto, String itemId) {
        if (StringUtils.hasLength(itemId.toString().trim()) && itemDto.getTotal() != null) {
            try {
                Optional<Items> itemCopy = this.repository.findById(itemId.toString());
                if (itemCopy.isEmpty()) {
                    throw new IllegalArgumentException("Bad request");
                }

                Items item = Items.builder()
                        .id(itemId.trim())
                        .total(itemCopy.get().getTotal() - itemDto.getTotal())
                        .price(itemCopy.get().getPrice())
                        .category(itemCopy.get().getCategory())
                        .color(itemCopy.get().getColor())
                        .manufacturer(itemCopy.get().getManufacturer())
                        .product(itemCopy.get().getProduct())
                        .build();

                this.repository.save(item);

            } catch (Exception e) {
                log.error("updateItem", e);
                throw new RuntimeException("Internal error");
            }

        } else {
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public void deleteItem(String itemId) {
        if (StringUtils.hasLength(itemId.toString().trim())) {
            Items item = Items.builder()
                    .id(itemId.trim())
                    .build();

            try {
                this.repository.delete(item);

            } catch (Exception e) {
                log.error("deleteItem error", e);
                throw new RuntimeException("Internal error");
            }

        } else {
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public ResponseItems getItems(
            String category,
            String manufacturer,
            String product,
            String page) {

        return repository.findItems(category, manufacturer, product, page);
    }

    @Override
    public ResponseItems searchFullText(String query, String fuzziness, String page) {
        try {
            log.info("Executing full-text search: query='{}', fuzziness='{}', page='{}'", query, fuzziness, page);

            if (StringUtils.isEmpty(query)) {
                throw new IllegalArgumentException("Query cannot be empty");
            }

            // Construir consulta multi_match con fuzzy
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // Multi-match query que busca en múltiples campos con fuzzy
            MultiMatchQueryBuilder multiMatchQuery = QueryBuilders.multiMatchQuery(query)
                    .field(Consts.PRODUCT, 2.0f)        // Mayor peso al campo product
                    .field(Consts.COLOR, 1.0f)          // Peso normal al color
                    .field(Consts.CATEGORY, 1.5f)       // Peso medio a la categoría
                    .field(Consts.MANUFACTURER, 1.5f)   // Peso medio al fabricante
                    .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                    .fuzziness(fuzziness)  // Tolerancia a errores tipográficos
                    .prefixLength(1)       // Mínimo 1 carácter exacto antes de aplicar fuzzy
                    .maxExpansions(50);    // Máximo 50 términos expandidos

            boolQuery.must(multiMatchQuery);

            // Configurar paginación
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery);

            int pageInt = Integer.parseInt(page);
            if (pageInt > 0) {
                queryBuilder.withPageable(PageRequest.of(pageInt - 1, 10));
            }

            // Ejecutar búsqueda
            SearchHits<Items> result = elasticClient.search(queryBuilder.build(), Items.class);

            List<Items> items = result.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("Full-text search completed: found {} items", items.size());
            return new ResponseItems(items);

        } catch (NumberFormatException e) {
            log.error("Invalid page number: {}", page, e);
            throw new IllegalArgumentException("Invalid page number: " + page);
        } catch (Exception e) {
            log.error("Error during full-text search", e);
            throw new RuntimeException("Full-text search failed", e);
        }
    }

    @Override
    public List<String> getAutocompleteSuggestions(String prefix, int limit) {
        try {
            log.info("Getting autocomplete suggestions: prefix='{}', limit={}", prefix, limit);

            if (StringUtils.isEmpty(prefix)) {
                throw new IllegalArgumentException("Prefix cannot be empty");
            }

            if (limit <= 0 || limit > 20) {
                limit = 5; // Default fallback
            }

            // Construcción de query para autocompletado usando search_as_you_type
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // QUERY 1: Usar bool_prefix para el campo search_as_you_type (product)
            MultiMatchQueryBuilder prefixQuery = QueryBuilders.multiMatchQuery(prefix)
                    .field(Consts.PRODUCT)
                    .field(Consts.PRODUCT + "._2gram")
                    .field(Consts.PRODUCT + "._3gram")
                    .field(Consts.PRODUCT + ".prefix")
                    .type(MultiMatchQueryBuilder.Type.BOOL_PREFIX);

            boolQuery.should(prefixQuery);

            // QUERY 2: Para campos keyword (manufacturer y category), usar prefix query
            // En lugar de phrase_prefix, usamos prefix query
            if (prefix.length() >= 1) {
                boolQuery.should(QueryBuilders.prefixQuery(Consts.MANUFACTURER, prefix.toLowerCase()));
                boolQuery.should(QueryBuilders.prefixQuery(Consts.CATEGORY, prefix.toLowerCase()));
            }

            // QUERY 3: También buscar en color (que es text field) usando phrase_prefix
            MultiMatchQueryBuilder colorQuery = QueryBuilders.multiMatchQuery(prefix)
                    .field(Consts.COLOR)
                    .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX);

            boolQuery.should(colorQuery);

            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .withPageable(PageRequest.of(0, limit * 2)); // Obtener más resultados para filtrar

            SearchHits<Items> result = elasticClient.search(queryBuilder.build(), Items.class);

            // Extraer sugerencias únicas
            Set<String> suggestions = new LinkedHashSet<>();

            for (SearchHit<Items> hit : result.getSearchHits()) {
                Items item = hit.getContent();

                // Agregar producto si coincide con el prefijo
                if (item.getProduct() != null &&
                        item.getProduct().toLowerCase().startsWith(prefix.toLowerCase())) {
                    suggestions.add(item.getProduct());
                }

                // Agregar fabricante si coincide
                if (item.getManufacturer() != null &&
                        item.getManufacturer().toLowerCase().startsWith(prefix.toLowerCase())) {
                    suggestions.add(item.getManufacturer());
                }

                // Agregar categoría si coincide
                if (item.getCategory() != null &&
                        item.getCategory().toLowerCase().startsWith(prefix.toLowerCase())) {
                    suggestions.add(item.getCategory());
                }

                // Agregar color si coincide
                if (item.getColor() != null &&
                        item.getColor().toLowerCase().contains(prefix.toLowerCase())) {
                    suggestions.add(item.getColor());
                }

                if (suggestions.size() >= limit) {
                    break;
                }
            }

            List<String> result_list = suggestions.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            log.info("Autocomplete completed: found {} suggestions", result_list.size());
            return result_list;

        } catch (Exception e) {
            log.error("Error during autocomplete search", e);
            throw new RuntimeException("Autocomplete search failed", e);
        }
    }

    @Override
    public ResponseItems advancedSearch(String query, String category, String manufacturer,
                                        String minPrice, String maxPrice, String page) {
        try {
            log.info("Executing advanced search: query='{}', category='{}', manufacturer='{}', minPrice='{}', maxPrice='{}', page='{}'",
                    query, category, manufacturer, minPrice, maxPrice, page);

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // Agregar búsqueda full-text si se proporciona query
            if (!StringUtils.isEmpty(query)) {
                MultiMatchQueryBuilder textQuery = QueryBuilders.multiMatchQuery(query)
                        .field(Consts.PRODUCT, 2.0f)
                        .field(Consts.COLOR, 1.0f)
                        .field(Consts.CATEGORY, 1.5f)
                        .field(Consts.MANUFACTURER, 1.5f)
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                        .fuzziness("AUTO");

                boolQuery.must(textQuery);
            }

            // Agregar filtros estructurados
            if (!StringUtils.isEmpty(category)) {
                boolQuery.filter(QueryBuilders.termQuery(Consts.CATEGORY, category));
            }

            if (!StringUtils.isEmpty(manufacturer)) {
                boolQuery.filter(QueryBuilders.termQuery(Consts.MANUFACTURER, manufacturer));
            }

            // Agregar filtros de rango de precio
            if (!StringUtils.isEmpty(minPrice) || !StringUtils.isEmpty(maxPrice)) {
                RangeQueryBuilder priceQuery = QueryBuilders.rangeQuery(Consts.PRICE);

                if (!StringUtils.isEmpty(minPrice)) {
                    try {
                        double min = Double.parseDouble(minPrice);
                        priceQuery.gte(min);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid minPrice format: " + minPrice);
                    }
                }

                if (!StringUtils.isEmpty(maxPrice)) {
                    try {
                        double max = Double.parseDouble(maxPrice);
                        priceQuery.lte(max);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid maxPrice format: " + maxPrice);
                    }
                }

                boolQuery.filter(priceQuery);
            }

            // Si no hay criterios de búsqueda, usar match_all
            if (!boolQuery.hasClauses()) {
                boolQuery.must(QueryBuilders.matchAllQuery());
            }

            // Configurar paginación
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery);

            int pageInt = Integer.parseInt(page);
            if (pageInt > 0) {
                queryBuilder.withPageable(PageRequest.of(pageInt - 1, 10));
            }

            // Ejecutar búsqueda
            SearchHits<Items> result = elasticClient.search(queryBuilder.build(), Items.class);

            List<Items> items = result.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());

            log.info("Advanced search completed: found {} items", items.size());
            return new ResponseItems(items);

        } catch (NumberFormatException e) {
            log.error("Invalid page number: {}", page, e);
            throw new IllegalArgumentException("Invalid page number: " + page);
        } catch (Exception e) {
            log.error("Error during advanced search", e);
            throw new RuntimeException("Advanced search failed", e);
        }
    }

}
