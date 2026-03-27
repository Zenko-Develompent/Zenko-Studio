package com.hackathon.edu.service;

import com.hackathon.edu.dto.community.CommunityDTO;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ActivityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
    //зенко
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {
    private static final int DEFAULT_LEADERBOARD_LIMIT = 9;
    private static final int DEFAULT_FEED_LIMIT = 10;
    private static final int MAX_LEADERBOARD_LIMIT = 9;
    private static final int MAX_FEED_LIMIT = 100;

    private final ActivityEventRepository activityEventRepository;

    public CommunityDTO.LeaderboardResponse leaderboard(String periodRaw, String metricRaw, Integer limitRaw) {
        Period period = parsePeriod(periodRaw);
        Metric metric = parseMetric(metricRaw);
        int limit = normalizeLimit(limitRaw, DEFAULT_LEADERBOARD_LIMIT, MAX_LEADERBOARD_LIMIT);

        OffsetDateTime toInclusive = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime fromInclusive = toInclusive.minusDays(period.days);
        PageRequest page = PageRequest.of(0, limit);

        List<ActivityEventRepository.LeaderboardRow> rows = metric == Metric.ACTIVITY
                ? activityEventRepository.findLeaderboardByActivityScore(fromInclusive, toInclusive, page)
                : activityEventRepository.findLeaderboardByXp(fromInclusive, toInclusive, page);

        List<CommunityDTO.LeaderboardItem> items = new ArrayList<>(rows.size());
        int rank = 1;
        for (ActivityEventRepository.LeaderboardRow row : rows) {
            items.add(new CommunityDTO.LeaderboardItem(
                    rank++,
                    row.getUserId(),
                    row.getUsername(),
                    row.getScore() == null ? 0L : Math.max(0L, row.getScore())
            ));
        }

        return new CommunityDTO.LeaderboardResponse(
                period.value,
                metric.value,
                fromInclusive,
                toInclusive,
                items
        );
    }

    public CommunityDTO.FeedResponse feed(Integer limitRaw) {
        int limit = normalizeLimit(limitRaw, DEFAULT_FEED_LIMIT);
        List<ActivityEventRepository.FeedRow> rows = activityEventRepository.findLatestFeed(PageRequest.of(0, limit));

        List<CommunityDTO.FeedItem> items = rows.stream()
                .map(row -> new CommunityDTO.FeedItem(
                        row.getEventId(),
                        row.getCreatedAt(),
                        row.getUserId(),
                        row.getUsername(),
                        row.getEventType(),
                        safeInt(row.getActivityScore()),
                        safeInt(row.getXpGranted()),
                        safeInt(row.getCoinGranted()),
                        row.getProgressPercent(),
                        row.getLessonId(),
                        row.getQuizId(),
                        row.getTaskId(),
                        row.getExamId(),
                        row.getDetails()
                ))
                .toList();

        return new CommunityDTO.FeedResponse(items);
    }

    private int normalizeLimit(Integer limitRaw, int defaultValue) {
        return normalizeLimit(limitRaw, defaultValue, MAX_FEED_LIMIT);
    }

    private int normalizeLimit(Integer limitRaw, int defaultValue, int maxLimit) {
        int limit = limitRaw == null ? defaultValue : limitRaw;
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, maxLimit);
    }

    private Period parsePeriod(String raw) {
        if (raw == null || raw.isBlank()) {
            return Period.WEEK;
        }
        String value = raw.trim().toLowerCase();
        return switch (value) {
            case "day" -> Period.DAY;
            case "week" -> Period.WEEK;
            case "month" -> Period.MONTH;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "community_period_invalid");
        };
    }

    private Metric parseMetric(String raw) {
        if (raw == null || raw.isBlank()) {
            return Metric.ACTIVITY;
        }
        String value = raw.trim().toLowerCase();
        return switch (value) {
            case "activity" -> Metric.ACTIVITY;
            case "xp" -> Metric.XP;
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "community_metric_invalid");
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private enum Period {
        DAY("day", 1),
        WEEK("week", 7),
        MONTH("month", 30);

        private final String value;
        private final int days;

        Period(String value, int days) {
            this.value = value;
            this.days = days;
        }
    }

    private enum Metric {
        ACTIVITY("activity"),
        XP("xp");

        private final String value;

        Metric(String value) {
            this.value = value;
        }
    }
}
