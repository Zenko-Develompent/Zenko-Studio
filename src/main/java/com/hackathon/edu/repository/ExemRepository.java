package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ExemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExemRepository extends JpaRepository<ExemEntity, UUID> {
    Optional<ExemEntity> findWithRelationsByExemId(UUID examId);

    Optional<ExemEntity> findWithRelationsByModule_ModuleId(UUID moduleId);
}
