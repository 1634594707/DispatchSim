package com.dispatchsim.service.impl;

import com.dispatchsim.common.exception.BusinessException;
import com.dispatchsim.common.exception.ResourceNotFoundException;
import com.dispatchsim.common.exception.ValidationException;
import com.dispatchsim.dto.PageResponse;
import com.dispatchsim.domain.model.Depot;
import com.dispatchsim.domain.model.Position;
import com.dispatchsim.domain.repository.DepotRepository;
import com.dispatchsim.dto.PositionDto;
import com.dispatchsim.dto.depot.DepotDto;
import com.dispatchsim.dto.depot.DepotUpsertRequest;
import com.dispatchsim.service.DepotService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepotServiceImpl implements DepotService {

    private final DepotRepository depotRepository;

    @Override
    @Transactional
    public DepotDto createDepot(DepotUpsertRequest request) {
        Depot depot = Depot.builder()
                .name(request.name().trim())
                .position(toPosition(request.position()))
                .icon(request.icon())
                .metadata(request.metadata())
                .createdAt(Instant.now())
                .build();
        return toDto(depotRepository.save(depot));
    }

    @Override
    public List<DepotDto> findAllDepots() {
        return depotRepository.findAll().stream()
                .sorted(Comparator.comparing(Depot::getId))
                .map(this::toDto)
                .toList();
    }

    @Override
    public PageResponse<DepotDto> findDepots(int page, int size) {
        return PageResponse.from(
                depotRepository.findAllByOrderByIdAsc(PageRequest.of(page, size))
                        .map(this::toDto)
        );
    }

    @Override
    public DepotDto getDepot(Long id) {
        return toDto(findDepot(id));
    }

    @Override
    @Transactional
    public DepotDto updateDepot(Long id, DepotUpsertRequest request) {
        Depot depot = findDepot(id);
        depot.setName(request.name().trim());
        depot.setPosition(toPosition(request.position()));
        depot.setIcon(request.icon());
        depot.setMetadata(request.metadata());
        return toDto(depotRepository.save(depot));
    }

    @Override
    @Transactional
    public void deleteDepot(Long id) {
        Depot depot = findDepot(id);
        depotRepository.delete(depot);
    }

    @Override
    public DepotDto findNearestDepot(double x, double y) {
        Position target = Position.builder().x(x).y(y).build();
        return depotRepository.findAll().stream()
                .min(Comparator.comparingDouble(depot -> depot.getPosition().distanceTo(target)))
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public List<DepotDto> importDepots(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new ValidationException("CSV 内容不能为空");
        }

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        List<Depot> depots = new ArrayList<>();
        try (CSVParser parser = format.parse(new StringReader(csvContent))) {
            for (CSVRecord record : parser) {
                String name = requireField(record, "name");
                double x = parseCoordinate(record, "x");
                double y = parseCoordinate(record, "y");
                String icon = nullableField(record, "icon");
                String metadata = nullableField(record, "metadata");

                depots.add(Depot.builder()
                        .name(name)
                        .position(Position.builder().x(x).y(y).build())
                        .icon(icon)
                        .metadata(metadata)
                        .createdAt(Instant.now())
                        .build());
            }
        } catch (IOException exception) {
            throw new ValidationException("CSV 解析失败");
        }

        return depotRepository.saveAll(depots).stream()
                .sorted(Comparator.comparing(Depot::getId))
                .map(this::toDto)
                .toList();
    }

    @Override
    public String exportDepots() {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("id", "name", "x", "y", "icon", "metadata", "createdAt")
                .build();

        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (Depot depot : depotRepository.findAll().stream().sorted(Comparator.comparing(Depot::getId)).toList()) {
                printer.printRecord(
                        depot.getId(),
                        depot.getName(),
                        depot.getPosition().getX(),
                        depot.getPosition().getY(),
                        depot.getIcon(),
                        depot.getMetadata(),
                        depot.getCreatedAt()
                );
            }
            printer.flush();
            return writer.toString();
        } catch (IOException exception) {
            throw new BusinessException("CSV 导出失败");
        }
    }

    private Depot findDepot(Long id) {
        return depotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("配送中心不存在: " + id));
    }

    private DepotDto toDto(Depot depot) {
        return new DepotDto(
                depot.getId(),
                depot.getName(),
                new PositionDto(depot.getPosition().getX(), depot.getPosition().getY()),
                depot.getIcon(),
                depot.getMetadata(),
                depot.getCreatedAt()
        );
    }

    private Position toPosition(PositionDto dto) {
        return Position.builder()
                .x(dto.x())
                .y(dto.y())
                .build();
    }

    private String requireField(CSVRecord record, String field) {
        String value = nullableField(record, field);
        if (value == null || value.isBlank()) {
            throw new ValidationException("CSV 缺少必填字段: " + field);
        }
        return value;
    }

    private String nullableField(CSVRecord record, String field) {
        return record.isMapped(field) ? record.get(field) : null;
    }

    private double parseCoordinate(CSVRecord record, String field) {
        String value = requireField(record, field);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new ValidationException("CSV 坐标格式错误: " + field);
        }
    }
}
