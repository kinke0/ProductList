package com.superpower.modules.requirement.repository;

import com.superpower.modules.requirement.entity.ReqLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReqLogRepository extends JpaRepository<ReqLog, Long> {

    List<ReqLog> findByReqIdOrderByCreatedAtDesc(Long reqId);
}
