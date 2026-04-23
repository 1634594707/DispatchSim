package com.dispatchsim.service;

import com.dispatchsim.dto.PageResponse;
import com.dispatchsim.dto.order.ArchiveOrderRequest;
import com.dispatchsim.dto.order.OrderDto;

import java.time.Instant;
import java.util.List;

public interface ArchiveService {

    OrderDto archiveOrder(Long orderId, ArchiveOrderRequest request);

    OrderDto restoreOrder(Long orderId);

    PageResponse<OrderDto> listArchivedOrders(Instant archivedFrom, Instant archivedTo, String reason, String orderNo, int page, int size);

    void autoArchiveCompletedOrders();
}
