package emberify.com.watchgesturecontroller;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

public class GameActivity extends WearableActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private float SHAKE_THRESHOLD = 0f;
    private int SHAKE_WAIT_TIME_MS = 5;
    private long mShakeTime = 0;

    float[] history = new float[2];
    TextView txtUp, txtDown, txtLeft, txtRight;
    GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_game_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                txtUp = (TextView) stub.findViewById(R.id.txt_up);
                txtDown = (TextView) stub.findViewById(R.id.txt_down);
                txtLeft = (TextView) stub.findViewById(R.id.txt_left);
                txtRight = (TextView) stub.findViewById(R.id.txt_right);

                changeMovementState(0);

                txtUp.setRotation(90);
                txtDown.setRotation(90);
                txtLeft.setRotation(90);
                txtRight.setRotation(90);
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    private void changeMovementState(int no) {
        switch (no) {
            case 0:
                txtUp.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtDown.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtLeft.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtRight.setTextColor(getResources().getColor(R.color.txt_disable_color));
                break;
            case 1:
                txtUp.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtDown.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtLeft.setTextColor(getResources().getColor(R.color.white_color));
                txtRight.setTextColor(getResources().getColor(R.color.txt_disable_color));
                break;
            case 2:
                txtUp.setTextColor(getResources().getColor(R.color.white_color));
                txtDown.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtLeft.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtRight.setTextColor(getResources().getColor(R.color.txt_disable_color));
                break;
            case 3:
                txtUp.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtDown.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtLeft.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtRight.setTextColor(getResources().getColor(R.color.white_color));
                break;
            case 4:
                txtUp.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtDown.setTextColor(getResources().getColor(R.color.white_color));
                txtLeft.setTextColor(getResources().getColor(R.color.txt_disable_color));
                txtRight.setTextColor(getResources().getColor(R.color.txt_disable_color));
                break;

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            deleteDataItems();
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onConnected(Bundle bundle) {
        deleteDataItems();
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        Random rndNumbers = new Random();
        int rndNo = rndNumbers.nextInt(10) + 1;
        putDataMapReq.getDataMap().putString("start_game_on_device", "start_game_on_device" + rndNo);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
                    mSensorManager.registerListener(GameActivity.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    Log.d("Data", "Data not set: " + result.getDataItem().getUri());
                }
            }
        });

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.i("onConnectionFailed", "Connection failed. Cause: "
                + result.toString());
    }

    private void deleteDataItems() {
        if (mGoogleApiClient.isConnected()) {
            Wearable.DataApi.getDataItems(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DataItemBuffer>() {
                        @Override
                        public void onResult(DataItemBuffer result) {
                            if (result.getStatus().isSuccess()) {
                                // Store the DataItem URIs in a List and close the buffer. Then use these URIs
                                // to delete the DataItems.
                                final List<DataItem> dataItemList = FreezableUtils.freezeIterable(result);
                                result.close();
                                for (final DataItem dataItem : dataItemList) {
                                    final Uri dataItemUri = dataItem.getUri();
                                    // In a real calendar application, this might delete the corresponding calendar
                                    // event from the calendar data provider. In this sample, we simply delete the
                                    // DataItem, but leave the phone's calendar data intact.
                                    Wearable.DataApi.deleteDataItems(mGoogleApiClient, dataItemUri);
                                }
                            } else {
                                Log.d("Del", "onDeleteEventsClicked(): failed to get Data Items");
                            }
                        }
                    });
        } else {
            Log.e("Failed", "Failed to delete data items"
                    + " - Client disconnected from Google Play Services");
        }
    }

    private void sendSensorDataToDevice(String sensorType, float x, float y, float z) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        putDataMapReq.getDataMap().putString("sensor_type", sensorType);
        putDataMapReq.getDataMap().putFloat("x_axis", x);// round(x, 2));
        putDataMapReq.getDataMap().putFloat("y_axis", y);// round(y, 2));
        putDataMapReq.getDataMap().putFloat("z_axis", z);//round(z, 2));

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
//                    Log.d("Data", "Data item set: " + result.getDataItem().getUri());
                } else {
                    Log.d("Data", "Data not set: " + result.getDataItem().getUri());
                }
            }
        });
    }

    public float round(float d, int decimalPlace) {
        float value;
        BigDecimal bd = new BigDecimal(Float.toString(d));
        value = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP).floatValue();
        return value;
    }


    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();
        if ((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;
            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;//use in future for 3d reality

            // gForce will be close to 1 when there is no movement
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > SHAKE_THRESHOLD) {

                if (history[0] > event.values[0] && Math.abs(event.values[0]) > 1) {
                    changeMovementState(2);
                } else if (history[0] < event.values[0] && Math.abs(event.values[0]) > 1) {
                    changeMovementState(4);
                } else if (history[1] > event.values[1] && Math.abs(event.values[1]) > 1) {
                    changeMovementState(1);
                } else if (history[1] < event.values[1] && Math.abs(event.values[1]) > 1) {
                    changeMovementState(3);
                }
                history[0] = event.values[0];
                history[1] = event.values[1];

                sendSensorDataToDevice(mSensor.getStringType(), event.values[0], event.values[1], event.values[2]);

            }
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/data") == 0) {

                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if (dataMap.containsKey("home_on_wear")) {
                        if (dataMap.getString("home_on_wear").contains("home_on_wear")) {
                            Intent intent = new Intent(GameActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
                Log.e("DataItem", "deleted");

            }
        }
    }
}