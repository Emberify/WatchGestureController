package emberify.com.watchgesturecontroller.watch;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import emberify.com.watchgesturecontroller.HomeActivity;
import emberify.com.watchgesturecontroller.R;
import emberify.com.watchgesturecontroller.utils.CommanCode;

public class GameActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {


    GoogleApiClient mGoogleApiClient;
    ImageView imgPointer;
    TextView txtScore;
    LinearLayout lyGameBoard;
    RelativeLayout rlMain;
    float dX, dY, startX = 0, startY = 0;
    int pDHeightMax = 0, pDWidthMax = 0;
    int imgPointerHeight, imgPointerWidth;
    int imgCapsulHeight, imgCapsulWidth;
    ImageView imageCapsul;
    TimerTask timerTask;
    Handler handlerTimer = new Handler();
    Handler handler = new Handler();
    Runnable runnable;
    Timer timer;
    int scoreCounter = 0, timerCounter = 0;
    List<ImageView> capsulList;
    boolean stopGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_game);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        imgPointer = (ImageView) findViewById(R.id.img_pointer);
        txtScore = (TextView) findViewById(R.id.txt_score);
        lyGameBoard = (LinearLayout) findViewById(R.id.ly_game_board);
        rlMain = (RelativeLayout) findViewById(R.id.rl_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        txtScore.setText(reSetScore());
    }

    private void StartGame() {
        capsulList = new ArrayList<>();

        imageCapsul = new ImageView(this);
        imageCapsul.setLayoutParams(new android.view.ViewGroup.LayoutParams(80, 80));
        imageCapsul.setImageResource(R.mipmap.ic_capsul);
        imgCapsulHeight = 80;//image.getMeasuredHeight();
        imgCapsulWidth = 80;//image.getMeasuredWidth();
        stopGame = true;

        runnable = new Runnable() {
            public void run() {
                final ImageView imageCapsul = new ImageView(GameActivity.this);
                imageCapsul.setLayoutParams(new android.view.ViewGroup.LayoutParams(80, 80));

                Random rand = new Random();
                int randInt = rand.nextInt(pDWidthMax);
                if ((pDWidthMax + imgCapsulWidth) < randInt) {
                    randInt = pDWidthMax - (pDWidthMax - randInt);
                }

                imageCapsul.setImageResource(R.mipmap.ic_capsul);
                imageCapsul.setX(randInt);
                imageCapsul.setY(-imgCapsulHeight);
                rlMain.addView(imageCapsul);
                capsulList.add(imageCapsul);

                imageCapsul.animate()
                        .x(randInt)
                        .y(-imgCapsulHeight)
                        .translationX(randInt)
                        .translationY(pDHeightMax + imgCapsulHeight)
                        .setDuration(7000)
                        .start();
                handler.postDelayed(this, 1400);
            }
        };
        handler.postDelayed(runnable, 0);


        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                handlerTimer.post(new Runnable() {
                    public void run() {
                        timerCounter += 10;
                        if (timerCounter % 1000 == 0) {
                            scoreCounter++;
                            txtScore.setText(String.valueOf(scoreCounter));
                        }
                        imgPointer.buildDrawingCache();
                        Bitmap bmapPointer = imgPointer.getDrawingCache();
                        for (ImageView image : capsulList) {
                            image.buildDrawingCache();
                            if (image.getDrawingCache() != null) {
                                Bitmap bmapCapsul = image.getDrawingCache();
                                if (isCollisionDetected(bmapPointer, (int) imgPointer.getX(), (int) imgPointer.getY(),
                                        bmapCapsul, (int) image.getX(), (int) image.getY())) {
                                    stopGame = false;
                                    timer.cancel();
                                    handler.removeCallbacks(runnable);
                                    break;
                                }
                            } else {
                                capsulList.remove(image);
                                rlMain.removeView(image);
                            }
                        }
                        if (!stopGame && capsulList.size() > 0) {
                            for (ImageView image : capsulList) {
                                image.animate().cancel();
                                rlMain.removeView(image);
                            }
                            capsulList.clear();
                            showRestartGameDialog();
                        }
                    }
                });
            }
        };
        timer.schedule(timerTask, 10, 10);
    }

    private void showRestartGameDialog() {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(getResources().getString(R.string.dialog_score) + " " + scoreCounter);
        builder.setNegativeButton(getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onBackPressed();
            }
        });
        builder.setPositiveButton(getResources().getString(R.string.dialog_restart_game), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                stopGame = true;
                imgPointer.clearAnimation();
                txtScore.setText(reSetScore());
                imgPointer.animate()
                        .x(startX)
                        .y(startY)
                        .setDuration(0)
                        .start();
                StartGame();
            }
        });
        builder.show();
    }


    private String reSetScore() {
        scoreCounter = 0;
        return "0";
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        stopGame = false;
        if (timer != null)
            timer.cancel();
        if (handler != null)
            handler.removeCallbacks(runnable);
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
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("Connection", "Connected......");
        CommanCode.deleteDataItems(mGoogleApiClient);

        imgPointerWidth = imgPointer.getMeasuredWidth();
        imgPointerHeight = imgPointer.getMeasuredHeight();

        pDWidthMax = lyGameBoard.getMeasuredWidth();
        pDHeightMax = lyGameBoard.getMeasuredHeight();
        Wearable.DataApi.addListener(mGoogleApiClient, this);


        sendDataToWearStratGame();
    }

    private void sendDataToWearStratGame() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/data");
        Random rndNumbers = new Random();
        int rndNo = rndNumbers.nextInt(10) + 1;
        putDataMapReq.getDataMap().putString("start_game_on_wear", "start_game_on_wear" + rndNo);

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

                    dX = 0;
                    dY = 0;

                    final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    if (dataMap.containsKey("start_game_on_device")) {
                        if (dataMap.getString("start_game_on_device").contains("start_game_on_device")) {
                            CommanCode.deleteDataItems(mGoogleApiClient);
                            if (startX == 0 && startY == 0) {
                                startX = imgPointer.getX();
                                startY = imgPointer.getY();
                            }
                            imgPointer.clearAnimation();
                            txtScore.setText(reSetScore());
                            imgPointer.animate()
                                    .x(startX)
                                    .y(startY)
                                    .setDuration(0)
                                    .start();
                            StartGame();
                        }
                    } else {
                        if (stopGame) {
                            dX = dataMap.getFloat("x_axis");
                            dY = dataMap.getFloat("y_axis");
//                        z = dataMap.getFloat("z_axis");////use in future for 3d reality

                            float finalX = (float) (dX * -1.5);
                            float finalY = (float) (dY * 1.5);

                            float finalpX = imgPointer.getX() + finalY;
                            float finalpY = imgPointer.getY() + finalX;

                            float screenBoundX = finalpX + imgPointerWidth;
                            float screenBoundY = finalpY + imgPointerHeight;

                            if (pDWidthMax > screenBoundX && imgPointerHeight < screenBoundX && pDHeightMax > screenBoundY && imgPointerHeight < screenBoundY) {
                                imgPointer.animate()
                                        .x(finalpX)
                                        .y(finalpY)
                                        .setDuration(0)
                                        .start();
                            }
                        }
                    }
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
                Log.e("DataItem", "deleted");

            }
        }
    }

    public static boolean isCollisionDetected(Bitmap bitmap1, int x1, int y1,
                                              Bitmap bitmap2, int x2, int y2) {

        Rect bounds1 = new Rect(x1, y1, x1 + bitmap1.getWidth(), y1 + bitmap1.getHeight());
        Rect bounds2 = new Rect(x2, y2, x2 + bitmap2.getWidth(), y2 + bitmap2.getHeight());

        if (Rect.intersects(bounds1, bounds2)) {
            Rect collisionBounds = getCollisionBounds(bounds1, bounds2);
            for (int i = collisionBounds.left; i < collisionBounds.right; i++) {
                for (int j = collisionBounds.top; j < collisionBounds.bottom; j++) {
                    int bitmap1Pixel = bitmap1.getPixel(i - x1, j - y1);
                    int bitmap2Pixel = bitmap2.getPixel(i - x2, j - y2);
                    if (isFilled(bitmap1Pixel) && isFilled(bitmap2Pixel)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Rect getCollisionBounds(Rect rect1, Rect rect2) {
        int left = (int) Math.max(rect1.left, rect2.left);
        int top = (int) Math.max(rect1.top, rect2.top);
        int right = (int) Math.min(rect1.right, rect2.right);
        int bottom = (int) Math.min(rect1.bottom, rect2.bottom);
        return new Rect(left, top, right, bottom);
    }

    private static boolean isFilled(int pixel) {
        return pixel != Color.TRANSPARENT;
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(GameActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
