package com.dispatchsim.domain.repository;

import com.dispatchsim.domain.model.ReplayEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ReplayEventLogRepository extends JpaRepository<ReplayEventLog, Long> {

    List<ReplayEventLog> findBySessionIdOrderByEventTimeAsc(String sessionId);

    List<ReplayEventLog> findByEventTimeBetweenOrderByEventTimeAsc(Instant startTime, Instant endTime);
}
