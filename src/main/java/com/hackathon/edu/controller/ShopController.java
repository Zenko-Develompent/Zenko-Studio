package com.hackathon.edu.controller;

import com.hackathon.edu.dto.shop.ShopDTO;
import com.hackathon.edu.service.AuthService;
import com.hackathon.edu.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {
    private final ShopService shopService;
    private final AuthService authService;

    @GetMapping("/items")
    public ShopDTO.ItemsResponse items() {
        return shopService.getItems();
    }

    @GetMapping("/state")
    public ShopDTO.StateResponse state(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return shopService.getState(userId);
    }

    @PostMapping("/purchase")
    public ShopDTO.PurchaseResponse purchase(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody ShopDTO.PurchaseRequest request
    ) {
        UUID userId = authService.requireUserIdFromAccessHeader(authorizationHeader);
        return shopService.purchase(userId, request.itemId());
    }
}
