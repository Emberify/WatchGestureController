package emberify.com.watchgesturecontroller.utils;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

/**
 * Created by gautam on 5/4/16.
 */
public class CommanCode {

    public static void deleteDataItems(final GoogleApiClient mGoogleApiClient) {
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
                                    Wearable.DataApi.deleteDataItems(mGoogleApiClient, dataItemUri)
                                            .setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                                                @Override
                                                public void onResult(DataApi.DeleteDataItemsResult deleteResult) {
                                                    if (deleteResult.getStatus().isSuccess()) {
                                                        Log.e("Successfully deleted:", "" + dataItemUri);
                                                    } else {
                                                        Log.e("Failed to delete", "" + dataItemUri);
                                                    }
                                                }
                                            });
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
}
