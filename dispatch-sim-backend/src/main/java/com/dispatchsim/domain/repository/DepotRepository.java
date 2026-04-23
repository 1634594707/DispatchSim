package com.dispatchsim.domain.repository;

import com.dispatchsim.domain.model.Depot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DepotRepository extends JpaRepository<Depot, Long> {

    Page<Depot> findAllByOrderByIdAsc(Pageable pageable);
}
