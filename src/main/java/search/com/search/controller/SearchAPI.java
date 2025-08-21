package search.com.search.controller;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import search.com.search.model.dto.ItemsDto;
import search.com.search.service.InnerSearch;
import search.com.search.model.dto.ResponseItems;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SearchAPI {

    private final InnerSearch search;


    @PostMapping("/v1/items")
    public ResponseEntity<Object> addItems(@RequestBody ItemsDto itemDto) {
        log.info("Received POST request to create item: {}", itemDto);
        try {
            log.info("Calling search.addItem...");
            this.search.addItem(itemDto);
            log.info("Item added successfully, creating response");
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "Item added successful!");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (IllegalArgumentException i) {
            log.error("IllegalArgumentException in addItems: {}", i.getMessage(), i);

            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Bad Request");
            errorResponse.put("message", i.getMessage());
            errorResponse.put("status", "400");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            HashMap<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", "500");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/v1/items/{itemId}")
    public ResponseEntity<Object> deleteItems(@PathVariable String itemId) {
        try {
            this.search.deleteItem(itemId);
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "Item deleted successful!");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }catch (IllegalArgumentException i) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
    }
    
    @PatchMapping("/v1/items/{itemId}")
    public ResponseEntity<Object> updateItems(@RequestBody ItemsDto itemDto, @PathVariable String itemId) {
        try {
            this.search.updateItem(itemDto, itemId);
            HashMap<String, String> response = new HashMap<>();
            response.put("message", "Item updated successful!");
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }catch (IllegalArgumentException i) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        
    }

    @GetMapping("/v1/items")
    public ResponseEntity<ResponseItems> getItems(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String manufacturer,
        @RequestParam(required = false) String product,
        @RequestParam(required = false, defaultValue = "1") String page
    ) {

        try {
            ResponseItems response = this.search.getItems(category, manufacturer, product, page);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }catch (IllegalArgumentException i) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
