package com.hackathon.edu.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHasher {
    private static final SecureRandom RND = new SecureRandom();
    private static final int SALT_LEN = 16;
    private static final int HASH_LEN = 32;
    private static final int ITER = 210_000;

    private PasswordHasher() {
    }

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_LEN];
        RND.nextBytes(salt);
        byte[] dk = pbkdf2(password, salt, ITER, HASH_LEN);
        return "pbkdf2$sha256$iter=" + ITER + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(dk);
    }

    public static boolean verify(char[] password, String phc) {
        try {
            String[] p = phc.split("\\$");
            int iter = Integer.parseInt(p[2].split("=")[1]);
            byte[] salt = Base64.getUrlDecoder().decode(p[3]);
            byte[] expected = Base64.getUrlDecoder().decode(p[4]);
            byte[] got = pbkdf2(password, salt, iter, expected.length);
            return constantTimeEq(expected, got);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] pwd, byte[] salt, int iter, int dkLen) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pwd, salt, iter, dkLen * 8);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEq(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length; i++) {
            r |= a[i] ^ b[i];
        }
        return r == 0;
    }
}
