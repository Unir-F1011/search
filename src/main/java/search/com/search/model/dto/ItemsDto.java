package search.com.search.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ItemsDto {
    private String id;
    private String product;
    private String color;
    private String category;
    private Integer price;
    private String manufacturer;
    private Integer total;

}
