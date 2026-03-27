package com.hackathon.edu.repository;

import com.hackathon.edu.entity.QuizEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<QuizEntity, UUID> {
    @EntityGraph(attributePaths = {"lesson", "quests"})
    Optional<QuizEntity> findWithQuestsByQuizId(UUID quizId);

    @EntityGraph(attributePaths = {"lesson", "quests"})
    Optional<QuizEntity> findWithQuestsByLesson_LessonId(UUID lessonId);

    @EntityGraph(attributePaths = {"lesson", "lesson.task", "quests", "quests.answers"})
    Optional<QuizEntity> findWithFlowByQuizId(UUID quizId);

    @EntityGraph(attributePaths = {"lesson", "lesson.task", "quests", "quests.answers"})
    Optional<QuizEntity> findWithFlowByLesson_LessonId(UUID lessonId);
}
