package de.maschell.jsc64lib;

public class Response {
    public final byte id;
    public final byte[] data;
    public final boolean error;

    public Response(byte id, byte[] data, boolean error) {
        this.id = id;
        this.data = data;
        this.error = error;
    }

    public static Response ErrorResponse() {
        return new Response((byte) 0x0, new byte[]{}, false);
    }
}
