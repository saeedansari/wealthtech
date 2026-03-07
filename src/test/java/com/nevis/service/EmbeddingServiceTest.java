package com.nevis.service;

import com.nevis.integration.TestEmbeddingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    private final EmbeddingService embeddingService = new TestEmbeddingService();

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

    @Test
    void embed_returnsDimensionalVector() {
        float[] result = embeddingService.embed("some text");
        assertEquals(768, result.length);
    }

    @Test
    void embed_sameInputProducesSameOutput() {
        float[] first = embeddingService.embed("hello world");
        float[] second = embeddingService.embed("hello world");
        assertArrayEquals(first, second);
    }

    @Test
    void embed_differentInputProducesDifferentOutput() {
        float[] first = embeddingService.embed("hello");
        float[] second = embeddingService.embed("goodbye");
        boolean allEqual = true;
        for (int i = 0; i < first.length; i++) {
            if (first[i] != second[i]) {
                allEqual = false;
                break;
            }
        }
        assertFalse(allEqual);
    }

    @Test
    void embed_relatedTermsProduceSimilarVectors() {
        float[] addressProof = embeddingService.embed("address proof");
        float[] utilityBill = embeddingService.embed("utility bill");
        float[] unrelated = embeddingService.embed("random unrelated");

        double simRelated = cosineSimilarity(addressProof, utilityBill);
        double simUnrelated = cosineSimilarity(addressProof, unrelated);

        // Related terms should have higher similarity than unrelated ones
        assertTrue(simRelated > simUnrelated,
                "Expected similarity(address proof, utility bill)=" + simRelated +
                " > similarity(address proof, random unrelated)=" + simUnrelated);
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
