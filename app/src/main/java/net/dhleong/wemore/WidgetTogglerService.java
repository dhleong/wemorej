package net.dhleong.wemore;

import net.dhleong.wemore.Wemore.Device;

import rx.functions.Func1;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetTogglerService extends IntentService {

    public static final String EXTRA_ID = "widget_id";

    private static final String TAG = "wemore:WidgetTogglerService";
    private AppWidgetManager widgetMan;
    private MulticastHelper multicast;
    private Wemore wemore;

    public WidgetTogglerService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        widgetMan = AppWidgetManager.getInstance(this);
        multicast = MulticastHelper.acquire(this);
        wemore = new Wemore();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        multicast.release();
    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        final int widgetId = intent.getIntExtra(EXTRA_ID, 0);
        final String friendlyName = widgetMan.getAppWidgetOptions(widgetId)
            .getString(WidgetProvider.OPT_FRIENDLY_NAME);

        // set the widget to "loading" mode
        markWidgetLoading(widgetId);

        Log.d(TAG, "toggle " + friendlyName);
        final Device device = locateDevice(friendlyName);
        if (device == null) {
            // TODO toast?
            Log.w(TAG, "Couldn't find a device to toggle");
        } else {

            device.toggleBinaryState()
                .toBlocking()
                .first();

            Log.d(TAG, "Toggled: " + device);
        }

        // restore the widget state
        markWidgetDone(widgetId, friendlyName);

    }

    Wemore.Device locateDevice(final String friendlyName) {
        try {
            return wemore.search()
                .filter(new Func1<Device, Boolean>() {
                    @Override
                    public Boolean call(Device device) {
                        return friendlyName == null
                            || device.hasFriendlyNameLike(friendlyName);
                    }
                })
                .toBlocking()
                .firstOrDefault(null);
        } catch (final Exception e) {
            Log.w(TAG, "IOE searching for " + friendlyName, e);
            return null;
        }
    }


    void markWidgetLoading(final int widgetId) {
        widgetMan.updateAppWidget(widgetId,
                new RemoteViews(this.getPackageName(), R.layout.widget_loading));
    }

    void markWidgetDone(final int widgetId, final String friendlyName) {
        widgetMan.updateAppWidget(widgetId,
                WidgetProvider.buildRemoteView(this, widgetId));
    }

}
