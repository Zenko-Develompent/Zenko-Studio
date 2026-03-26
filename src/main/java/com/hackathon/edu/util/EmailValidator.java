package com.hackathon.edu.util;

public final class EmailValidator {
    private EmailValidator() {
    }

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        return email.length() <= 254
                && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
