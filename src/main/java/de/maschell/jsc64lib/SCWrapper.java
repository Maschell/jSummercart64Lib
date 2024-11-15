package de.maschell.jsc64lib;

import com.fazecast.jSerialComm.SerialPort;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Optional;

import static de.maschell.jsc64lib.SC64Defines.*;

public class SCWrapper {
    int SUPPORTED_MAJOR_VERSION = 2;
    int SUPPORTED_MINOR_VERSION = 20;

    private final SerialPort _serialPort;

    public SCWrapper(SerialPort serialPort) {
        this._serialPort = serialPort;
    }

    private void send_command(CMD id, int[] args, byte[] data) {

        _serialPort.writeBytes(CMD_HEADER, CMD_HEADER.length);
        _serialPort.writeBytes(new byte[]{id.getValue()}, 1);

        if (args == null || args.length != 2) {
            args = new int[]{0, 0};
        }

        for (var arg : args) {
            _serialPort.writeBytes(ByteUtils.getBytesFromIntBE(arg), 4);
        }

        if (data != null) {
            _serialPort.writeBytes(data, data.length);
        }

        _serialPort.flushIOBuffers();
    }


    private Optional<Response> process_incoming_data(DataType dataType) throws Exception {

        while (true) {
            byte[] header = new byte[4];
            var read_len = _serialPort.readBytes(header, header.length);


            if (read_len < 4) {
                throw new Exception("Failed to read header");
            }

            if (Arrays.compare(header, 0, 3, RESPONSE_TYPE_CMP, 0, 3) == 0 ||
                    Arrays.compare(header, 0, 3, RESPONSE_TYPE_PKT, 0, 3) == 0 ||
                    Arrays.compare(header, 0, 3, RESPONSE_TYPE_ERR, 0, 3) == 0) {
                var id = header[3];

                byte[] payload_len_buf = new byte[4];
                read_len = _serialPort.readBytes(payload_len_buf, payload_len_buf.length);
                if (read_len < 4) {
                    throw new Exception("Failed to read header");
                }
                var payload_length = ByteUtils.getIntFromBytes(payload_len_buf, 0, ByteOrder.BIG_ENDIAN);
                byte[] payload = new byte[payload_length];
                read_len = _serialPort.readBytes(payload, payload.length);
                if (read_len != payload.length) {
                    throw new Exception("Failed to read payload");
                }
                if (Arrays.compare(header, 0, 3, RESPONSE_TYPE_PKT, 0, 3) == 0) {
                    if (dataType != DataType.PACKET) {
                        return Optional.empty();
                    }
                    return Optional.of(new AsynchronousPacket(id, payload));
                }
                return Optional.of(new Response(id, payload, Arrays.compare(header, 0, 3, RESPONSE_TYPE_ERR, 0, 3) == 0));
            }
            break;
        }
        return Optional.empty();
    }

    private Response receive_response() throws Exception {
        var response = process_incoming_data(DataType.RESPONSE);
        return response.orElseThrow(() -> new Exception("Err"));
    }

    private byte[] execute_command_raw(CMD id, int[] args, byte[] data, boolean no_response, boolean ignore_error) throws Exception {
        send_command(id, args, data);
        var response = receive_response();
        if (response.id != id.getValue()) {
            throw new Exception("Invalid command response");
        }
        if (!ignore_error && response.error) {
            throw new Exception("Error while executing command");
        }
        return response.data;
    }

    public byte[] execute_command(CMD id, int[] args, byte[] data) throws Exception {
        return execute_command_raw(id, args, data, false, false);
    }

    public SdCardOpPacket command_sd_card_operation(SC64Defines.SdCardOp op) throws Exception {
        var data = execute_command_raw(SC64Defines.CMD.SD_CARD_OP, new int[]{op.getValue(), 0}, new byte[0], false, true);
        if (data.length != 8) {
            throw new Exception(
                    "Invalid data length received for SD card operation command"
            );
        }

        SdCardResult value = SdCardResult.createFrom(data);
        SdCardStatus status = new SdCardStatus(data);

        return new SdCardOpPacket(value, status);
    }

    public boolean open() {
        _serialPort.setBaudRate(115200);
        var res = this._serialPort.openPort();
        System.out.println(res);

        _serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 100);

        return res;
    }

    public void close() {
        this._serialPort.closePort();
    }

    public boolean reset() throws Exception {
        long startTime = System.currentTimeMillis();

        _serialPort.getOutputStream().flush();

        _serialPort.setDTR();
        while (!_serialPort.getDTR()) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            if (estimatedTime > 1000) {
                throw new Exception("Reset timeout");
            }
        }

        var _ = _serialPort.getInputStream().skip(_serialPort.bytesAvailable());

        startTime = System.currentTimeMillis();
        _serialPort.clearDTR();
        while (_serialPort.getDTR()) {
            long estimatedTime = System.currentTimeMillis() - startTime;
            if (estimatedTime > 1000) {
                throw new Exception("Reset timeout");
            }
        }

        return true;
    }

    public byte[] command_identifier_get() throws Exception {
        var data = execute_command(CMD.IDENTIFIER_GET, new int[0], new byte[0]);
        if (data.length != 4) {
            throw new Exception("Unexpected size");
        }
        return data;
    }

    public int command_config_get(SC64Defines.ConfigId configId) throws Exception {
        var data = execute_command(SC64Defines.CMD.CONFIG_GET, new int[]{configId.ordinal(), 0}, new byte[0]);
        if (data.length != 4) {
            throw new Exception("Unexpected size");
        }
        return ByteUtils.getIntFromBytes(data, 0, ByteOrder.BIG_ENDIAN);
    }

    public void command_config_set(SC64Defines.ConfigId configId, int val) throws Exception {
        execute_command(SC64Defines.CMD.CONFIG_SET, new int[]{configId.ordinal(), val}, new byte[0]);
    }

    public void command_state_reset() throws Exception {
        execute_command(SC64Defines.CMD.STATE_RESET, null, null);
    }

    public byte[] command_version_get() throws Exception {
        return execute_command(CMD.VERSION_GET, null, null);
    }

    public SdCardOpPacket init_sd_card() throws Exception {
        return command_sd_card_operation(SdCardOp.Init);
    }

    public SdCardOpPacket deinit_sd_card() throws Exception {
        return command_sd_card_operation(SdCardOp.Deinit);
    }

    public SdCardOpPacket get_sd_card_status() throws Exception {
        return command_sd_card_operation(SdCardOp.GetStatus);
    }

    public boolean check_device() throws Exception {
        var identifier = command_identifier_get();
        return Arrays.compare(identifier, new byte[]{'S', 'C', 'v', '2'}) == 0;
    }

    public void reset_state() throws Exception {
        command_state_reset();
    }

    public boolean check_firmware_version() throws Exception {
        var version = command_version_get();
        var major = ByteUtils.getShortFromBytesBE(version, 0);
        var minor = ByteUtils.getShortFromBytesBE(version, 2);
        var revision = ByteUtils.getIntFromBytes(version, 4, ByteOrder.BIG_ENDIAN);

        System.out.printf("Detected fw: %d.%d.%d%n", major, minor, revision);
        return major == SUPPORTED_MAJOR_VERSION && minor >= SUPPORTED_MINOR_VERSION;
    }
}
