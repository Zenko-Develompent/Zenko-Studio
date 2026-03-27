package com.hackathon.edu.service;

import com.hackathon.edu.entity.ActivityEventEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ActivityEventRepository;
import com.hackathon.edu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityEventService {
    public static final String TYPE_QUIZ_COMPLETED = "quiz_completed";
    public static final String TYPE_TASK_COMPLETED = "task_completed";
    public static final String TYPE_EXAM_COMPLETED = "exam_completed";
    public static final String TYPE_LEVEL_UP = "level_up";
    public static final String TYPE_ACHIEVEMENT_UNLOCKED = "achievement_unlocked";
    public static final String TYPE_STREAK_DAY = "streak_day";

    private static final int SCORE_QUIZ_COMPLETED = 10;
    private static final int SCORE_TASK_COMPLETED = 15;
    private static final int SCORE_EXAM_COMPLETED = 40;
    private static final int SCORE_LEVEL_UP = 10;
    private static final int SCORE_ACHIEVEMENT_UNLOCKED = 20;
    private static final int SCORE_STREAK_DAY = 5;

    private final ActivityEventRepository activityEventRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recordQuizCompleted(UUID userId, UUID lessonId, UUID quizId, int xpGranted, int coinGranted) {
        saveEvent(
                userId,
                TYPE_QUIZ_COMPLETED,
                SCORE_QUIZ_COMPLETED,
                xpGranted,
                coinGranted,
                100,
                lessonId,
                quizId,
                null,
                null,
                null
        );
    }

    @Transactional
    public void recordTaskCompleted(
            UUID userId,
            UUID lessonId,
            UUID taskId,
            UUID examId,
            int xpGranted,
            int coinGranted
    ) {
        saveEvent(
                userId,
                TYPE_TASK_COMPLETED,
                SCORE_TASK_COMPLETED,
                xpGranted,
                coinGranted,
                100,
                lessonId,
                null,
                taskId,
                examId,
                null
        );
    }

    @Transactional
    public void recordExamCompleted(UUID userId, UUID examId, int xpGranted, int coinGranted) {
        saveEvent(
                userId,
                TYPE_EXAM_COMPLETED,
                SCORE_EXAM_COMPLETED,
                xpGranted,
                coinGranted,
                100,
                null,
                null,
                null,
                examId,
                null
        );
    }

    @Transactional
    public void recordLevelUp(UUID userId, int levelFrom, int levelTo) {
        if (levelTo <= levelFrom) {
            return;
        }
        saveEvent(
                userId,
                TYPE_LEVEL_UP,
                SCORE_LEVEL_UP * (levelTo - levelFrom),
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                "from=" + levelFrom + ",to=" + levelTo
        );
    }

    @Transactional
    public void recordAchievementUnlocked(UUID userId, String achievementName) {
        if (achievementName == null || achievementName.isBlank()) {
            return;
        }
        saveEvent(
                userId,
                TYPE_ACHIEVEMENT_UNLOCKED,
                SCORE_ACHIEVEMENT_UNLOCKED,
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                achievementName.trim()
        );
    }

    @Transactional
    public void recordStreakDay(UUID userId, int streakDays) {
        if (streakDays <= 0) {
            return;
        }
        saveEvent(
                userId,
                TYPE_STREAK_DAY,
                SCORE_STREAK_DAY,
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                "days=" + streakDays
        );
    }

    private void saveEvent(
            UUID userId,
            String eventType,
            int activityScore,
            int xpGranted,
            int coinGranted,
            Integer progressPercent,
            UUID lessonId,
            UUID quizId,
            UUID taskId,
            UUID examId,
            String details
    ) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials"));

        ActivityEventEntity event = new ActivityEventEntity();
        event.setUser(user);
        event.setEventType(eventType);
        event.setActivityScore(Math.max(0, activityScore));
        event.setXpGranted(Math.max(0, xpGranted));
        event.setCoinGranted(Math.max(0, coinGranted));
        event.setProgressPercent(clampPercent(progressPercent));
        event.setLessonId(lessonId);
        event.setQuizId(quizId);
        event.setTaskId(taskId);
        event.setExamId(examId);
        event.setDetails(details);
        activityEventRepository.save(event);
    }

    private Integer clampPercent(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }
}
