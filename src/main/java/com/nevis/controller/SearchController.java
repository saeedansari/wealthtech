package com.nevis.controller;

import com.nevis.dto.SearchResponse;
import com.nevis.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/search")
@Tag(name = "Search", description = "Search across clients and documents")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(
            summary = "Search across clients and documents",
            description = "Performs keyword search on clients (name, email, description) " +
                          "and semantic search on documents (content similarity)",
            parameters = @Parameter(
                    name = "q",
                    description = "Search query string",
                    required = true,
                    example = "financial statement"
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Search results",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = SearchResponse.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "clients": [
                                                        {
                                                          "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                          "firstName": "John",
                                                          "lastName": "Doe",
                                                          "email": "john.doe@neviswealth.com",
                                                          "description": "Senior financial advisor specializing in wealth management",
                                                          "socialLinks": ["https://linkedin.com/in/johndoe"],
                                                          "createdAt": "2025-03-15T10:30:00",
                                                          "updatedAt": "2025-03-15T10:30:00"
                                                        }
                                                      ],
                                                      "documents": [
                                                        {
                                                          "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                                                          "clientId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
                                                          "title": "Bank Statement Q1 2025",
                                                          "content": "Quarterly bank statement showing financial transactions, account balance of $250,000 and investment portfolio details.",
                                                          "summary": "A quarterly bank statement for Q1 2025 showing a balance of $250,000 with investment portfolio details.",
                                                          "createdAt": "2025-03-15T10:35:00",
                                                          "distance": 0.15
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Missing or blank query parameter",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2025-03-15T10:30:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Required request parameter 'q' for method parameter type String is not present"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<SearchResponse> search(
            @RequestParam("q") String query) {
        if (StringUtils.isBlank(query)) {
            return ResponseEntity.badRequest().build();
        }
        SearchResponse response = searchService.search(query.trim());
        return ResponseEntity.ok(response);
    }
}
