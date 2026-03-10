package com.nevis.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ApiKeyFilterTest {

    private static final String VALID_KEY = "test-secret-key";
    private ApiKeyFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyFilter(VALID_KEY);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void validKey_allowsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/clients");
        request.addHeader("X-API-KEY", VALID_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void missingKey_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/clients");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString()).contains("Invalid or missing API key");
    }

    @Test
    void wrongKey_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/clients");
        request.addHeader("X-API-KEY", "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or missing API key");
    }

    @Test
    void emptyConfiguredKey_returns401() throws Exception {
        ApiKeyFilter emptyKeyFilter = new ApiKeyFilter("");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/clients");
        request.addHeader("X-API-KEY", "any-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        emptyKeyFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void swaggerUiPath_bypassesFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void apiDocsPath_bypassesFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
