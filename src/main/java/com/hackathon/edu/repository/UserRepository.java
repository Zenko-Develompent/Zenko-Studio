package com.hackathon.edu.repository;

import com.hackathon.edu.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);

    @Query("select u from UserEntity u where lower(u.username) = lower(:username)")
    Optional<UserEntity> findByUsernameIgnoreCase(@Param("username") String username);

    @Query("select count(u) > 0 from UserEntity u where lower(u.username) = lower(:username)")
    boolean existsByUsernameIgnoreCase(@Param("username") String username);

    @Query("""
            select u from UserEntity u
            where u.userId <> :currentUserId and lower(u.username) like lower(concat('%', :query, '%'))
            order by u.username asc
            """)
    List<UserEntity> searchByUsername(
            @Param("currentUserId") UUID currentUserId,
            @Param("query") String query,
            org.springframework.data.domain.Pageable pageable
    );
}
