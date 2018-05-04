package com.jayway.blockchain.blockchain;

import java.lang.IllegalArgumentException;
import java.lang.String;
import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 *  This class offers convenience hashing methods.
 */
public class HashUtils {
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
     *  @return The resulting digested hash byte array.
     */
    public static byte[] hash(final String string) {
        if (string == null)
            throw new IllegalArgumentException("Invalid payload: " + string);

        return ALGORITHM.digest(string.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     *  Converts a byte array to a corresponding hexadecimal based string.
     *
     *  @param string   The string to hash.
     *
     *  @return The hexadecimal notation of the hash result.
     */
    public static String convertToHexString(byte[] hash) {
        if (hash == null)
            throw new IllegalArgumentException("Invalid hash");

        StringBuilder stringBuilder = new StringBuilder();
        for(byte b : hash)
            stringBuilder.append(Integer.toHexString(0xFF & b));

        return stringBuilder.toString();
    }
    
}
