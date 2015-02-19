package net.dhleong.wemore;

import net.dhleong.wemore.Wemore.Device;

import rx.Observable;
import rx.functions.Func1;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class WidgetTogglerService extends IntentService {

    private static final String TAG = "wemore:WidgetTogglerService";
    private MulticastHelper multicast;
    private Wemore wemore;

    public WidgetTogglerService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        multicast = MulticastHelper.acquire(this);
        wemore = new Wemore();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        multicast.release();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "toggle " + intent);
        Device device = wemore.search()
            // TODO Filter on friendlyName
            .flatMap(new Func1<Device, Observable<Device>>() {
                @Override
                public Observable<Device> call(Device device) {
                    return device.toggleBinaryState();
                }
            })
            .toBlocking()
            .firstOrDefault(null);

        if (device == null) {
            Log.w(TAG, "Couldn't find a device to toggle");
        } else {
            Log.d(TAG, "Toggled: " + device);
        }
    }

}
