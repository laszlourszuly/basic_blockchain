package com.jayway.blockchain;

import java.lang.IllegalArgumentException;
import java.lang.String;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 *  This class offers convenience hashing methods.
 */
public class HashHelper {
    private static final MessageDigest ALGORITHM;

    static {
        try {
            ALGORITHM = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No SHA-256 algorithm", e);
        }
    }

    /**
     *  Tries to hash a string with the SHA256 algorithm.
     *
     *  @param string   The string to hash.
     *
     *  @return The resulting digested hash expressed as a hex-string.
     */
    public static String hash(final String string) {
        if (string == null)
            throw new IllegalArgumentException("Invalid payload: " + string);

        byte[] hash = ALGORITHM.digest(string.getBytes(StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();

        for(byte b : hash)
            stringBuilder.append(Integer.toHexString(0xFF & b));

        return stringBuilder.toString();
    }
    
}
