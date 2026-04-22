package com.dispatchsim.service;

import com.dispatchsim.domain.model.DispatchStrategy;

import java.util.Map;

public interface DispatchEngine {

    void dispatchPendingOrders();

    DispatchStrategy getCurrentStrategy();

    void setCurrentStrategy(DispatchStrategy strategy);

    Map<DispatchStrategy, Long> getStrategyUsageStats();
}
