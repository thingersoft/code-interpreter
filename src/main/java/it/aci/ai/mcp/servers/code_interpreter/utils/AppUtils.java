package it.aci.ai.mcp.servers.code_interpreter.utils;

import java.security.SecureRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;

public final class AppUtils {

    // Allow uppercase/lowercase letters, digits, dashes and unserscores
    private static final char[] UID_ALPHABET = Stream.of(
            IntStream.rangeClosed('A', 'Z'),
            IntStream.rangeClosed('a', 'z'),
            IntStream.rangeClosed('0', '9'))
            .flatMapToInt(s -> s)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .append('-')
            .append('_')
            .toString().toCharArray();
    private static final int UID_LENGTH = 21;
    private static final SecureRandom UID_RANDOM = new SecureRandom();

    public static String generateSessionId() {
        return generateFileSystemUid("sess-");
    }

    public static String generateFileId() {
        return generateFileSystemUid(null);
    }

    public static String generateFileSystemUid(String prefix) {
        int uidLength = UID_LENGTH;
        if (prefix == null) {
            prefix = "";
        }
        uidLength = uidLength - prefix.length();
        return prefix + NanoIdUtils.randomNanoId(UID_RANDOM, UID_ALPHABET, uidLength);
    }

    private AppUtils() {
    }

}
