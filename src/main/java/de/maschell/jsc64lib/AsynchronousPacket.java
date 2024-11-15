package de.maschell.jsc64lib;

public class AsynchronousPacket extends Response {
    public AsynchronousPacket(byte id, byte[] data) {
        super(id, data, false);
    }
}
