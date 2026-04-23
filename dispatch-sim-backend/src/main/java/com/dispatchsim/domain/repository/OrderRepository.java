package com.dispatchsim.domain.repository;

import com.dispatchsim.domain.model.Order;
import com.dispatchsim.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByArchivedFalseOrderByCreatedAtDesc();

    List<Order> findByStatusAndArchivedFalseOrderByCreatedAtDesc(OrderStatus status);

    List<Order> findByArchivedFalseAndStatusIn(List<OrderStatus> statuses);

    List<Order> findByArchivedFalseAndStatusInAndCompletedAtBefore(List<OrderStatus> statuses, Instant completedBefore);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    long countByStatus(OrderStatus status);

    List<Order> findByStrategyIsNotNull();

    Optional<Order> findByAssignedVehicleIdAndStatusIn(Long vehicleId, List<OrderStatus> statuses);

    @Query("""
            select o from Order o
            where o.archived = true
              and (:archivedFrom is null or o.archivedAt >= :archivedFrom)
              and (:archivedTo is null or o.archivedAt <= :archivedTo)
              and (:reason is null or lower(o.archivalReason) like lower(concat('%', :reason, '%')))
              and (:orderNo is null or lower(o.orderNo) like lower(concat('%', :orderNo, '%')))
            order by o.archivedAt desc, o.id desc
            """)
    Page<Order> searchArchived(
            @Param("archivedFrom") Instant archivedFrom,
            @Param("archivedTo") Instant archivedTo,
            @Param("reason") String reason,
            @Param("orderNo") String orderNo,
            Pageable pageable);
}
