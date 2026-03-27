package com.hackathon.edu.websocket;

import java.security.Principal;

public record StompUserPrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
