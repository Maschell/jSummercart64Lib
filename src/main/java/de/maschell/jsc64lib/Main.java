package de.maschell.jsc64lib;

import com.fazecast.jSerialComm.*;

public class Main {
    public static void main(String[] args) throws Exception {
        SCWrapper sc64port = null;
        boolean found = false;
        for (var t : SerialPort.getCommPorts()) {
            if (t.getProductID() == 0x6014 && t.getVendorID() == 0x0403 && t.getPortDescription().equals("SC64")) {

                sc64port = new SCWrapper(t);
                found = true;
                break;
            }
        }
        if (!found) {
            System.out.println("No SC64 found");
            return;
        }

        sc64port.open();

        sc64port.reset();
        sc64port.reset_state();

        if (!sc64port.check_device()) {
            System.out.println("Check device failed!");
            return;
        }

        if (!sc64port.check_firmware_version()) {
            System.out.println("Check firmware version failed!");
            return;
        }

        int t = sc64port.command_config_get(SC64Defines.ConfigId.ButtonMode);

        System.out.println("ButtonMode: " + t);

/*
        var res = sc64port.get_sd_card_status();
        var res1 = sc64port.init_sd_card();
        var res2 = sc64port.deinit_sd_card();
 */
        sc64port.close();
    }
}