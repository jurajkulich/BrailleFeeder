package com.example.android.braillefeeder;

public class PeripheralDefaults {
    // TODO - ADD CONSTANTS
    static String getFirstSolenoidGpioPin() {
        return "GPIO6_IO14";
    }

    static String getSecondsSolenoidGpioPin() {
        return "GPIO6_IO15";
    }

    static String getThirdSolenoidGpioPin() {
        return "GPIO2_IO07";
    }

    static String getFourthSolenoidGpioPin() {
        return "GPIO2_IO05";
    }

    static String getFifthSolenoidGpioPin() {
        return "GPIO2_IO00";
    }

    static String getSixthSolenoidGpioPin() {
        return "GPIO2_IO02";
    }

    static String getSwitchButtonGpioPin() {
        return "GPIO02_IO03";
    }

    static String getVolumeButtonGpioPin() {
        return "GPIO01_IO10";
    }

    static String getLengthButtonGpioPin() {
        return "GPIO06_IO13";
    }
}
