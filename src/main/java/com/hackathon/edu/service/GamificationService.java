package com.hackathon.edu.service;

import com.hackathon.edu.entity.ExamEntity;
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
    private static final int MAX_LEVEL = 150;
    private static final String XP_POTION_ITEM_ID = "xp_potion";
    private static final int DEFAULT_XP_POTION_BONUS_PERCENT = 50;
    private static final int DEFAULT_XP_POTION_EXAM_CHARGE_COST = 5;

    private final UserRepository userRepository;
    private final ActivityEventService activityEventService;
    private final AchievementProgressService achievementProgressService;
    private final ShopCatalogService shopCatalogService;

    @Transactional
    public GrantResult grantLessonQuizReward(UUID userId, QuizEntity quiz) {
        GrantResult result = grant(userId, safeInt(quiz.getXpReward()), safeInt(quiz.getCoinReward()), RewardSource.LESSON_QUIZ);
        triggerAchievement(userId, "lesson_quiz_completed", Map.of("quizId", quiz.getQuizId().toString()));
        return result;
    }

    @Transactional
    public GrantResult grantLessonTaskReward(UUID userId, TasksEntity task) {
        GrantResult result = grant(userId, safeInt(task.getXpReward()), safeInt(task.getCoinReward()), RewardSource.LESSON_TASK);
        triggerAchievement(userId, "lesson_task_completed", Map.of("taskId", task.getTasksId().toString()));
        return result;
    }

    @Transactional
    public GrantResult grantExamReward(UUID userId, ExamEntity exam) {
        GrantResult result = grant(userId, safeInt(exam.getXpReward()), safeInt(exam.getCoinReward()), RewardSource.EXAM);
        triggerAchievement(userId, "exam_completed", Map.of("examId", exam.getExamId().toString()));
        return result;
    }

    public void triggerAchievement(UUID userId, String trigger, Map<String, String> context) {
        achievementProgressService.onTrigger(userId, trigger, context);
    }

    private GrantResult grant(UUID userId, int xp, int coin, RewardSource source) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials"));

        int baseXp = Math.max(0, xp);
        int grantedCoins = Math.max(0, coin);
        int bonusXp = 0;
        int levelBefore = safeInt(user.getLevel());
        int boostCharges = safeInt(user.getXpBoostCharges());
        XpPotionConfig potionConfig = resolveXpPotionConfig();

        if (baseXp > 0 && boostCharges > 0 && potionConfig.bonusPercent() > 0) {
            if (source == RewardSource.EXAM && boostCharges >= potionConfig.examChargeCost()) {
                bonusXp = (int) Math.ceil(baseXp * (potionConfig.bonusPercent() / 100.0d));
                boostCharges -= potionConfig.examChargeCost();
            } else if (source != RewardSource.EXAM) {
                bonusXp = (int) Math.ceil(baseXp * (potionConfig.bonusPercent() / 100.0d));
                boostCharges -= 1;
            }
        }

        int grantedXp = baseXp + Math.max(0, bonusXp);

        int nextXp = safeInt(user.getXp()) + grantedXp;
        user.setXp(nextXp);
        user.setCoins(safeInt(user.getCoins()) + grantedCoins);
        user.setXpBoostCharges(Math.max(0, boostCharges));
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

        while (xpLeft >= requiredForNextLevel && level < MAX_LEVEL) {
            xpLeft -= requiredForNextLevel;
            level++;
            requiredForNextLevel += LEVEL_XP_STEP;
        }

        return level;
    }

    private XpPotionConfig resolveXpPotionConfig() {
        return shopCatalogService.list().stream()
                .filter(item -> XP_POTION_ITEM_ID.equals(item.id()))
                .findFirst()
                .map(item -> new XpPotionConfig(
                        Math.max(0, item.xpBoostPercent()),
                        Math.max(1, item.examChargeCost())
                ))
                .orElse(new XpPotionConfig(DEFAULT_XP_POTION_BONUS_PERCENT, DEFAULT_XP_POTION_EXAM_CHARGE_COST));
    }

    private enum RewardSource {
        LESSON_QUIZ,
        LESSON_TASK,
        EXAM
    }

    private record XpPotionConfig(
            int bonusPercent,
            int examChargeCost
    ) {
    }

    public record GrantResult(int xpGranted, int coinGranted) {
        public static GrantResult none() {
            return new GrantResult(0, 0);
        }
    }
}
