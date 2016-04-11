package emberify.com.watchgesturecontroller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.List;

public class CardboardActivity extends Activity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {


    private int SHAKE_WAIT_TIME_MS = 500;
    private long mShakeTime = 0;
    float[] history = new float[2];
    TextView txtLeft, txtRight;
    GoogleApiClient mGoogleApiClient;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cardboard);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        txtLeft = (TextView) findViewById(R.id.txt_cardboard_left);
        txtRight = (TextView) findViewById(R.id.txt_cardboard_right);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        txtLeft.setRotation(90);
        txtRight.setRotation(90);

        long now = System.currentTimeMillis();
        mShakeTime = now;
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
        mSensorManager.registerListener(CardboardActivity.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
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

    private void sendSensorDataToDevice(float direction) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        putDataMapReq.getDataMap().putFloat("rotation_direction", direction);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);

    }


    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();
        if ((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;
            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;//use in future for 3d reality

            if (history[1] > event.values[1] && Math.abs(event.values[1]) > 7) {
                sendSensorDataToDevice(event.values[1]);
            } else if (history[1] < event.values[1] && Math.abs(event.values[1]) > 4) {
                sendSensorDataToDevice(event.values[1]);
            }
            history[0] = event.values[0];
            history[1] = event.values[1];
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
                            Intent intent = new Intent(CardboardActivity.this, HomeActivity.class);
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
