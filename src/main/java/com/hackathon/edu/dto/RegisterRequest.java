package com.hackathon.edu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank
        String username,
        @NotBlank
        String password,
        @NotNull
        @Min(1)
        @Max(120)
        Integer age,
        @NotBlank
        String role
) {
}
