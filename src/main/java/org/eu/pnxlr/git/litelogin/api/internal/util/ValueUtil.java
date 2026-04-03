package org.eu.pnxlr.git.litelogin.api.internal.util;

import org.eu.pnxlr.git.litelogin.api.internal.util.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Value utilities.
 */
@ApiStatus.Internal
public class ValueUtil {
    /**
     * Converts a UUID to bytes.
     *
     * @param uuid UUID to convert
     * @return converted bytes
     */
    public static byte[] uuidToBytes(UUID uuid) {
        byte[] uuidBytes = new byte[16];
        ByteBuffer.wrap(uuidBytes).order(ByteOrder.BIG_ENDIAN).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits());
        return uuidBytes;
    }

    /**
     * Converts bytes to a UUID.
     *
     * @param bytes bytes to convert
     * @return converted UUID
     */
    public static UUID bytesToUuid(byte[] bytes) {
        if (bytes.length != 16) return null;
        int i = 0;
        long msl = 0;
        for (; i < 8; i++) {
            msl = (msl << 8) | (bytes[i] & 0xFF);
        }
        long lsl = 0;
        for (; i < 16; i++) {
            lsl = (lsl << 8) | (bytes[i] & 0xFF);
        }
        return new UUID(msl, lsl);
    }

    /**
     * Creates a UUID from a string.
     *
     * @param uuid string value
     * @return matching UUID, or null if invalid
     */
    public static UUID getUuidOrNull(String uuid) {
        UUID ret = null;
        try {
            ret = UUID.fromString(uuid.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
        } catch (Exception ignored) {
        }
        return ret;
    }

    /**
     * Checks whether a string is empty.
     *
     * @param str string to check
     * @return whether the string is empty
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static String transPapi(String s, Pair<?, ?>... pairs) {
        for (int i = 0; i < pairs.length; i++) {
            s = s.replace("{" + pairs[i].getValue1() + "}", pairs[i].getValue2() + "");
            s = s.replace("{" + i + "}", pairs[i].getValue2() + "");
        }
        return s;
    }

    /**
     * Joins strings.
     */
    public static String join(CharSequence delimiter, CharSequence lastDelimiter, Object... elements) {
        if (elements.length == 0) return "";
        if (elements.length == 1) return elements[0].toString();
        StringJoiner joiner = new StringJoiner(delimiter);
        for (int i = 0; i < elements.length - 1; i++) {
            joiner.add(elements[i].toString());
        }
        return joiner.toString() + lastDelimiter + elements[elements.length - 1];
    }

    /**
     * Returns the SHA-256 hash of a string.
     */
    public static byte[] sha256(String str) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(str.getBytes(StandardCharsets.UTF_8));
    }
}
