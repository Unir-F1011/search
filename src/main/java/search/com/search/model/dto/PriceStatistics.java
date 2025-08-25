package search.com.search.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PriceStatistics {
    private Double min;           // Precio mínimo
    private Double max;           // Precio máximo
    private Double avg;           // Precio promedio
    private Long count;           // Número de productos con precio
    private Double sum;           // Suma total de precios
}