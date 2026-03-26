package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "role")
@Getter
@Setter
public class RoleEntity {
    @Id
    @Column(name = "role_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(unique = true, nullable = false, length = 50)
    private String name;
}
