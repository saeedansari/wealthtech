package com.nevis.repository;

import com.nevis.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    @Query(value = """
            SELECT c.*
            FROM clients c
            WHERE c.email ILIKE '%' || :query || '%'
               OR c.first_name ILIKE '%' || :query || '%'
               OR c.last_name ILIKE '%' || :query || '%'
               OR c.description ILIKE '%' || :query || '%'
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> searchClients(@Param("query") String query, @Param("limit") int limit);

  Optional<Client> findByEmail(String email);
}
