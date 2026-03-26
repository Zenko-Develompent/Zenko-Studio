package com.hackathon.edu.repository;

import com.hackathon.edu.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {
    @Query("select r from RoleEntity r where lower(r.name) = lower(:name)")
    Optional<RoleEntity> findByNameIgnoreCase(@Param("name") String name);
}
