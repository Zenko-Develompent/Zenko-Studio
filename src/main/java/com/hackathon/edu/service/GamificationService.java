package com.hackathon.edu.service;

import com.hackathon.edu.entity.ExemEntity;
import com.hackathon.edu.entity.QuizEntity;
import com.hackathon.edu.entity.TasksEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GamificationService {
    private static final int FIRST_LEVEL_XP = 50;
    private static final int LEVEL_XP_STEP = 50;

    private final UserRepository userRepository;
    private final ActivityEventService activityEventService;

    @Transactional
    public GrantResult grantLessonQuizReward(UUID userId, QuizEntity quiz) {
        GrantResult result = grant(userId, safeInt(quiz.getXpReward()), safeInt(quiz.getCoinReward()));
        triggerAchievement(userId, "lesson_quiz_completed", Map.of("quizId", quiz.getQuizId().toString()));
        return result;
    }

    @Transactional
    public GrantResult grantLessonTaskReward(UUID userId, TasksEntity task) {
        GrantResult result = grant(userId, safeInt(task.getXpReward()), safeInt(task.getCoinReward()));
        triggerAchievement(userId, "lesson_task_completed", Map.of("taskId", task.getTasksId().toString()));
        return result;
    }

    @Transactional
    public GrantResult grantExamReward(UUID userId, ExemEntity exam) {
        GrantResult result = grant(userId, safeInt(exam.getXpReward()), safeInt(exam.getCoinReward()));
        triggerAchievement(userId, "exam_completed", Map.of("examId", exam.getExemId().toString()));
        return result;
    }
    //ззенко
    public void triggerAchievement(UUID userId, String trigger, Map<String, String> context) {
        
    }
    //Golovchenko
    private GrantResult grant(UUID userId, int xp, int coin) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials"));

        int grantedXp = Math.max(0, xp);
        int grantedCoins = Math.max(0, coin);
        int levelBefore = safeInt(user.getLevel());

        int nextXp = safeInt(user.getXp()) + grantedXp;
        user.setXp(nextXp);
        user.setCoins(safeInt(user.getCoins()) + grantedCoins);
        int levelAfter = calculateLevel(nextXp);
        user.setLevel(levelAfter);

        userRepository.save(user);
        if (levelAfter > levelBefore) {
            activityEventService.recordLevelUp(userId, levelBefore, levelAfter);
        }
        return new GrantResult(grantedXp, grantedCoins);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private int calculateLevel(int xp) {
        long xpLeft = Math.max(0L, xp);
        long requiredForNextLevel = FIRST_LEVEL_XP;
        int level = 0;

        while (xpLeft >= requiredForNextLevel) {
            xpLeft -= requiredForNextLevel;
            level++;
            requiredForNextLevel += LEVEL_XP_STEP;
        }

        return level;
    }

    public record GrantResult(int xpGranted, int coinGranted) {
        public static GrantResult none() {
            return new GrantResult(0, 0);
        }
    }
}
