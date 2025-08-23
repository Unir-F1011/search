package search.com.search.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacetsResponse {
    private Long totalDocuments;                    // Total de documentos en el índice
    private List<FacetBucket> categories;           // Facetas por categoría
    private List<FacetBucket> manufacturers;        // Facetas por fabricante
    private List<PriceRangeBucket> priceRanges;     // Rangos de precio
    private PriceStatistics priceStatistics;       // Estadísticas de precio
}