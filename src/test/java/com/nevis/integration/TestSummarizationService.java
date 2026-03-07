package com.nevis.integration;

import com.nevis.service.SummarizationService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Fake summarization service for integration tests.
 * Returns a deterministic summary without calling Ollama.
 */
@Service
@Primary
public class TestSummarizationService extends SummarizationService {

    public TestSummarizationService() {
        super("http://localhost:11434", "test-model");
    }

    @Override
    public String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "Empty document.";
        }
        // Return a truncated version as a predictable "summary"
        String truncated = content.length() > 100 ? content.substring(0, 100) + "..." : content;
        return "Summary: " + truncated;
    }
}
