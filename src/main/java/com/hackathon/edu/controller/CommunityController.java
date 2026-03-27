package com.hackathon.edu.controller;

import com.hackathon.edu.dto.community.CommunityDTO;
import com.hackathon.edu.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communityService;

    @GetMapping("/leaderboard")
    public CommunityDTO.LeaderboardResponse leaderboard(
            @RequestParam(name = "period", required = false) String period,
            @RequestParam(name = "metric", required = false) String metric,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return communityService.leaderboard(period, metric, limit);
    }

    @GetMapping("/feed")
    public CommunityDTO.FeedResponse feed(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return communityService.feed(limit);
    }
}
