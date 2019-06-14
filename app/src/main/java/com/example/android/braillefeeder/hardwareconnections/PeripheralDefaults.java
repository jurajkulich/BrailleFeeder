package com.example.android.braillefeeder.hardwareconnections;

public class PeripheralDefaults {

    static String getClockGpioPin() {
        return "GPIO2_IO01";
    }
    static String getLatchGpioPin() {
        return "GPIO6_IO12";
    }
    static String getDataGpioPin() {
        return "GPIO6_IO13";
    }

    static String getFirstSolenoidGpioPin() {
        return "GPIO6_IO12";
    }

    static String getSecondSolenoidGpioPin() {
        return "GPIO1_IO10";
    }

    static String getThirdSolenoidGpioPin() {
        return "GPIO6_IO13";
    }

    static String getFourthSolenoidGpioPin() {
        return "GPIO2_IO05";
    }

    static String getFifthSolenoidGpioPin() {
        return "GPIO2_IO01";
    }

    static String getSixthSolenoidGpioPin() {
        return "GPIO2_IO02";
    }

    static String getSwitchButtonGpioPin() {
        return "GPIO6_IO14";
    }

    static String getVolumeButtonGpioPin() {
        return "GPIO6_IO15";
    }

    static String getLengthButtonGpioPin() {
        return "GPIO2_IO07";
    }
}
