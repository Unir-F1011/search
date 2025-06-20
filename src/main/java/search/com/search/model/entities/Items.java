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

    @Field(type = FieldType.Text, name = "product")
    private String product;

    @Field(type = FieldType.Text, name = "color")
    private String color;

    @Field(type = FieldType.Text, name = "category")
    private String category;

    @Field(type = FieldType.Integer, name = "price")
    private Integer price;

    @Field(type = FieldType.Text, name = "manufacturer")
    private String manufacturer;

    @Field(type = FieldType.Integer, name = "total")
    private Integer total;
    
}
