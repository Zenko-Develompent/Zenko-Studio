package com.hackathon.edu.controller;

import com.hackathon.edu.dto.parent.ParentControlDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.ParentControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/parent")
@RequiredArgsConstructor
public class ParentControlController {
    private final ParentControlService parentControlService;
    private final AuthService authService;

    @PostMapping("/requests")
    public ParentControlDTO.SendRequestResponse sendRequest(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ParentControlDTO.SendRequestRequest request
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return parentControlService.sendRequest(userId, request.childUserId());
    }

    @GetMapping("/requests/outgoing")
    public ParentControlDTO.RequestsResponse outgoing(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return parentControlService.outgoing(userId, limit);
    }

    @GetMapping("/requests/incoming")
    public ParentControlDTO.RequestsResponse incoming(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return parentControlService.incoming(userId, limit);
    }

    @PostMapping("/requests/{requestId}/accept")
    public ParentControlDTO.AcceptRejectResponse accept(
            @PathVariable("requestId") UUID requestId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return parentControlService.accept(userId, requestId);
    }

    @PostMapping("/requests/{requestId}/reject")
    public ParentControlDTO.AcceptRejectResponse reject(
            @PathVariable("requestId") UUID requestId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return parentControlService.reject(userId, requestId);
    }

    @GetMapping("/children")
    public ParentControlDTO.ChildrenResponse children(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return parentControlService.children(userId);
    }

    @GetMapping("/children/{childId}/dashboard")
    public ParentControlDTO.DashboardResponse dashboard(
            @PathVariable("childId") UUID childId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return parentControlService.dashboard(userId, childId);
    }
}
