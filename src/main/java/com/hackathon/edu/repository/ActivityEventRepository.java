package com.hackathon.edu.repository;

import com.hackathon.edu.entity.ActivityEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEventEntity, UUID> {
    long countByUser_UserIdAndEventType(UUID userId, String eventType);

    @Query("""
            select e.user.userId as userId,
                   e.user.username as username,
                   coalesce(sum(e.activityScore), 0) as score
            from ActivityEventEntity e
            where e.createdAt >= :fromInclusive and e.createdAt <= :toInclusive
            group by e.user.userId, e.user.username
            order by coalesce(sum(e.activityScore), 0) desc, e.user.username asc
            """)
    List<LeaderboardRow> findLeaderboardByActivityScore(
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toInclusive") OffsetDateTime toInclusive,
            Pageable pageable
    );

    @Query("""
            select e.user.userId as userId,
                   e.user.username as username,
                   coalesce(sum(e.xpGranted), 0) as score
            from ActivityEventEntity e
            where e.createdAt >= :fromInclusive and e.createdAt <= :toInclusive
            group by e.user.userId, e.user.username
            order by coalesce(sum(e.xpGranted), 0) desc, e.user.username asc
            """)
    List<LeaderboardRow> findLeaderboardByXp(
            @Param("fromInclusive") OffsetDateTime fromInclusive,
            @Param("toInclusive") OffsetDateTime toInclusive,
            Pageable pageable
    );

    @Query("""
            select e.eventId as eventId,
                   e.createdAt as createdAt,
                   e.user.userId as userId,
                   e.user.username as username,
                   e.eventType as eventType,
                   e.activityScore as activityScore,
                   e.xpGranted as xpGranted,
                   e.coinGranted as coinGranted,
                   e.progressPercent as progressPercent,
                   e.lessonId as lessonId,
                   e.quizId as quizId,
                   e.taskId as taskId,
                   e.examId as examId,
                   e.details as details
            from ActivityEventEntity e
            order by e.createdAt desc, e.eventId desc
            """)
    List<FeedRow> findLatestFeed(Pageable pageable);

    @Query("""
            select e.eventId as eventId,
                   e.createdAt as createdAt,
                   e.user.userId as userId,
                   e.user.username as username,
                   e.eventType as eventType,
                   e.activityScore as activityScore,
                   e.xpGranted as xpGranted,
                   e.coinGranted as coinGranted,
                   e.progressPercent as progressPercent,
                   e.lessonId as lessonId,
                   e.quizId as quizId,
                   e.taskId as taskId,
                   e.examId as examId,
                   e.details as details
            from ActivityEventEntity e
            where e.user.userId = :userId and e.eventType in :eventTypes
            order by e.createdAt desc, e.eventId desc
            """)
    List<FeedRow> findLatestByUserIdAndEventTypeIn(
            @Param("userId") UUID userId,
            @Param("eventTypes") List<String> eventTypes,
            Pageable pageable
    );

    @Query("""
            select max(e.createdAt)
            from ActivityEventEntity e
            where e.user.userId = :userId
            """)
    OffsetDateTime findLastActivityAtByUserId(@Param("userId") UUID userId);

    interface LeaderboardRow {
        UUID getUserId();

        String getUsername();

        Long getScore();
    }

    interface FeedRow {
        UUID getEventId();

        OffsetDateTime getCreatedAt();

        UUID getUserId();

        String getUsername();

        String getEventType();

        Integer getActivityScore();

        Integer getXpGranted();

        Integer getCoinGranted();

        Integer getProgressPercent();

        UUID getLessonId();

        UUID getQuizId();

        UUID getTaskId();

        UUID getExamId();

        String getDetails();
    }
}
