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

    private List<Gpio> mSolenoids;
    private PeripheralManager peripheralManager;

    public PeripheralConnections(PeripheralManager peripheralManagerService) {
        this.peripheralManager = peripheralManagerService;
        Log.d("PERIHPHERAl", "Available GPIO: " +this.peripheralManager.getGpioList());
        mSolenoids = new ArrayList<>();
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
        mSolenoids.add(configureGpioPin(PeripheralDefaults.getFirstSolenoidGpioPin()));
        mSolenoids.add(configureGpioPin(PeripheralDefaults.getSecondSolenoidGpioPin()));
        mSolenoids.add(configureGpioPin(PeripheralDefaults.getThirdSolenoidGpioPin()));
        mSolenoids.add(configureGpioPin(PeripheralDefaults.getFourthSolenoidGpioPin()));
        mSolenoids.add(configureGpioPin(PeripheralDefaults.getFifthSolenoidGpioPin()));
        mSolenoids.add(configureGpioPin(PeripheralDefaults.getSixthSolenoidGpioPin()));
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
        for (int i = 0; i < mSolenoids.size(); i++) {
            try {
                mSolenoids.get(i).setValue((int) sequence.charAt(i) == '1');
            } catch (IOException error) {
                Log.e(error.getMessage(), "There was an error configuring GPIO pins");
            }
        }
    }
}
