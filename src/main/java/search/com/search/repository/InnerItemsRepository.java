package search.com.search.repository;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import search.com.search.model.entities.Items;

public interface InnerItemsRepository extends ElasticsearchRepository <Items, String> {

    List<Items> findByCategory(String category);

    List<Items> findByManufacturer(String manufacuter);
    
    Items save(Items item);

    void delete(Items item);

    Items update(Items item);

    List<Items> findAll();

}
