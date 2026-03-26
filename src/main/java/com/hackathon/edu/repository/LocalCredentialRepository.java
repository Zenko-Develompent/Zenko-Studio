package com.hackathon.edu.repository;

import com.hackathon.edu.entity.LocalCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LocalCredentialRepository extends JpaRepository<LocalCredentialEntity, UUID> {
    @Query("select c from LocalCredentialEntity c where lower(c.email) = lower(:email)")
    Optional<LocalCredentialEntity> findByEmailIgnoreCase(@Param("email") String email);

    @Query("select count(c) > 0 from LocalCredentialEntity c where lower(c.email) = lower(:email)")
    boolean existsByEmailIgnoreCase(@Param("email") String email);
}
