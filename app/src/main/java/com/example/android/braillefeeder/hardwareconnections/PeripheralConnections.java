package com.example.android.braillefeeder.hardwareconnections;

import android.util.Log;

import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.things.pio.Gpio.ACTIVE_HIGH;
import static com.google.android.things.pio.Gpio.DIRECTION_OUT_INITIALLY_LOW;

/**
 * Created by juraj on 9/21/18.
 */

public class PeripheralConnections {

    private Gpio latchPin;
    private Gpio clockPin;
    private Gpio dataPin;

    private PeripheralManager peripheralManager;

    public PeripheralConnections(PeripheralManager peripheralManagerService) {
        this.peripheralManager = peripheralManagerService;
        Log.d("PERIHPHERAl", "Available GPIO: " +this.peripheralManager.getGpioList());
        try {
            latchPin = peripheralManager.openGpio(PeripheralDefaults.getLatchGpioPin());
            latchPin.setDirection(DIRECTION_OUT_INITIALLY_LOW);
            clockPin = peripheralManager.openGpio(PeripheralDefaults.getClockGpioPin());
            clockPin.setDirection(DIRECTION_OUT_INITIALLY_LOW);
            dataPin = peripheralManager.openGpio(PeripheralDefaults.getDataGpioPin());
            dataPin.setDirection(DIRECTION_OUT_INITIALLY_LOW);

            latchPin.setValue(false);
            clockPin.setValue(false);
            dataPin.setValue(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openSwitchButtonsGpio(Button.OnButtonEventListener buttonEventListener) {
        try {
            Button switchButton = new Button(PeripheralDefaults.getSwitchButtonGpioPin(),
                    Button.LogicState.PRESSED_WHEN_LOW);
            switchButton.setOnButtonEventListener(buttonEventListener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openLengthButtonsGpio(Button.OnButtonEventListener buttonEventListener) {
        try {
            Button lengthButton = new Button(PeripheralDefaults.getLengthButtonGpioPin(),
                    Button.LogicState.PRESSED_WHEN_LOW);
            lengthButton.setOnButtonEventListener(buttonEventListener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openVolumeButtonsGpio(Button.OnButtonEventListener buttonEventListener) {
        try {
            Button volumeButton = new Button(PeripheralDefaults.getVolumeButtonGpioPin(),
                    Button.LogicState.PRESSED_WHEN_LOW);
            volumeButton.setOnButtonEventListener(buttonEventListener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openSolenoidsGpio() {

    }

    private Gpio configureGpioPin(String pin) {
        Gpio gpioPin = null;
        try {
            gpioPin = peripheralManager.openGpio(pin);
            gpioPin.setDirection(DIRECTION_OUT_INITIALLY_LOW);
            gpioPin.setActiveType(ACTIVE_HIGH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gpioPin;
    }

    public void sendGpioValues(String sequence) {
        Log.d("sendGpio", sequence);
        try {
            latchPin.setValue(false);
            shiftOut(sequence);
            latchPin.setValue(true);
        } catch (IOException o) {
            o.printStackTrace();
        }

    }

    void shiftOut(String sequence) {
        Log.d("TAG", sequence);
        try {
            for (int i = 0; i < 8; i++) {
                if( i < 2) {
                    clockPin.setValue(true);
                    clockPin.setValue(false);
                } else {
                    clockPin.setValue(false);
                    dataPin.setValue((sequence.charAt(i-2) - '0') == 1);
                    clockPin.setValue(true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
