package com.hackathon.edu.util;

public final class PasswordPolicy {
    private PasswordPolicy() {
    }

    public static boolean accept(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return hasLetter && hasDigit;
    }
}
