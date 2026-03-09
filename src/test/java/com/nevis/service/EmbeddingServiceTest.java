package com.nevis.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private final EmbeddingService embeddingService = text -> new float[]{};

    @Test
    void toVectorString_formatsCorrectly() {
        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        String result = embeddingService.toVectorString(embedding);
        assertEquals("[0.1,0.2,0.3]", result);
    }

    @Test
    void toVectorString_emptyArray() {
        float[] embedding = new float[]{};
        String result = embeddingService.toVectorString(embedding);
        assertEquals("[]", result);
    }

    @Test
    void toVectorString_singleElement() {
        float[] embedding = new float[]{0.5f};
        String result = embeddingService.toVectorString(embedding);
        assertEquals("[0.5]", result);
    }
}
