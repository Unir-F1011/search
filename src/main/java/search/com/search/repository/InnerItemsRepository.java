package search.com.search.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import search.com.search.model.entities.Items;

public interface InnerItemsRepository extends ElasticsearchRepository <Items, String> {

    List<Items> findByCategory(String category);

    Optional<Items> findById(String itemId);

    Items save(Items item);

    void delete(Items item);

    void update(Items item);

    List<Items> findAll();

    List<Items> findByManufacturer(String manufacuter);
}
