package com.dispatchsim.domain.repository;

import com.dispatchsim.infrastructure.persistence.OutboxEventEntity;
import com.dispatchsim.infrastructure.persistence.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    Optional<OutboxEventEntity> findByEventId(String eventId);
}
