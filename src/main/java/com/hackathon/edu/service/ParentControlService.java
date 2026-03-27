package com.hackathon.edu.service;

import com.hackathon.edu.dto.parent.ParentControlDTO;
import com.hackathon.edu.dto.progress.ProgressDTO;
import com.hackathon.edu.entity.CourseEntity;
import com.hackathon.edu.entity.LessonEntity;
import com.hackathon.edu.entity.ModuleEntity;
import com.hackathon.edu.entity.ParentChildLinkEntity;
import com.hackathon.edu.entity.ParentControlRequestEntity;
import com.hackathon.edu.entity.UserEntity;
import com.hackathon.edu.exception.ApiException;
import com.hackathon.edu.repository.ActivityEventRepository;
import com.hackathon.edu.repository.CourseRepository;
import com.hackathon.edu.repository.ModuleRepository;
import com.hackathon.edu.repository.ParentChildLinkRepository;
import com.hackathon.edu.repository.ParentControlRequestRepository;
import com.hackathon.edu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentControlService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_REJECTED = "REJECTED";

    private static final List<String> DASHBOARD_EVENT_TYPES = List.of(
            ActivityEventService.TYPE_QUIZ_COMPLETED,
            ActivityEventService.TYPE_TASK_COMPLETED,
            ActivityEventService.TYPE_EXAM_COMPLETED
    );

    private final ParentControlRequestRepository parentControlRequestRepository;
    private final ParentChildLinkRepository parentChildLinkRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final ActivityEventRepository activityEventRepository;
    private final ProgressQueryService progressQueryService;
    private final SocialRealtimeService socialRealtimeService;

    @Transactional
    public ParentControlDTO.SendRequestResponse sendRequest(UUID parentUserId, UUID childUserId) {
        UserEntity parent = requireUser(parentUserId);
        requireRoleParent(parent);

        if (parentUserId.equals(childUserId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "parent_control_request_self");
        }

        UserEntity child = requireUser(childUserId);
        requireRoleStudent(child);

        if (parentChildLinkRepository.existsByParentUserIdAndChildUserId(parentUserId, childUserId)) {
            throw new ApiException(HttpStatus.CONFLICT, "parent_control_already_active");
        }
        if (parentControlRequestRepository.existsByParentUserIdAndChildUserIdAndStatus(
                parentUserId,
                childUserId,
                STATUS_PENDING
        )) {
            throw new ApiException(HttpStatus.CONFLICT, "parent_control_request_already_exists");
        }

        ParentControlRequestEntity request = new ParentControlRequestEntity();
        request.setParentUserId(parentUserId);
        request.setChildUserId(childUserId);
        request.setStatus(STATUS_PENDING);
        request = parentControlRequestRepository.save(request);
        socialRealtimeService.publishParentControlRequest(request);

        return new ParentControlDTO.SendRequestResponse(request.getRequestId(), request.getStatus());
    }

    public ParentControlDTO.RequestsResponse incoming(UUID childUserId, Integer limit) {
        requireRoleStudent(requireUser(childUserId));
        int safeLimit = normalizeLimit(limit, 20, 50);

        List<ParentControlRequestEntity> rows = parentControlRequestRepository
                .findByChildUserIdAndStatusOrderByCreatedAtDesc(childUserId, STATUS_PENDING)
                .stream()
                .limit(safeLimit)
                .toList();

        return new ParentControlDTO.RequestsResponse(toRequestItems(rows));
    }

    public ParentControlDTO.RequestsResponse outgoing(UUID parentUserId, Integer limit) {
        requireRoleParent(requireUser(parentUserId));
        int safeLimit = normalizeLimit(limit, 20, 50);

        List<ParentControlRequestEntity> rows = parentControlRequestRepository
                .findByParentUserIdAndStatusOrderByCreatedAtDesc(parentUserId, STATUS_PENDING)
                .stream()
                .limit(safeLimit)
                .toList();

        return new ParentControlDTO.RequestsResponse(toRequestItems(rows));
    }

    @Transactional
    public ParentControlDTO.AcceptRejectResponse accept(UUID childUserId, UUID requestId) {
        requireRoleStudent(requireUser(childUserId));

        ParentControlRequestEntity request = parentControlRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "parent_control_request_not_found"));
        requireRoleParent(requireUser(request.getParentUserId()));
        requireRoleStudent(requireUser(request.getChildUserId()));

        if (!childUserId.equals(request.getChildUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "parent_control_forbidden");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "parent_control_request_not_pending");
        }

        request.setStatus(STATUS_ACCEPTED);
        request.setRespondedAt(OffsetDateTime.now());
        parentControlRequestRepository.save(request);
        socialRealtimeService.publishParentControlAccepted(request);

        ensureParentChildLink(request.getParentUserId(), request.getChildUserId());
        return new ParentControlDTO.AcceptRejectResponse(request.getRequestId(), request.getStatus());
    }

    @Transactional
    public ParentControlDTO.AcceptRejectResponse reject(UUID childUserId, UUID requestId) {
        requireRoleStudent(requireUser(childUserId));

        ParentControlRequestEntity request = parentControlRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "parent_control_request_not_found"));

        if (!childUserId.equals(request.getChildUserId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "parent_control_forbidden");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "parent_control_request_not_pending");
        }

        request.setStatus(STATUS_REJECTED);
        request.setRespondedAt(OffsetDateTime.now());
        parentControlRequestRepository.save(request);
        socialRealtimeService.publishParentControlRejected(request);

        return new ParentControlDTO.AcceptRejectResponse(request.getRequestId(), request.getStatus());
    }

    public ParentControlDTO.ChildrenResponse children(UUID parentUserId) {
        requireRoleParent(requireUser(parentUserId));
        List<ParentChildLinkEntity> rows = parentChildLinkRepository.findByParentUserIdOrderByCreatedAtDesc(parentUserId);

        Map<UUID, UserEntity> users = getUsersMap(rows.stream().map(ParentChildLinkEntity::getChildUserId).toList());
        List<ParentControlDTO.ChildItem> items = rows.stream()
                .map(link -> {
                    UserEntity child = users.get(link.getChildUserId());
                    return new ParentControlDTO.ChildItem(
                            link.getChildUserId(),
                            child == null ? null : child.getUsername(),
                            link.getCreatedAt()
                    );
                })
                .toList();
        return new ParentControlDTO.ChildrenResponse(items);
    }

    public ParentControlDTO.DashboardResponse dashboard(UUID parentUserId, UUID childUserId) {
        requireRoleParent(requireUser(parentUserId));
        if (!parentChildLinkRepository.existsByParentUserIdAndChildUserId(parentUserId, childUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "parent_control_forbidden");
        }

        UserEntity child = requireUser(childUserId);
        requireRoleStudent(child);

        List<ParentControlDTO.ProgressItem> courseItems = new ArrayList<>();
        List<ParentControlDTO.ProgressItem> moduleItems = new ArrayList<>();
        List<ParentControlDTO.ProgressItem> lessonItems = new ArrayList<>();

        List<CourseEntity> courses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "name", "courseId"));
        for (CourseEntity course : courses) {
            ProgressDTO.ProgressResponse p = progressQueryService.getCourseProgress(childUserId, course.getCourseId());
            courseItems.add(new ParentControlDTO.ProgressItem(
                    course.getCourseId(),
                    course.getName(),
                    course.getCourseId(),
                    null,
                    p.percent(),
                    p.completed(),
                    p.doneItems(),
                    p.totalItems()
            ));

            List<ModuleEntity> modules = moduleRepository.findWithExamAndLessonsByCourseIdOrderByNameAsc(course.getCourseId());
            for (ModuleEntity module : modules) {
                ProgressDTO.ProgressResponse mp = progressQueryService.getModuleProgress(childUserId, module.getModuleId());
                moduleItems.add(new ParentControlDTO.ProgressItem(
                        module.getModuleId(),
                        module.getName(),
                        course.getCourseId(),
                        module.getModuleId(),
                        mp.percent(),
                        mp.completed(),
                        mp.doneItems(),
                        mp.totalItems()
                ));

                List<LessonEntity> lessons = module.getLessons() == null
                        ? List.of()
                        : module.getLessons().stream()
                        .sorted(java.util.Comparator.comparing(
                                lesson -> safeLower(lesson.getName())
                        ))
                        .toList();

                for (LessonEntity lesson : lessons) {
                    ProgressDTO.ProgressResponse lp = progressQueryService.getLessonProgress(childUserId, lesson.getLessonId());
                    lessonItems.add(new ParentControlDTO.ProgressItem(
                            lesson.getLessonId(),
                            lesson.getName(),
                            course.getCourseId(),
                            module.getModuleId(),
                            lp.percent(),
                            lp.completed(),
                            lp.doneItems(),
                            lp.totalItems()
                    ));
                }
            }
        }

        List<ActivityEventRepository.FeedRow> rows = activityEventRepository.findLatestByUserIdAndEventTypeIn(
                childUserId,
                DASHBOARD_EVENT_TYPES,
                PageRequest.of(0, 10)
        );
        List<ParentControlDTO.ActivityItem> recentActivities = rows.stream()
                .map(this::toActivityItem)
                .toList();

        ParentControlDTO.ChildSummary summary = new ParentControlDTO.ChildSummary(
                child.getUserId(),
                child.getUsername(),
                child.getXp() == null ? 0 : child.getXp(),
                child.getLevel() == null ? 0 : child.getLevel(),
                child.getCoins() == null ? 0 : child.getCoins(),
                activityEventRepository.findLastActivityAtByUserId(childUserId)
        );

        return new ParentControlDTO.DashboardResponse(
                summary,
                courseItems,
                moduleItems,
                lessonItems,
                recentActivities
        );
    }

    private ParentControlDTO.ActivityItem toActivityItem(ActivityEventRepository.FeedRow row) {
        return new ParentControlDTO.ActivityItem(
                row.getEventId(),
                row.getCreatedAt(),
                row.getEventType(),
                row.getProgressPercent(),
                row.getXpGranted(),
                row.getCoinGranted(),
                row.getLessonId(),
                row.getQuizId(),
                row.getTaskId(),
                row.getExamId(),
                row.getDetails()
        );
    }

    private List<ParentControlDTO.RequestItem> toRequestItems(List<ParentControlRequestEntity> rows) {
        Set<UUID> ids = rows.stream()
                .flatMap(row -> java.util.stream.Stream.of(row.getParentUserId(), row.getChildUserId()))
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, UserEntity> users = getUsersMap(ids);

        return rows.stream()
                .map(row -> new ParentControlDTO.RequestItem(
                        row.getRequestId(),
                        row.getParentUserId(),
                        users.get(row.getParentUserId()) == null ? null : users.get(row.getParentUserId()).getUsername(),
                        row.getChildUserId(),
                        users.get(row.getChildUserId()) == null ? null : users.get(row.getChildUserId()).getUsername(),
                        row.getStatus(),
                        row.getCreatedAt(),
                        row.getRespondedAt()
                ))
                .toList();
    }

    private Map<UUID, UserEntity> getUsersMap(Iterable<UUID> userIds) {
        Map<UUID, UserEntity> map = new HashMap<>();
        for (UserEntity user : userRepository.findAllById(userIds)) {
            map.put(user.getUserId(), user);
        }
        return map;
    }

    private void ensureParentChildLink(UUID parentUserId, UUID childUserId) {
        if (parentChildLinkRepository.existsByParentUserIdAndChildUserId(parentUserId, childUserId)) {
            return;
        }
        ParentChildLinkEntity link = new ParentChildLinkEntity();
        link.setParentUserId(parentUserId);
        link.setChildUserId(childUserId);
        parentChildLinkRepository.save(link);
    }

    private UserEntity requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found"));
    }

    private void requireRoleParent(UserEntity user) {
        if (!hasRole(user, "parent")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "parent_control_parent_role_required");
        }
    }

    private void requireRoleStudent(UserEntity user) {
        if (!hasRole(user, "student")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "parent_control_child_role_required");
        }
    }

    private boolean hasRole(UserEntity user, String expectedRole) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }
        return expectedRole.equals(user.getRole().getName().trim().toLowerCase(Locale.ROOT));
    }

    private int normalizeLimit(Integer value, int defaultValue, int maxValue) {
        int safe = value == null || value <= 0 ? defaultValue : value;
        return Math.min(safe, maxValue);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
