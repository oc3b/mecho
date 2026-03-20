package com.mecho.infrastructure.persistence.repositories;

import com.mecho.infrastructure.persistence.entities.AlertEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    
    List<AlertEntity> findByStatusOrderBySentAtDesc(AlertEntity.AlertStatus status, Pageable pageable);
    
    List<AlertEntity> findByStatusInOrderBySentAtDesc(List<AlertEntity.AlertStatus> statuses, Pageable pageable);
    
    long countByStatus(AlertEntity.AlertStatus status);
}
