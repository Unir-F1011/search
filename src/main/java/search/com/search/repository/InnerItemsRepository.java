package search.com.search.repository;

import java.util.Optional;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import search.com.search.model.entities.Items;

public interface InnerItemsRepository extends ElasticsearchRepository<Items, String> {

    Items save(Items item);

    void delete(Items item);
    
    Optional<Items> findById(String id);
}
