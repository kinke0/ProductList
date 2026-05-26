package com.superpower.modules.customtab.repository;

import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.entity.CustomTabEntryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomTabEntryRepository extends JpaRepository<CustomTabEntry, CustomTabEntryId> {
    List<CustomTabEntry> findByCustomTabId(Long customTabId);
    void deleteByCustomTabId(Long customTabId);

    @Modifying
    @Query("DELETE FROM CustomTabEntry e WHERE e.customTabId = :tabId AND e.entryId = :entryId")
    void deleteByCustomTabIdAndEntryId(@Param("tabId") Long tabId, @Param("entryId") Long entryId);
}
