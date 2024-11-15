package de.maschell.jsc64lib;

import java.nio.ByteOrder;

public class SC64Defines {
    public enum CMD {
        IDENTIFIER_GET((byte) 'v'),
        SD_CARD_OP((byte) 'i'),
        STATE_RESET((byte) 'R'),
        VERSION_GET((byte) 'V'),
        CONFIG_GET((byte) 'c'),
        CONFIG_SET((byte) 'C'),
        SETTING_GET((byte) 'a'),
        SETTING_SET((byte) 'A');

        private final byte _cmd;

        byte getValue() {
            return _cmd;
        }

        CMD(byte cmd) {
            this._cmd = cmd;
        }
    }

    public enum DataType {
        COMMAND(1),
        RESPONSE(2),
        PACKET(3),
        KEEP_ALIVE(0xCAFEBEEFL);

        private final long _packet;

        DataType(long packet) {
            this._packet = packet;
        }
    }


    public enum SdCardOp {
        Deinit(0),
        Init(1),
        GetStatus(2),
        GetInfo(3),
        ByteSwapOn(4),
        ByteSwapOff(5);

        private final int _op;

        SdCardOp(int op) {
            this._op = op;
        }

        public int getValue() {
            return _op;
        }
    }


    public enum ConfigId {
        BootloaderSwitch(0),
        RomWriteEnable(1),
        RomShadowEnable(2),
        DdMode(3),
        ISViewer(4),
        BootMode(5),
        SaveType(6),
        CicSeed(7),
        TvType(8),
        DdSdEnable(9),
        DdDriveType(10),
        DdDiskState(11),
        ButtonState(12),
        ButtonMode(13),
        RomExtendedEnable(14);

        private final int _id;

        ConfigId(int id) {
            this._id = id;
        }
    }

    public enum SdCardResult {
        OK,
        NoCardInSlot,
        NotInitialized,
        InvalidArgument,
        InvalidAddress,
        InvalidOperation,
        Cmd2IO,
        Cmd3IO,
        Cmd6CheckIO,
        Cmd6CheckCRC,
        Cmd6CheckTimeout,
        Cmd6CheckResponse,
        Cmd6SwitchIO,
        Cmd6SwitchCRC,
        Cmd6SwitchTimeout,
        Cmd6SwitchResponse,
        Cmd7IO,
        Cmd8IO,
        Cmd9IO,
        Cmd10IO,
        Cmd18IO,
        Cmd18CRC,
        Cmd18Timeout,
        Cmd25IO,
        Cmd25CRC,
        Cmd25Timeout,
        Acmd6IO,
        Acmd41IO,
        Acmd41OCR,
        Acmd41Timeout,
        Locked;

        public static SdCardResult createFrom(byte[] data) throws Exception {
            if (data.length < 8) {
                throw new Exception("not enough data");
            }
            var status = ByteUtils.getIntFromBytes(data, 0, ByteOrder.BIG_ENDIAN);
            return SdCardResult.values()[status];
        }
    }

    public static class SdCardOpPacket {
        public SdCardResult result;
        public SdCardStatus status;

        public SdCardOpPacket(SdCardResult result, SdCardStatus status) {
            this.result = result;
            this.status = status;
        }
    }

    public static class SdCardStatus {
        public boolean byte_swap;
        public boolean clock_mode_50mhz;
        public boolean card_type_block;
        public boolean card_initialized;
        public boolean card_inserted;

        public SdCardStatus(byte[] data) throws Exception {
            if (data.length < 8) {
                throw new Exception("not enough data");
            }
            var status = ByteUtils.getIntFromBytes(data, 4, ByteOrder.BIG_ENDIAN);

            byte_swap = (status & ((1 << 4))) > 0;
            clock_mode_50mhz = (status & ((1 << 3))) > 0;
            card_type_block = (status & ((1 << 2))) > 0;
            card_initialized = (status & ((1 << 1))) > 0;
            card_inserted = (status & ((1 << 0))) > 0;
        }
    }

    public static final byte[] RESPONSE_TYPE_CMP = new byte[]{'C', 'M', 'P'};
    public static final byte[] RESPONSE_TYPE_PKT = new byte[]{'P', 'K', 'T'};
    public static final byte[] RESPONSE_TYPE_ERR = new byte[]{'E', 'R', 'R'};

    public static final byte[] CMD_HEADER = new byte[]{'C', 'M', 'D'};
}
