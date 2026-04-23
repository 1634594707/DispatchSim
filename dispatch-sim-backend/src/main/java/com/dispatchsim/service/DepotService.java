package com.dispatchsim.service;

import com.dispatchsim.dto.PageResponse;
import com.dispatchsim.dto.depot.DepotDto;
import com.dispatchsim.dto.depot.DepotUpsertRequest;

import java.util.List;

public interface DepotService {

    DepotDto createDepot(DepotUpsertRequest request);

    List<DepotDto> findAllDepots();

    PageResponse<DepotDto> findDepots(int page, int size);

    DepotDto getDepot(Long id);

    DepotDto updateDepot(Long id, DepotUpsertRequest request);

    void deleteDepot(Long id);

    DepotDto findNearestDepot(double x, double y);

    List<DepotDto> importDepots(String csvContent);

    String exportDepots();
}
