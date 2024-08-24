package com.wefood.back.farm.repository;

import com.wefood.back.farm.dto.FarmListResponse;
import com.wefood.back.farm.dto.FarmResponse;
import com.wefood.back.farm.entity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * class: FarmRepository.
 *
 * @author JBum
 * @version 2024/08/15
 */
public interface FarmRepository extends JpaRepository<Farm, Long> {

    FarmResponse findByUserId(Long userId);

    Boolean existsByUserId(Long userId);

    @Query("select new com.wefood.back.farm.dto.FarmListResponse(f.id, f.name, i.name) from Farm f inner join FarmImage fi on fi.pk.farmId=f.id inner join Image i on i.id=fi.pk.imageId where fi.sequence=1")
    Page<FarmListResponse> findFarms(Pageable pageable);

    FarmResponse findFarmById(Long id);
}
