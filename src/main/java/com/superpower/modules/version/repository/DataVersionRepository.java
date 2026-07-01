package com.superpower.modules.version.repository;

import com.superpower.modules.version.entity.DataVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DataVersionRepository extends JpaRepository<DataVersion, Long> {
    Optional<DataVersion> findTopByOrderByCreatedAtDesc();

    @Query("SELECT v FROM DataVersion v WHERE v.status = 'released' ORDER BY v.createdAt DESC")
    java.util.List<DataVersion> findAllReleased();

    boolean existsByStatus(String status);

    java.util.List<DataVersion> findByStatus(String status);
}
