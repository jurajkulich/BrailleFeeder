package com.example.android.braillefeeder;

import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

/**
 * Created by juraj on 9/21/18.
 */

public class PeripheralConnections {

    public interface PeripheralConnectionsListener {
        void onButtonClicked(boolean state);
    }

    private static final String buttonGPIO_TAG = "GPIO6_IO14";
    private static final String solenoidGPIO_TAG = "GPIO2_IO07";

    private PeripheralConnectionsListener mConnectionsListener;
    private Gpio mGpioButton;
    private Gpio mSolenoid;

    private Handler mHandler = new Handler();
    private boolean mLedState = false;

    public PeripheralConnections(PeripheralConnectionsListener listener) {
        mConnectionsListener = listener;
        accessGpio();
    }

    private void accessGpio() {
        try {
            PeripheralManager peripheralManager = PeripheralManager.getInstance();
            mGpioButton = peripheralManager.openGpio(buttonGPIO_TAG);
            mSolenoid = peripheralManager.openGpio(solenoidGPIO_TAG);

            mGpioButton.setDirection(Gpio.DIRECTION_IN);
            mGpioButton.setEdgeTriggerType(Gpio.EDGE_RISING);
            mGpioButton.registerGpioCallback(mGpioCallback);

            mSolenoid.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            mHandler.post(runnable);

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

        if( mSolenoid != null) {
            try {
                mSolenoid.close();
                mSolenoid = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private GpioCallback mGpioCallback = new GpioCallback() {

        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {

                    mConnectionsListener.onButtonClicked(true);

                } else {
                    mConnectionsListener.onButtonClicked(false);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Exit Runnable if the GPIO is already closed
            if (mSolenoid == null) {
                return;
            }
            try {
                // Toggle the GPIO state
                mLedState = !mLedState;
                mSolenoid.setValue(mLedState);

                // Reschedule the same runnable in {#INTERVAL_BETWEEN_BLINKS_MS} milliseconds
                mHandler.postDelayed(runnable, 3000);
                Log.e("Peripheral", String.valueOf(mLedState));
            } catch (IOException e) {
                Log.e("aaaa", "Error on PeripheralIO API", e);
            }
        }
    };
}
