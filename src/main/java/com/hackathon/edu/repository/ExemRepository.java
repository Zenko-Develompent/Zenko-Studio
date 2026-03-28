package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ExamEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<ExamEntity, UUID> {
    Optional<ExamEntity> findWithRelationsByExamId(UUID examId);

    Optional<ExamEntity> findWithRelationsByModule_ModuleId(UUID moduleId);
}
