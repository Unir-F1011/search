package search.com.search.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import search.com.search.model.dto.ItemsDto;
import search.com.search.model.dto.ResponseItems;
import search.com.search.repository.ItemsRepository;


public interface InnerSearch {
    void addItem(ItemsDto items);
    void updateItem(ItemsDto items, String itemId);
    void deleteItem(String itemId);
    ResponseItems getItems(
        String category,
        String manufacturer,
        String product,
        String page
    );
}

@Service
@RequiredArgsConstructor
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

    @Override
    public ResponseItems getItems(
        String category,
        String manufacturer,
        String product,
        String page
    ) {

        return repository.findItems(category, manufacturer, product, page);
    }
}
