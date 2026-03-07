package com.nevis.controller;

import com.nevis.dto.SearchResponse;
import com.nevis.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "Search across clients and documents")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Search across clients and documents",
               description = "Performs keyword search on clients (name, email, description) " +
                             "and semantic search on documents (content similarity)")
    public ResponseEntity<SearchResponse> search(
            @Parameter(description = "Search query string", required = true)
            @RequestParam("q") String query) {
        if (StringUtils.isBlank(query)) {
            return ResponseEntity.badRequest().build();
        }
        SearchResponse response = searchService.search(query.trim());
        return ResponseEntity.ok(response);
    }
}
