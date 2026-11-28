package org.unitedlands.unitedlands.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtils {

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 20000;
    private static final int SALT_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    public static String hash(String password) {
        var salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        var hash = derive(password, salt, ITERATIONS, KEY_LENGTH);
        var encoder = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    public static boolean verify(String password, String storedHash) {
        if (password == null || !isHash(storedHash))
            return false;

        var parts = storedHash.split("\\$");
        try {
            var iterations = Integer.parseInt(parts[1]);
            var decoder = Base64.getDecoder();
            var salt = decoder.decode(parts[2]);
            var expected = decoder.decode(parts[3]);
            var actual = derive(password, salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static boolean isHash(String value) {
        return value != null && value.startsWith(PREFIX + "$") && value.split("\\$").length == 4;
    }

    private static byte[] derive(String password, byte[] salt, int iterations, int keyLength) {
        var spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

}
