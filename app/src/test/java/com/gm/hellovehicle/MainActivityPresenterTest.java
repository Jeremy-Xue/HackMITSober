package com.gm.hellovehicle;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;

import com.gm.android.vehicle.hardware.RotaryControlHelper;
import com.gm.android.vehicle.signals.ConnectionResult;
import com.gm.android.vehicle.signals.Permissions;
import com.gm.android.vehicle.signals.VehicleDataRequest;
import com.gm.android.vehicle.signals.VehicleDataResult;
import com.gm.android.vehicle.signals.VehicleManager;
import com.gm.android.vehicle.support.SupportDriveModeManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MainActivityPresenter.class, RotaryControlHelper.class, VehicleManager.class,
        SupportDriveModeManager.class, ContextCompat.class, Permissions.class})
public class MainActivityPresenterTest {
    @Mock
    Context mockContext;
    @Mock
    RotaryControlHelper.RotaryListener mockListener;
    @Mock
    SupportDriveModeManager mockSupportDriveModeManager;
    @Mock
    MainActivityPresenter.View mockView;
    @Mock
    VehicleManager mockVehicleManager;

    private MainActivityPresenter subject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(VehicleManager.class);
        PowerMockito.mockStatic(SupportDriveModeManager.class);

        when(SupportDriveModeManager.getInstance()).thenReturn(mockSupportDriveModeManager);
        when(VehicleManager.getInstance(mockContext)).thenReturn(mockVehicleManager);
        subject = new MainActivityPresenter(mockView, mockContext);
    }

    @Test
    public void didHandleEvent_should_return_true_on_RotaryClockwise() {
        KeyEvent mockKeyEvent = mock(KeyEvent.class);

        when(mockKeyEvent.getKeyCode()).thenReturn(10001);
        when(mockListener.onRotaryClockwise(10001)).thenReturn(true);

        assertEquals(true, subject.didHandleEvent(mockKeyEvent));
    }

    @Test
    public void didHandleEvent_should_return_true_on_RotaryCounterClockwise() {
        KeyEvent mockKeyEvent = mock(KeyEvent.class);

        when(mockKeyEvent.getKeyCode()).thenReturn(10000);
        when(mockListener.onRotaryCounterClockwise(10000)).thenReturn(true);

        assertEquals(true, subject.didHandleEvent(mockKeyEvent));
    }

    @Test
    public void didHandleEvent_should_return_true_on_MenuButtonPress() {
        KeyEvent mockKeyEvent = mock(KeyEvent.class);

        when(mockKeyEvent.getKeyCode()).thenReturn(66);
        when(mockListener.onMenuButtonClick()).thenReturn(true);

        assertEquals(true, subject.didHandleEvent(mockKeyEvent));
    }

    @Test
    public void didHandleEvent_should_return_false_on_any_other_key_press() {
        KeyEvent mockKeyEvent = mock(KeyEvent.class);

        when(mockKeyEvent.getKeyCode()).thenReturn(1);
        when(mockListener.onMenuButtonClick()).thenReturn(true);
        when(mockListener.onRotaryCounterClockwise(anyInt())).thenReturn(true);
        when(mockListener.onRotaryClockwise(anyInt())).thenReturn(true);

        assertEquals(false, subject.didHandleEvent(mockKeyEvent));
    }

    @Test
    public void onRotaryClockwise_should_invoke_mockViewRotaryClockwise_and_return_true() {
        assertEquals(true, subject.onRotaryClockwise(1));
        verify(mockView).rotaryClockwise();
    }

    @Test
    public void onRotaryCounterClockwise_should_invoke_mockViewRotaryCounterClockwise_and_return_true() {
        assertEquals(true, subject.onRotaryCounterClockwise(1));
        verify(mockView).rotaryCounterClockwise();
    }

    @Test
    public void onMenuButtonClick_should_invoke_rotaryButtonClick_and_return_true() {
        assertEquals(true, subject.onMenuButtonClick());
        verify(mockView).showToast("Menu Button Click!");
    }

    @Test
    public void vehicle_data_call_should_update_vin() throws Exception {
        VehicleDataRequest mockVehicleDataRequest = mock(VehicleDataRequest.class);
        VehicleDataResult mockVehicleDataResult = mock(VehicleDataResult.class);
        VehicleDataResult[] mockVehicleDataResults = new VehicleDataResult[]{mockVehicleDataResult};
        VehicleDataRequest.Builder mockBuilder = mock(VehicleDataRequest.Builder.class);


        PowerMockito.whenNew(VehicleDataRequest.Builder.class).withAnyArguments().thenReturn(mockBuilder);
        when(mockBuilder.addDataElement(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockVehicleDataRequest);
        when(mockVehicleManager.get(mockVehicleDataRequest)).thenReturn(mockVehicleDataResults);
        when(mockVehicleDataResult.getData()).thenReturn("11111");

        subject.onRequestPermissionsResult(1, new String[]{}, new int[]{}); // request code != 0

        verify(mockView).updateVin("11111");
    }

    @Test
    public void on_start_should_subscribe_SupportDriveModeManager_and_connect_VehicleManager() {
        subject.onStart();

        verify(mockSupportDriveModeManager).subscribe(subject);
        verify(mockVehicleManager).connect();
    }

    @Test
    public void on_stop_should_unsubscribe_SupportDriveModeManager_and_disconnect_VehicleManager() {
        subject.onStop();

        verify(mockSupportDriveModeManager).unsubscribe(subject);
        verify(mockVehicleManager).disconnect();
    }

    @Test
    public void checkPermissions_method_should_check_for_appropriate_permissions_if_not_already_granted() {
        PowerMockito.mockStatic(ContextCompat.class);
        PowerMockito.mockStatic(Permissions.class);

        when(Permissions.getPermission(anyInt())).thenReturn("PERMISSION");
        when(ContextCompat.checkSelfPermission(mockContext, "PERMISSION")).thenReturn(1);

        subject.onConnect();

        verify(mockView).requestPermissions(0);
    }

    @Test
    public void checkPermissions_method_should_getVehicleData_if_permissions_are_already_granted() throws Exception {
        PowerMockito.mockStatic(ContextCompat.class);
        PowerMockito.mockStatic(Permissions.class);

        VehicleDataRequest mockVehicleDataRequest = mock(VehicleDataRequest.class);
        VehicleDataResult mockVehicleDataResult = mock(VehicleDataResult.class);
        VehicleDataResult[] mockVehicleDataResults = new VehicleDataResult[]{mockVehicleDataResult};
        VehicleDataRequest.Builder mockBuilder = mock(VehicleDataRequest.Builder.class);

        PowerMockito.whenNew(VehicleDataRequest.Builder.class).withAnyArguments().thenReturn(mockBuilder);
        when(Permissions.getPermission(anyInt())).thenReturn("PERMISSION");
        when(ContextCompat.checkSelfPermission(mockContext, "PERMISSION")).thenReturn(0);
        when(mockBuilder.addDataElement(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockVehicleDataRequest);
        when(mockVehicleManager.get(mockVehicleDataRequest)).thenReturn(mockVehicleDataResults);
        when(mockVehicleDataResult.getData()).thenReturn("11111");

        subject.onConnect();

        verify(mockVehicleManager).subscribe(mockVehicleDataRequest, subject);
    }

    @Test
    public void drive_mode_change_should_call_updateDriveMode_with_current_mode() {

        subject.onDrivingModeChange(SupportDriveModeManager.MODE_PARKED, anyInt());
        subject.onDrivingModeChange(SupportDriveModeManager.MODE_PCM, anyInt());
        subject.onDrivingModeChange(SupportDriveModeManager.MODE_LOW_SPEED, anyInt());
        subject.onDrivingModeChange(SupportDriveModeManager.MODE_HIGH_SPEED, anyInt());
        subject.onDrivingModeChange(SupportDriveModeManager.MODE_TEEN, anyInt());
        subject.onDrivingModeChange(SupportDriveModeManager.MODE_UNKNOWN, anyInt());

        verify(mockView).updateDriveMode("PARKED");
        verify(mockView).updateDriveMode("PASSENGER CONTROL MODE");
        verify(mockView).updateDriveMode("LOW SPEED");
        verify(mockView).updateDriveMode("HIGH SPEED");
        verify(mockView).updateDriveMode("TEEN DRIVER");
        verify(mockView).updateDriveMode("UNKNOWN");

    }

    @Test
    public void error_check_should_show_toast_if_permissions_were_not_granted() {
        int[] result = new int[]{-1};
        String[] permissions = new String[]{"PERMISSION"};

        subject.onRequestPermissionsResult(0, permissions, result);

        verify(mockView).showToast("Oh no! You need vehicle permissions :(");
    }

    @Test
    public void onConnectionFailed_to_IConnectionListener_should_show_toast_message() {
        subject.onConnectionFailed(new ConnectionResult(0, "Err"));
        verify(mockView).showToast("Connection Failed: Err");
    }

    @Test
    public void onConnectionSuspended_to_IConnectionListener_should_show_toast_message() {
        subject.onConnectionSuspended();
        verify(mockView).showToast("Connection Suspended");
    }

    @Test
    public void vehicle_speed_should_update_when_onDataReceived_from_IDataListener() throws Exception {
        VehicleDataRequest mockVehicleDataRequest = mock(VehicleDataRequest.class);
        VehicleDataResult mockVehicleDataResult = mock(VehicleDataResult.class);
        VehicleDataResult[] mockVehicleDataResults = new VehicleDataResult[]{mockVehicleDataResult};
        VehicleDataRequest.Builder mockBuilder = mock(VehicleDataRequest.Builder.class);


        PowerMockito.whenNew(VehicleDataRequest.Builder.class).withAnyArguments().thenReturn(mockBuilder);
        when(mockBuilder.addDataElement(anyInt())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockVehicleDataRequest);
        when(mockVehicleManager.get(mockVehicleDataRequest)).thenReturn(mockVehicleDataResults);
        when(mockVehicleDataResult.getData()).thenReturn("70");

        subject.onDataReceived(mockVehicleDataResult);

        verify(mockView).updateSpeed("70");
    }
}