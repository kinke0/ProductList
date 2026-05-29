package com.superpower.modules.data.repository;

import com.superpower.modules.data.entity.DataEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataEntryRepository extends JpaRepository<DataEntry, Long> {

    List<DataEntry> findByVersionId(Long versionId);

    List<DataEntry> findByVersionIdAndParentId(Long versionId, Long parentId);

    List<DataEntry> findByVersionIdAndLevel(Long versionId, Integer level);

    List<DataEntry> findByVersionIdAndParentIdOrderBySortOrder(Long versionId, Long parentId);

    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
           "AND e.level = :level " +
           "AND (:name IS NULL OR e.colProductSystem LIKE %:name% " +
           "     OR e.colBizCategory LIKE %:name% " +
           "     OR e.colBizDomain LIKE %:name%) " +
           "AND (:status IS NULL OR e.colStatus = :status) " +
           "AND (:pm IS NULL OR e.colProductManager LIKE %:pm%) " +
           "AND (:solution IS NULL OR e.colOtherSolutionTag = :solution) " +
           "AND (:versionTag IS NULL OR e.colVersionDivision LIKE %:versionTag%) " +
           "ORDER BY e.sortOrder")
    List<DataEntry> findByVersionIdAndLevelWithFilter(
            @Param("versionId") Long versionId,
            @Param("level") Integer level,
            @Param("name") String name,
            @Param("status") String status,
            @Param("pm") String productManager,
            @Param("solution") String solution,
            @Param("versionTag") String versionTag);

    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
           "AND e.parentId = :parentId " +
           "AND (:name IS NULL OR e.colProductSystem LIKE %:name% " +
           "     OR e.colBizCategory LIKE %:name% " +
           "     OR e.colBizDomain LIKE %:name%) " +
           "AND (:status IS NULL OR e.colStatus = :status) " +
           "AND (:pm IS NULL OR e.colProductManager LIKE %:pm%) " +
           "AND (:solution IS NULL OR e.colOtherSolutionTag = :solution) " +
           "AND (:versionTag IS NULL OR e.colVersionDivision LIKE %:versionTag%) " +
           "ORDER BY e.sortOrder")
    List<DataEntry> findByVersionIdAndParentIdWithFilter(
            @Param("versionId") Long versionId,
            @Param("parentId") Long parentId,
            @Param("name") String name,
            @Param("status") String status,
            @Param("pm") String productManager,
            @Param("solution") String solution,
            @Param("versionTag") String versionTag);

    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
           "AND e.level >= 3 " +
           "AND (:customTabId IS NULL OR e.id IN (SELECT ce.entryId FROM CustomTabEntry ce WHERE ce.customTabId = :customTabId)) " +
           "AND (:name IS NULL OR e.colProductSystem LIKE %:name% " +
           "     OR e.colBizCategory LIKE %:name% " +
           "     OR e.colBizDomain LIKE %:name%) " +
           "AND (:status IS NULL OR e.colStatus = :status) " +
           "AND (:pm IS NULL OR e.colProductManager LIKE %:pm%) " +
           "AND (:solution IS NULL OR e.colOtherSolutionTag = :solution) " +
           "AND (:versionDivision IS NULL OR e.colVersionDivision LIKE %:versionDivision%) " +
           "AND (:bizCategory IS NULL OR e.colBizCategory = :bizCategory) " +
           "AND (:bizDomain IS NULL OR e.colBizDomain = :bizDomain) " +
            "ORDER BY e.level, e.parentId, e.sortOrder")
    List<DataEntry> queryEntries(@Param("versionId") Long versionId,
                                 @Param("customTabId") Long customTabId,
                                 @Param("name") String name,
                                 @Param("status") String status,
                                 @Param("pm") String productManager,
                                 @Param("solution") String solution,
                                 @Param("versionDivision") String versionDivision,
                                 @Param("bizCategory") String bizCategory,
                                 @Param("bizDomain") String bizDomain);

    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
           "AND e.level >= 3 " +
           "ORDER BY e.level, e.parentId, e.sortOrder")
    List<DataEntry> findAllEntries(@Param("versionId") Long versionId);

    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
           "AND e.level >= 3 " +
           "AND e.id IN (SELECT ce.entryId FROM CustomTabEntry ce WHERE ce.customTabId = :customTabId) " +
           "ORDER BY e.level, e.parentId, e.sortOrder")
    List<DataEntry> findEntriesByTab(@Param("versionId") Long versionId,
                                     @Param("customTabId") Long customTabId);

    @Query("SELECT e FROM DataEntry e WHERE e.versionId = :versionId " +
           "AND e.level >= 3 " +
           "AND (:bizCategory IS NULL OR e.colBizCategory = :bizCategory) " +
           "AND (:bizDomain IS NULL OR e.colBizDomain = :bizDomain) " +
           "ORDER BY e.level, e.parentId, e.sortOrder")
    List<DataEntry> findEntriesByDomain(@Param("versionId") Long versionId,
                                        @Param("bizCategory") String bizCategory,
                                        @Param("bizDomain") String bizDomain);

    long countByVersionIdAndLevel(Long versionId, Integer level);

    List<DataEntry> findByVersionIdAndColBizCategoryAndColBizDomainAndColProductSystem(
            Long versionId, String colBizCategory, String colBizDomain, String colProductSystem);

    List<DataEntry> findByVersionIdAndColBizCategoryAndColBizDomain(
            Long versionId, String colBizCategory, String colBizDomain);

    List<DataEntry> findByVersionIdAndColProductSystem(Long versionId, String colProductSystem);
}
