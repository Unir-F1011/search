package search.com.search.service;

import java.util.Optional;
import java.util.UUID;

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

    void updateItem(ItemsDto items, UUID itemId);

    void deleteItem(UUID itemId);

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
        if (StringUtils.hasLength(itemDto.getId().toString().trim()) &&
                StringUtils.hasLength(itemDto.getCategory().trim()) &&
                StringUtils.hasLength(itemDto.getColor().trim()) &&
                StringUtils.hasLength(itemDto.getManufacturer().trim()) &&
                StringUtils.hasLength(itemDto.getProduct().trim()) &&
                itemDto.getPrice() != null &&
                itemDto.getTotal() != null) {

            Items item = Items.builder()
                    .category(itemDto.getCategory().trim())
                    .id(itemDto.getId())
                    .color(itemDto.getCategory().trim())
                    .manufacturer(itemDto.getManufacturer().trim())
                    .price(itemDto.getPrice())
                    .total(itemDto.getTotal())
                    .product(itemDto.getProduct().trim())
                    .build();

            try {
                this.repository.save(item);
            } catch (Exception e) {
                log.error("addItem error", e);
                throw new RuntimeException("Internal error");
            }

        } else {
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public void updateItem(ItemsDto itemDto, UUID itemId) {
        if (StringUtils.hasLength(itemId.toString().trim()) && itemDto.getTotal() != null) {
            try {
                Optional<Items> itemCopy = this.repository.findById(itemId.toString());
                if (itemCopy.isEmpty()) {
                    throw new IllegalArgumentException("Bad request");
                }
                
                Items item = Items.builder()
                        .id(itemId)
                        .total(itemCopy.get().getTotal() - itemDto.getTotal())
                        .price(itemCopy.get().getPrice())
                        .category(itemCopy.get().getCategory())
                        .color(itemCopy.get().getColor())
                        .manufacturer(itemCopy.get().getManufacturer())
                        .product(itemCopy.get().getProduct())
                        .build();
                
                this.repository.save(item);

            } catch (Exception e) {
                log.error("updateItem", e);
                throw new RuntimeException("Internal error");
            }

        } else {
            throw new IllegalArgumentException("Bad request");
        }
    }

    @Override
    public void deleteItem(UUID itemId) {
        if (StringUtils.hasLength(itemId.toString().trim())) {
            Items item = Items.builder()
                    .id(itemId)
                    .build();

            try {
                this.repository.delete(item);

            } catch (Exception e) {
                log.error("deleteItem error", e);
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
