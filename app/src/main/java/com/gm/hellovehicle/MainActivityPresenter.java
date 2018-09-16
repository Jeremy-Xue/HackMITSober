package com.gm.hellovehicle;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SparseArrayCompat;
import android.view.KeyEvent;

import com.gm.android.vehicle.hardware.RotaryControlHelper;
import com.gm.android.vehicle.signals.ConnectionResult;
import com.gm.android.vehicle.signals.IConnectionListener;
import com.gm.android.vehicle.signals.IVehicleDataListener;
import com.gm.android.vehicle.signals.Permissions;
import com.gm.android.vehicle.signals.VehicleDataRequest;
import com.gm.android.vehicle.signals.VehicleDataResult;
import com.gm.android.vehicle.signals.VehicleManager;
import com.gm.android.vehicle.signals.config.Config;
import com.gm.android.vehicle.signals.motion.Motion;
import com.gm.android.vehicle.support.SupportDriveModeManager;


public class MainActivityPresenter implements SupportDriveModeManager.OnDrivingModeChangeListener,
        IConnectionListener, IVehicleDataListener,
        RotaryControlHelper.RotaryListener {

    private final int STARTUP_PERMISSIONS = 0;

    private MainActivityPresenter.View mView;
    private SupportDriveModeManager mSupportDriveModeManager;
    private VehicleManager mVehicleManager;
    private Context mContext;
    private SparseArrayCompat<String> mDriveModes = new SparseArrayCompat<>();

    MainActivityPresenter(View view, Context context) {
        mView = view;
        mContext = context;
        mVehicleManager = VehicleManager.getInstance(context);
        mVehicleManager.registerConnectionListener(this);

        mSupportDriveModeManager = SupportDriveModeManager.getInstance();

        initDriveModeMap();
    }

    void onStart() {
        mVehicleManager.connect();
        mSupportDriveModeManager.subscribe(this);
    }

    void onStop() {
        mVehicleManager.disconnect();
        mSupportDriveModeManager.unsubscribe(this);
    }

    boolean didHandleEvent(KeyEvent event) {
        return RotaryControlHelper.onKeyEvent(event, this);
    }

    void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                    @NonNull int[] grantResults) {
        if (requestCode == STARTUP_PERMISSIONS) {
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    mView.showToast("Oh no! You need vehicle permissions :(");
                    return;
                }
            }
        }
        getVehicleData();
    }

    // Callback for IVehicleDataListener
    @Override
    public void onDataReceived(VehicleDataResult... vehicleDataResults) {
        mView.updateSpeed(vehicleDataResults[0].getData().toString());
    }

    // Callbacks for IConnectionListener
    @Override
    public void onConnect() {
        checkPermissions();
        mView.showToast("Connection Established");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mView.showToast("Connection Failed: " + connectionResult.getErrorMessage());
    }

    @Override
    public void onConnectionSuspended() {
        mView.showToast("Connection Suspended");
    }

    // Callback for OnDrivingModeChangeListener
    @Override
    public void onDrivingModeChange(int currentState, int previousState) {
        mView.updateDriveMode(mDriveModes.get(currentState));
    }

    // Callbacks for RotaryListener
    @Override
    public boolean onRotaryClockwise(int i) {
        mView.rotaryClockwise();
        return true;
    }

    @Override
    public boolean onRotaryCounterClockwise(int i) {
        mView.rotaryCounterClockwise();
        return true;
    }

    @Override
    public boolean onMenuButtonClick() {
        mView.showToast("Menu Button Click!");
        return true;
    }

    // VIN Request and Speed Request Subscription
    private void getVehicleData() {
        VehicleDataRequest vinRequest = new VehicleDataRequest.Builder()
                .addDataElement(Config.VIN)
                .build();

        VehicleDataRequest speedRequest = new VehicleDataRequest.Builder()
                .addDataElement(Motion.SPEED)
                .build();

        VehicleDataResult[] resultsArray = mVehicleManager.get(vinRequest);
        String data = resultsArray[0].getData().toString();
        mView.updateVin(data);
        mVehicleManager.subscribe(speedRequest, this);
    }

    // Permission Check for VIN and Speed
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(mContext, Permissions.getPermission(Config.VIN)) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(mContext, Permissions.getPermission(Motion.SPEED)) != PackageManager.PERMISSION_GRANTED) {
            mView.requestPermissions(STARTUP_PERMISSIONS);
        } else {
            getVehicleData();
        }
    }

    private void initDriveModeMap() {
        mDriveModes.append(SupportDriveModeManager.MODE_PARKED, "PARKED");
        mDriveModes.append(SupportDriveModeManager.MODE_PCM, "PASSENGER CONTROL MODE");
        mDriveModes.append(SupportDriveModeManager.MODE_LOW_SPEED, "LOW SPEED");
        mDriveModes.append(SupportDriveModeManager.MODE_HIGH_SPEED, "HIGH SPEED");
        mDriveModes.append(SupportDriveModeManager.MODE_TEEN, "TEEN DRIVER");
        mDriveModes.append(SupportDriveModeManager.MODE_UNKNOWN, "UNKNOWN");
    }

    // Interface for MainActivity
    interface View {
        void updateDriveMode(String state);

        void updateVin(String vin);

        void updateSpeed(String speed);

        void showToast(String msg);

        void requestPermissions(int requestCode);

        void rotaryClockwise();

        void rotaryCounterClockwise();
    }
}
