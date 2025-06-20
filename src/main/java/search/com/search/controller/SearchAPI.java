package search.com.search.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import search.com.search.service.InnerSearch;

@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
@Slf4j
public class SearchAPI {

    private final InnerSearch search;


    
    
}
