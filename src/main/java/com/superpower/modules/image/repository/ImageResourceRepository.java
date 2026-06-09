package com.superpower.modules.image.repository;

import com.superpower.modules.image.entity.ImageResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageResourceRepository extends JpaRepository<ImageResource, Long> {

    List<ImageResource> findByCategoryAndDomainAndProductOrderByCreatedAtDesc(String category, String domain, String product);

    List<ImageResource> findByCategoryOrderByCreatedAtDesc(String category);

    List<ImageResource> findByCategoryAndDomainOrderByCreatedAtDesc(String category, String domain);

    List<ImageResource> findByVersionIdOrderByCreatedAtDesc(Long versionId);

    List<ImageResource> findByVersionIdAndCategoryOrderByCreatedAtDesc(Long versionId, String category);

    List<ImageResource> findByVersionIdAndCategoryAndDomainOrderByCreatedAtDesc(Long versionId, String category, String domain);

    List<ImageResource> findByVersionIdAndCategoryAndDomainAndProductOrderByCreatedAtDesc(Long versionId, String category, String domain, String product);

    List<ImageResource> findByCategoryAndDomainAndProductAndStoredName(String category, String domain, String product, String storedName);
}
