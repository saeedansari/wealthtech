package com.nevis.integration;

import com.nevis.service.EmbeddingService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Fake embedding service for integration tests. Returns deterministic vectors
 * that allow testing the full search pipeline without needing a running Ollama instance.
 *
 * Semantically related terms are given similar vectors so that pgvector cosine
 * similarity queries return meaningful results in tests.
 */
@Service
@Primary
public class TestEmbeddingService implements EmbeddingService {

    private static final int DIMENSIONS = 768;
    private static final Map<String, float[]> KNOWN_VECTORS = new HashMap<>();

    static {
        // Create similar vectors for semantically related terms.
        // "address proof" and "utility bill" share a base vector with minor perturbations
        // so cosine similarity is high between them.
        float[] addressProofBase = seededVector(42);
        KNOWN_VECTORS.put("address proof", addressProofBase);
        KNOWN_VECTORS.put("utility bill", perturbVector(addressProofBase, 0.05f));
        KNOWN_VECTORS.put("proof of residence", perturbVector(addressProofBase, 0.08f));

        // Financial terms cluster
        float[] financialBase = seededVector(99);
        KNOWN_VECTORS.put("financial statement", financialBase);
        KNOWN_VECTORS.put("bank statement", perturbVector(financialBase, 0.05f));
        KNOWN_VECTORS.put("income report", perturbVector(financialBase, 0.08f));

        // Unrelated term — distant vector
        KNOWN_VECTORS.put("random unrelated", seededVector(200));
    }

    @Override
    public float[] embed(String text) {
        String normalized = text.toLowerCase().trim();

        // Check for known terms (exact or contained)
        for (Map.Entry<String, float[]> entry : KNOWN_VECTORS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue().clone();
            }
        }

        // For unknown text, generate a deterministic vector based on hashCode
        return seededVector(normalized.hashCode());
    }

    private static float[] seededVector(long seed) {
        Random rng = new Random(seed);
        float[] vector = new float[DIMENSIONS];
        float norm = 0;
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] = (float) rng.nextGaussian();
            norm += vector[i] * vector[i];
        }
        // Normalize to unit vector for cosine similarity
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < DIMENSIONS; i++) {
            vector[i] /= norm;
        }
        return vector;
    }

    private static float[] perturbVector(float[] base, float amount) {
        Random rng = new Random(Float.floatToIntBits(amount));
        float[] result = new float[base.length];
        float norm = 0;
        for (int i = 0; i < base.length; i++) {
            result[i] = base[i] + (float) rng.nextGaussian() * amount;
            norm += result[i] * result[i];
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < base.length; i++) {
            result[i] /= norm;
        }
        return result;
    }
}
