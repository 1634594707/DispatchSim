package com.dispatchsim.service.impl;

import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.dto.PositionDto;
import com.dispatchsim.dto.order.OrderDto;
import com.dispatchsim.dto.order.CreateOrderRequest;
import com.dispatchsim.dto.simulation.BatchOrderGenerationStrategy;
import com.dispatchsim.dto.simulation.BatchOrderRangeDto;
import com.dispatchsim.dto.simulation.BatchOrderRequest;
import com.dispatchsim.dto.simulation.BatchOrderResponse;
import com.dispatchsim.service.BatchOrderService;
import com.dispatchsim.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchOrderServiceImpl implements BatchOrderService {

    private final OrderService orderService;

    @Override
    public BatchOrderResponse generate(BatchOrderRequest request) {
        validateRanges(request.pickupRange(), "pickupRange");
        validateRanges(request.deliveryRange(), "deliveryRange");

        int totalOrders = request.totalOrders();
        int batchSize = request.batchSize();
        int batchesCreated = (int) Math.ceil((double) totalOrders / batchSize);
        List<Long> orderIds = new ArrayList<>(totalOrders);
        List<CreateOrderRequest> batchRequests = new ArrayList<>(batchSize);

        log.info("Generating batch orders: totalOrders={}, batchSize={}, strategy={}",
                totalOrders, batchSize, request.strategy());

        for (int index = 0; index < totalOrders; index++) {
            PositionDto pickup = generatePosition(request.pickupRange(), request.strategy(), index, totalOrders);
            PositionDto delivery = generatePosition(request.deliveryRange(), request.strategy(), index, totalOrders);
            batchRequests.add(new CreateOrderRequest(
                    pickup,
                    delivery,
                    request.priority()
            ));

            boolean shouldFlushBatch = batchRequests.size() == batchSize || index == totalOrders - 1;
            if (!shouldFlushBatch) {
                continue;
            }

            List<OrderDto> createdOrders = orderService.createOrdersBatch(List.copyOf(batchRequests));
            createdOrders.stream()
                    .map(OrderDto::id)
                    .forEach(orderIds::add);
            batchRequests.clear();
        }

        return new BatchOrderResponse(
                totalOrders,
                batchesCreated,
                request.strategy(),
                request.batchIntervalMs(),
                orderIds
        );
    }

    private void validateRanges(BatchOrderRangeDto range, String name) {
        if (range.minX() > range.maxX() || range.minY() > range.maxY()) {
            throw new BusinessException(name + " 范围无效：最小值不能大于最大值");
        }
    }

    private PositionDto generatePosition(BatchOrderRangeDto range, BatchOrderGenerationStrategy strategy, int index, int totalOrders) {
        return switch (strategy) {
            case UNIFORM -> generateUniform(range, index, totalOrders);
            case PEAK -> generatePeak(range);
            case RANDOM -> generateRandom(range);
        };
    }

    private PositionDto generateUniform(BatchOrderRangeDto range, int index, int totalOrders) {
        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(totalOrders)));
        int row = index / columns;
        int col = index % columns;
        int rows = Math.max(1, (int) Math.ceil((double) totalOrders / columns));

        double x = interpolate(range.minX(), range.maxX(), col, Math.max(columns - 1, 1));
        double y = interpolate(range.minY(), range.maxY(), row, Math.max(rows - 1, 1));
        return new PositionDto(x, y);
    }

    private PositionDto generatePeak(BatchOrderRangeDto range) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double centerX = (range.minX() + range.maxX()) / 2.0;
        double centerY = (range.minY() + range.maxY()) / 2.0;
        double radiusX = Math.max((range.maxX() - range.minX()) / 6.0, 1.0);
        double radiusY = Math.max((range.maxY() - range.minY()) / 6.0, 1.0);

        double x = clamp(centerX + random.nextGaussian() * radiusX, range.minX(), range.maxX());
        double y = clamp(centerY + random.nextGaussian() * radiusY, range.minY(), range.maxY());
        return new PositionDto(x, y);
    }

    private PositionDto generateRandom(BatchOrderRangeDto range) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x = random.nextDouble(range.minX(), Math.nextUp(range.maxX()));
        double y = random.nextDouble(range.minY(), Math.nextUp(range.maxY()));
        return new PositionDto(x, y);
    }

    private double interpolate(double min, double max, int index, int denominator) {
        if (denominator == 0) {
            return min;
        }
        return min + ((max - min) * index / denominator);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
