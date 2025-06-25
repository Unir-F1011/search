package search.com.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import search.com.search.model.dto.ItemsDto;
import search.com.search.model.dto.ResponseItems;
import search.com.search.model.entities.Items;
import search.com.search.repository.ItemsRepository;

public interface InnerSearch {
    
    void addItem(ItemsDto items);

    void updateItem(ItemsDto items, String itemId);

    void deleteItem(String itemId);

    ResponseItems getItems(String category, String manufacturer, String product, String page);
}

@Service
@RequiredArgsConstructor
@Slf4j
class Search implements InnerSearch {

    @Autowired
    private ItemsRepository repository;

    @Override
    public void addItem(ItemsDto itemDto) {
        if (StringUtils.hasLength(itemDto.getId().trim()) &&
                StringUtils.hasLength(itemDto.getCategory().trim()) &&
                StringUtils.hasLength(itemDto.getColor().trim()) &&
                StringUtils.hasLength(itemDto.getManufacturer().trim()) &&
                StringUtils.hasLength(itemDto.getProduct().trim()) &&
                itemDto.getPrice() != null &&
                itemDto.getTotal() != null) {

            Items item = Items.builder()
                    .category(itemDto.getCategory().trim())
                    .id(itemDto.getId().trim())
                    .color(itemDto.getCategory().trim())
                    .manufacturer(itemDto.getManufacturer().trim())
                    .price(itemDto.getPrice())
                    .total(itemDto.getTotal())
                    .product(itemDto.getProduct().trim())
                    .build();

            try {
                this.repository.save(item);
            } catch (Exception e) {
                throw new RuntimeException("Internal error");
            }

        } else {
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public void updateItem(ItemsDto itemDto, String itemId) {
        if (StringUtils.hasLength(itemId.trim()) && itemDto.getTotal() != null) {
            Items item = Items.builder()
                    .id(itemId.trim())
                    .total(itemDto.getTotal())
                    .build();

            try {
                this.repository.save(item);

            } catch (Exception e) {
                throw new RuntimeException("Internal error");
            }

        } else {
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public void deleteItem(String itemId) {
        if (StringUtils.hasLength(itemId.trim())) {
            Items item = Items.builder()
                    .id(itemId.trim())
                    .build();

            try {
                this.repository.delete(item);

            } catch (Exception e) {
                throw new RuntimeException("Internal error");
            }

        } else {
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public ResponseItems getItems(
            String category,
            String manufacturer,
            String product,
            String page) {

        return repository.findItems(category, manufacturer, product, page);
    }
}
