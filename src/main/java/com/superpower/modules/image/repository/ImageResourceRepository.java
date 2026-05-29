package com.superpower.modules.image.repository;

import com.superpower.modules.image.entity.ImageResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageResourceRepository extends JpaRepository<ImageResource, Long> {

    List<ImageResource> findByCategoryAndDomainAndProduct(String category, String domain, String product);

    List<ImageResource> findByCategory(String category);

    List<ImageResource> findByCategoryAndDomain(String category, String domain);

    List<ImageResource> findByVersionId(Long versionId);

    List<ImageResource> findByVersionIdAndCategory(Long versionId, String category);

    List<ImageResource> findByVersionIdAndCategoryAndDomain(Long versionId, String category, String domain);

    List<ImageResource> findByVersionIdAndCategoryAndDomainAndProduct(Long versionId, String category, String domain, String product);

    List<ImageResource> findByCategoryAndDomainAndProductAndStoredName(String category, String domain, String product, String storedName);
}