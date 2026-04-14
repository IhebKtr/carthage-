package com.carthage.utils;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDUtils {

    /**
     * Converts a binary(16) byte array from MySQL into a java.util.UUID.
     */
    public static UUID fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 16) return null;
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    /**
     * Converts a UUID into a 16-byte array for binary(16) storage.
     */
    public static byte[] toBytes(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
    
    /**
     * Helper to clean a UUID string from dashes for MySQL REPLACE/UNHEX logic.
     */
    public static String toCleanString(UUID uuid) {
        if (uuid == null) return null;
        return uuid.toString().replace("-", "");
    }
}
