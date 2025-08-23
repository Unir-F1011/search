package search.com.search.service;

import java.util.*;
import java.util.stream.Collectors;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Stats;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.ArrayList;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import search.com.search.model.consts.Consts;
import search.com.search.model.dto.*;
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

    FacetsResponse getFacets(String query, String category, String manufacturer);
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

    @Override
    public FacetsResponse getFacets(String query, String category, String manufacturer) {
        try {
            log.info("Getting facets with filters: query='{}', category='{}', manufacturer='{}'",
                    query, category, manufacturer);

            // Construir query base con filtros opcionales
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

            // Agregar filtro de texto si se proporciona
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

            // Si no hay filtros, usar match_all
            if (!boolQuery.hasClauses()) {
                boolQuery.must(QueryBuilders.matchAllQuery());
            }

            // Construir query con agregaciones
            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .withPageable(PageRequest.of(0, 1)) // No necesitamos documentos, solo agregaciones

                    // Agregación por categorías
                    .addAggregation(AggregationBuilders.terms("categories")
                            .field(Consts.CATEGORY)
                            .size(50) // Máximo 50 categorías
                            .order(BucketOrder.count(false))) // Ordenar por count descendente

                    // Agregación por fabricantes
                    .addAggregation(AggregationBuilders.terms("manufacturers")
                            .field(Consts.MANUFACTURER)
                            .size(50) // Máximo 50 fabricantes
                            .order(BucketOrder.count(false)))

                    // Agregación de rangos de precio
                    .addAggregation(AggregationBuilders.range("price_ranges")
                            .field(Consts.PRICE)
                            .addUnboundedTo("0-50", 50.0)           // Hasta $50
                            .addRange("50-100", 50.0, 100.0)       // $50 - $100
                            .addRange("100-300", 100.0, 300.0)     // $100 - $300
                            .addRange("300-500", 300.0, 500.0)     // $300 - $500
                            .addRange("500-1000", 500.0, 1000.0)   // $500 - $1000
                            .addRange("1000-2000", 1000.0, 2000.0) // $1000 - $2000
                            .addUnboundedFrom("2000+", 2000.0))     // $2000+

                    // Estadísticas de precio
                    .addAggregation(AggregationBuilders.stats("price_stats")
                            .field(Consts.PRICE));

            // Ejecutar query
            SearchHits<Items> searchResult = elasticClient.search(queryBuilder.build(), Items.class);
            Aggregations aggregations = searchResult.getAggregations();

            // Procesar agregaciones
            FacetsResponse response = FacetsResponse.builder()
                    .totalDocuments(searchResult.getTotalHits())
                    .categories(processCategoryAggregation(aggregations))
                    .manufacturers(processManufacturerAggregation(aggregations))
                    .priceRanges(processPriceRangeAggregation(aggregations))
                    .priceStatistics(processPriceStatistics(aggregations))
                    .build();

            log.info("Facets processed successfully: {} categories, {} manufacturers, {} price ranges",
                    response.getCategories().size(),
                    response.getManufacturers().size(),
                    response.getPriceRanges().size());

            return response;

        } catch (Exception e) {
            log.error("Error getting facets", e);
            throw new RuntimeException("Facets operation failed", e);
        }
    }

    private List<FacetBucket> processCategoryAggregation(Aggregations aggregations) {
        List<FacetBucket> buckets = new ArrayList<>();

        Terms categoryTerms = aggregations.get("categories");
        long totalDocs = categoryTerms.getSumOfOtherDocCounts() +
                categoryTerms.getBuckets().stream().mapToLong(Terms.Bucket::getDocCount).sum();

        for (Terms.Bucket bucket : categoryTerms.getBuckets()) {
            double percentage = totalDocs > 0 ? (bucket.getDocCount() * 100.0) / totalDocs : 0.0;

            buckets.add(FacetBucket.builder()
                    .key(bucket.getKeyAsString())
                    .displayName(formatCategoryDisplayName(bucket.getKeyAsString()))
                    .docCount(bucket.getDocCount())
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        return buckets;
    }

    private List<FacetBucket> processManufacturerAggregation(Aggregations aggregations) {
        List<FacetBucket> buckets = new ArrayList<>();

        Terms manufacturerTerms = aggregations.get("manufacturers");
        long totalDocs = manufacturerTerms.getSumOfOtherDocCounts() +
                manufacturerTerms.getBuckets().stream().mapToLong(Terms.Bucket::getDocCount).sum();

        for (Terms.Bucket bucket : manufacturerTerms.getBuckets()) {
            double percentage = totalDocs > 0 ? (bucket.getDocCount() * 100.0) / totalDocs : 0.0;

            buckets.add(FacetBucket.builder()
                    .key(bucket.getKeyAsString())
                    .displayName(bucket.getKeyAsString()) // Los fabricantes se muestran tal como están
                    .docCount(bucket.getDocCount())
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .build());
        }

        return buckets;
    }

    private List<PriceRangeBucket> processPriceRangeAggregation(Aggregations aggregations) {
        List<PriceRangeBucket> buckets = new ArrayList<>();

        Range priceRanges = aggregations.get("price_ranges");
        long totalDocs = priceRanges.getBuckets().stream().mapToLong(Range.Bucket::getDocCount).sum();

        for (Range.Bucket bucket : priceRanges.getBuckets()) {
            if (bucket.getDocCount() > 0) { // Solo incluir rangos que tengan documentos
                double percentage = totalDocs > 0 ? (bucket.getDocCount() * 100.0) / totalDocs : 0.0;

                // CORRECIÓN: Convertir Object a Double de forma segura
                Double fromValue = convertToDouble(bucket.getFrom());
                Double toValue = convertToDouble(bucket.getTo());

                buckets.add(PriceRangeBucket.builder()
                        .key(bucket.getKeyAsString())
                        .from(fromValue)
                        .to(toValue)
                        .docCount(bucket.getDocCount())
                        .percentage(Math.round(percentage * 100.0) / 100.0)
                        .displayName(formatPriceRangeDisplayName(bucket))
                        .build());
            }
        }

        return buckets;
    }

    private PriceStatistics processPriceStatistics(Aggregations aggregations) {
        Stats priceStats = aggregations.get("price_stats");

        // Obtener valores usando métodos que devuelven Double (objeto) en lugar de double (primitivo)
        Double minValue = convertToDouble(priceStats.getMinAsString());
        Double maxValue = convertToDouble(priceStats.getMaxAsString());
        Double avgValue = priceStats.getAvg();
        Double sumValue = convertToDouble(priceStats.getSumAsString());

        // Manejar valores null y calcular promedio redondeado
        Double roundedAvg = null;
        if (avgValue != null && !Double.isNaN(avgValue) && !Double.isInfinite(avgValue)) {
            roundedAvg = Math.round(avgValue * 100.0) / 100.0;
        }

        return PriceStatistics.builder()
                .min(minValue)
                .max(maxValue)
                .avg(roundedAvg)
                .count(priceStats.getCount())
                .sum(sumValue)
                .build();
    }


    private String formatCategoryDisplayName(String category) {
        // Formatear nombres de categorías para mejor visualización
        switch (category.toLowerCase()) {
            case "electronics": return "Electrónicos";
            case "computers": return "Computadoras";
            case "tablets": return "Tabletas";
            case "gaming": return "Gaming";
            default: return category;
        }
    }

    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Double) {
            Double doubleValue = (Double) value;
            // Verificar valores especiales
            if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                return null;
            }
            return doubleValue;
        }

        if (value instanceof Number) {
            double doubleValue = ((Number) value).doubleValue();
            if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                return null;
            }
            return doubleValue;
        }

        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.trim().isEmpty() || "null".equalsIgnoreCase(strValue)) {
                return null;
            }
            try {
                double doubleValue = Double.parseDouble(strValue);
                if (Double.isNaN(doubleValue) || Double.isInfinite(doubleValue)) {
                    return null;
                }
                return doubleValue;
            } catch (NumberFormatException e) {
                log.warn("Cannot convert string to double: {}", value);
                return null;
            }
        }

        log.warn("Cannot convert object to double: {} (type: {})", value, value.getClass().getSimpleName());
        return null;
    }

    private String formatPriceRangeDisplayName(Range.Bucket bucket) {
        String key = bucket.getKeyAsString();

        switch (key) {
            case "0-50": return "Hasta $50";
            case "50-100": return "$50 - $100";
            case "100-300": return "$100 - $300";
            case "300-500": return "$300 - $500";
            case "500-1000": return "$500 - $1,000";
            case "1000-2000": return "$1,000 - $2,000";
            case "2000+": return "Más de $2,000";
            default:
                // Generar nombre dinámico si no está en el switch
                Double from = convertToDouble(bucket.getFrom());
                Double to = convertToDouble(bucket.getTo());

                if (from != null && to != null) {
                    return String.format("$%.0f - $%.0f", from, to);
                } else if (from != null) {
                    return String.format("Más de $%.0f", from);
                } else if (to != null) {
                    return String.format("Hasta $%.0f", to);
                } else {
                    return key;
                }
        }
    }

}
