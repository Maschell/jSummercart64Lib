package de.maschell.jsc64lib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ByteUtils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] getBytesFromInt(int value, ByteOrder bo) {
        byte[] result = new byte[0x04];
        ByteBuffer buffer = ByteBuffer.allocate(4).order(bo).putInt(value);
        buffer.position(0);
        buffer.get(result);
        return result;
    }

    public static byte[] getBytesFromIntBE(int value) {
        return getBytesFromInt(value, ByteOrder.BIG_ENDIAN);
    }

    public static int getIntFromBytes(byte[] input, int offset, ByteOrder bo) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(bo);
        Arrays.copyOfRange(input, offset, offset + 4);
        buffer.put(Arrays.copyOfRange(input, offset, offset + 4));

        return buffer.getInt(0);
    }

    public static short getShortFromBytes(byte[] input, int offset, ByteOrder bo) {
        return ByteBuffer.wrap(Arrays.copyOfRange(input, offset, offset + 2)).order(bo).getShort();
    }

    public static short getShortFromBytesBE(byte[] input, int offset) {
        return getShortFromBytes(input, offset, ByteOrder.BIG_ENDIAN);
    }

}

