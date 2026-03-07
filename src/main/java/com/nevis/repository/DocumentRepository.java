package com.nevis.repository;

import com.nevis.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByClientId(UUID clientId);

    @Query(value = """
            SELECT d.*, 1 - (d.content_vector <=> CAST(:queryVectorString AS vector)) AS score
            FROM documents d
            WHERE d.content_vector IS NOT NULL
              AND 1 - (d.content_vector <=> CAST(:queryVectorString AS vector)) >= :minScore
            ORDER BY d.content_vector <=> CAST(:queryVectorString AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findBySemanticSimilarity(@Param("queryVectorString") String queryVectorString,
                                             @Param("minScore") double minScore,
                                             @Param("limit") int limit);
}
