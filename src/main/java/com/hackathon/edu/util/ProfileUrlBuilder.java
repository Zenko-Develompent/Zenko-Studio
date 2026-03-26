package com.hackathon.edu.util;

import java.util.UUID;

public final class ProfileUrlBuilder {
    private ProfileUrlBuilder() {
    }

    public static String avatarUrl(UUID userId, int rev) {
        String hex = userId.toString().replace("-", "");
        int safeRev = Math.max(0, rev);
        return "/media/avatars/" + hex + "@" + safeRev + ".png";
    }

    public static String bannerUrl(UUID userId, int rev) {
        if (rev <= 0) {
            return null;
        }
        String hex = userId.toString().replace("-", "");
        return "/media/profile-headers/" + hex + "@" + rev + ".png";
    }

    public static String wallpaperUrl(UUID userId, int rev) {
        if (rev <= 0) {
            return null;
        }
        String hex = userId.toString().replace("-", "");
        return "/media/wallpapers/" + hex + "@" + rev + ".png";
    }
}
