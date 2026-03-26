package com.hackathon.edu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "module")
@Getter
@Setter
public class CourseLessonEntity {
    @Id
    @Column(name = "course_lesson_id", nullable = false, updatable = false)
    private UUID moduleId;

    @Column(name = "lesson_id", nullable = false, updatable = false) // связь с уроком 
    private LessonEntity lessonid;
    
    @Column(name = "course_id", nullable = false, updatable = false) // связь с курсом если удалён либо урок либо курс удалить запись 
    private CoursEntity courseid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
