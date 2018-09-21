package com.example.android.braillefeeder;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

/**
 * Created by juraj on 9/21/18.
 */

public class PeripheralConnections {

    private static final String buttonGPIO_TAG = "GPIO6_IO14";

    private Gpio mGpioButton;

    public PeripheralConnections() {
        accessGpio();
    }

    private void accessGpio() {
        try {
            PeripheralManager peripheralManager = PeripheralManager.getInstance();
            mGpioButton = peripheralManager.openGpio(buttonGPIO_TAG);
            mGpioButton.setDirection(Gpio.DIRECTION_IN);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeGpio() {
        if( mGpioButton != null) {
            try {
                mGpioButton.close();
                mGpioButton = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /*
    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {

            if( gpio.getValue()) {

            } else {

            }

            return true;
        }

    }

    */
}
