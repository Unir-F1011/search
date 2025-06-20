package search.com.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import search.com.search.model.dto.ItemsDto;
import search.com.search.repository.ItemsRepository;



public interface InnerSearch {
    void addItem(ItemsDto items);
    void updateItem(ItemsDto items, String itemId);
    void deleteItem(String itemId);
}

@Service
@Slf4j
class Search implements InnerSearch {
    
    @Autowired
    private ItemsRepository repository;


    @Override
    public void addItem(ItemsDto items) {

    }

    @Override
    public void updateItem(ItemsDto items, String itemId) {

    }

    @Override
    public void deleteItem(String itemId){

    }

}
