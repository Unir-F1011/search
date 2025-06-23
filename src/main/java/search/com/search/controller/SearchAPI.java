package search.com.search.controller;

import java.util.HashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
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
import search.com.search.model.entities.Items;
import search.com.search.service.InnerSearch;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
@Slf4j
public class SearchAPI {

    private final InnerSearch search;


    @PostMapping("/v1/items")
    public ResponseEntity<Object> addItems(@RequestBody ItemsDto item) {

        HashMap<String,String> response = new HashMap<String,String>();
        response.put("message", "Item added successful!");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @DeleteMapping("/v1/items")
    public ResponseEntity<Object> deleteItems(@PathVariable String itemId) {

        HashMap<String, String> response = new HashMap<String, String>();
        response.put("message", "Item deleted successful!");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
    }
    
    @PatchMapping("/v1/items")
    public ResponseEntity<Object> updateItems(@RequestBody ItemsDto item, @PathVariable String itemId) {

        HashMap<String, String> response = new HashMap<String, String>();
        response.put("message", "Item added successful!");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
    }

    @GetMapping("/v1/items")
    public ResponseEntity<Items> getItems(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String manufacturer,
        @RequestParam(required = false) String product,
        @RequestParam(required = false, defaultValue = "0") String page
    ) {

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
