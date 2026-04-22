package com.dispatchsim.service;

import com.dispatchsim.dto.simulation.BatchOrderRequest;
import com.dispatchsim.dto.simulation.BatchOrderResponse;

public interface BatchOrderService {

    BatchOrderResponse generate(BatchOrderRequest request);
}
