package search.com.search.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PriceRangeBucket {
    private String key;           // Clave del rango (ej: "0-100", "100-500")
    private Double from;          // Precio mínimo del rango
    private Double to;            // Precio máximo del rango
    private Long docCount;        // Número de documentos en este rango
    private Double percentage;    // Porcentaje del total
    private String displayName;   // Nombre para mostrar (ej: "Hasta $100", "$100 - $500")
}