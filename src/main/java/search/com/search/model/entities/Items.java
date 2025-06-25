package search.com.search.model.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import search.com.search.model.consts.Consts;

@Document(indexName = "items", createIndex = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Items {

    @Id
    private String id;

    @Field(type = FieldType.Search_As_You_Type, name = Consts.PRODUCT)
    private String product;

    @Field(type = FieldType.Text, name = Consts.COLOR)
    private String color;

    @Field(type = FieldType.Keyword, name = Consts.CATEGORY)
    private String category;

    @Field(type = FieldType.Double, name = Consts.PRICE)
    private Integer price;

    @Field(type = FieldType.Keyword, name = Consts.MANUFACTURER)
    private String manufacturer;

    @Field(type = FieldType.Double, name = Consts.TOTAL)
    private Integer total;
}
