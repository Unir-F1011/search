package search.com.search.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacetBucket {
    private String key;           // Valor del bucket (ej: "Electronics", "Apple")
    private String displayName;   // Nombre para mostrar (opcional)
    private Long docCount;        // Número de documentos en este bucket
    private Double percentage;    // Porcentaje del total (opcional)
}