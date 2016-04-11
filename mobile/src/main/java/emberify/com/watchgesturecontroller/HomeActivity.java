package emberify.com.watchgesturecontroller;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Random;

import emberify.com.watchgesturecontroller.cardboard.CardBoardMainActivity;
import emberify.com.watchgesturecontroller.utils.SharedPreferencesUtils;
import emberify.com.watchgesturecontroller.watch.GameActivity;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;
    SharedPreferencesUtils spu = new SharedPreferencesUtils();
    Boolean isConnectedToApp = false;
    AppCompatButton btnSpaceGestureGame, btnVrGesture, btnHelp;
    TableRow trConnectWear;
    TextView txtConnectWear, txtAndroidExperiments;
    ImageView imgConnectWear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_home);

        if (!spu.fechSharedPreferenesBoolean(HomeActivity.this, SharedPreferencesUtils.PREF_OONBOARDING_COMPLETED, false)) {
            Intent intent = new Intent(HomeActivity.this, OnboardingActivity.class);
            startActivity(intent);
            finish();
        }
        btnSpaceGestureGame = (AppCompatButton) findViewById(R.id.btn_space_gesture_game);
        btnVrGesture = (AppCompatButton) findViewById(R.id.btn_vr_gesture);
        btnHelp = (AppCompatButton) findViewById(R.id.btn_help);
        trConnectWear = (TableRow) findViewById(R.id.tr_connect_wear);
        imgConnectWear = (ImageView) findViewById(R.id.img_connect_wear);
        txtConnectWear = (TextView) findViewById(R.id.txt_connect_wear);
        txtAndroidExperiments = (TextView) findViewById(R.id.txt_android_experiments);

        btnSpaceGestureGame.setOnClickListener(this);
        btnVrGesture.setOnClickListener(this);
        btnHelp.setOnClickListener(this);
        trConnectWear.setOnClickListener(this);
        txtAndroidExperiments.setOnClickListener(this);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btn_space_gesture_game:
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && isConnectedToApp) {
                    intent = new Intent(HomeActivity.this, GameActivity.class);
                    startActivity(intent);
                    finish();
                }
                break;

            case R.id.btn_vr_gesture:
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected() && isConnectedToApp) {
                    intent = new Intent(HomeActivity.this, CardBoardMainActivity.class);
                    startActivity(intent);
                    finish();
                }
                break;

            case R.id.btn_help:
                intent = new Intent(HomeActivity.this, OnboardingActivity.class);
                startActivity(intent);
                break;

            case R.id.txt_android_experiments:
                String id = "Emberify+-+Instant";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri
                            .parse("market://developer?id=" + id)));
                } catch (android.content.ActivityNotFoundException e) {
                    e.printStackTrace();
                    startActivity(new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/developer?id=" + id)));
                }

                break;
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        new CheckWearableConnected().execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
        txtConnectWear.setText(getResources().getString(R.string.txt_connect_your_wear));
        imgConnectWear.setImageResource(R.mipmap.watch_disconnect);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        txtConnectWear.setText(getResources().getString(R.string.txt_connect_your_wear));
        imgConnectWear.setImageResource(R.mipmap.watch_disconnect);
    }

    private class CheckWearableConnected extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            NodeApi.GetConnectedNodesResult nodes =
                    Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

            if (nodes != null && nodes.getNodes().size() > 0) {
                isConnectedToApp = true;
            } else {
                isConnectedToApp = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (isConnectedToApp) {
                txtConnectWear.setText(getResources().getString(R.string.txt_connected_your_wear));
                imgConnectWear.setImageResource(R.mipmap.watch_connect);
            } else {
                txtConnectWear.setText(getResources().getString(R.string.txt_connect_your_wear));
                imgConnectWear.setImageResource(R.mipmap.watch_disconnect);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }
}
