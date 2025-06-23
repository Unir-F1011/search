package search.com.search.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import search.com.search.model.entities.Items;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponseItems {
    private List<Items> items;
}
