package emberify.com.watchgesturecontroller.cardboard;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.widgets.pano.VrPanoramaView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import emberify.com.watchgesturecontroller.HomeActivity;
import emberify.com.watchgesturecontroller.R;
import emberify.com.watchgesturecontroller.utils.CommanCode;

public class CardBoardMainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private GoogleApiClient mGoogleApiClient;
    private VrPanoramaView panoWidgetView;
    private ImageLoaderTask imageLoaderTask;
    final Handler handler = new Handler();
    Runnable runnable;
    int imagePositions = 0;
    float direction = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_board_main);

        panoWidgetView = (VrPanoramaView) findViewById(R.id.pano_view);
        if (imageLoaderTask != null) {
            imageLoaderTask.cancel(true);
        }
        imageLoaderTask = new ImageLoaderTask();
        imageLoaderTask.execute();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    class ImageLoaderTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            runnable = new Runnable() {
                public void run() {
                    VrPanoramaView.Options panoOptions = null;  // It's safe to use null VrPanoramaView.Options.
                    InputStream istr = null;
                    AssetManager assetManager = getAssets();

                    if (direction > 0) {
                        imagePositions++;
                    } else {
                        imagePositions--;
                    }
                    if (imagePositions > 2) {
                        imagePositions = 0;
                    }
                    if (imagePositions < 0) {
                        imagePositions = 2;
                    }
                    try {
                        if (imagePositions == 0) {
                            istr = assetManager.open("a.jpg");
                        }
                        if (imagePositions == 1) {
                            istr = assetManager.open("b.jpg");
                        }
                        if (imagePositions == 2) {
                            istr = assetManager.open("c.jpg");
                        }

                        panoOptions = new VrPanoramaView.Options();
                        panoOptions.inputType = VrPanoramaView.Options.TYPE_MONO;

                        panoWidgetView.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);

                        if (istr != null)
                            istr.close();
                    } catch (IOException e) {
                        Log.e("CardBoardMainActivity", "Could not decode default bitmap: " + e);
                    } catch (OutOfMemoryError e) {
                        Log.e("CardBoardMainActivity", "Out of memory issue: " + e);
                    }
                }
            };
            handler.postDelayed(runnable, 0);
            return null;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            stopAndMoveHomeOnWear();
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    private void stopAndMoveHomeOnWear() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        Random rndNumbers = new Random();
        int rndNo = rndNumbers.nextInt(10) + 1;
        putDataMapReq.getDataMap().putString("home_on_wear", "home_on_wear" + rndNo);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
                    Log.d("Data game", "Data item set: " + result.getDataItem().getUri());
                } else {
                    Log.d("Data game", "Data not set: " + result.getDataItem().getUri());
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("Connection", "Connected......");
        CommanCode.deleteDataItems(mGoogleApiClient);
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        sendDataToWearOpenCardboard();
    }

    private void sendDataToWearOpenCardboard() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        Random rndNumbers = new Random();
        int rndNo = rndNumbers.nextInt(10) + 1;
        putDataMapReq.getDataMap().putString("open_cardboard", "open_cardboard" + rndNo);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if (result.getStatus().isSuccess()) {
                    Log.d("Data cardboard", "Data item set: " + result.getDataItem().getUri());
                } else {
                    Log.d("Data cardboard", "Data not set: " + result.getDataItem().getUri());
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("onConnectionSuspended", "onConnectionSuspended, " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("onConnectionFailed", "onConnectionFailed");
        connectionResult.getErrorMessage();
        if (null != Wearable.DataApi) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/data") == 0) {

                    final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    direction = dataMap.getFloat("rotation_direction");

                    if (imageLoaderTask != null) {
                        imageLoaderTask.cancel(true);
                    }
                    imageLoaderTask = new ImageLoaderTask();
                    imageLoaderTask.execute();


                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
                Log.e("DataItem", "deleted");

            }
        }
    }

    @Override
    protected void onPause() {
        panoWidgetView.pauseRendering();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        panoWidgetView.resumeRendering();
    }

    @Override
    protected void onDestroy() {
        // Destroy the widget and free memory.
        panoWidgetView.shutdown();
        if (imageLoaderTask != null) {
            imageLoaderTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(CardBoardMainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}

