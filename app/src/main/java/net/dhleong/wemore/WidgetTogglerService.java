package net.dhleong.wemore;

import net.dhleong.wemore.Wemore.Device;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetTogglerService extends IntentService {

    public static final String EXTRA_ID = "widget_id";
    public static final String EXTRA_FRIENDLY = "widget_friendly_name";

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
        final String friendlyName = intent.getStringExtra(EXTRA_FRIENDLY);

        // set the widget to "loading" mode
        markWidgetLoading(widgetId);

        Log.d(TAG, "toggle " + intent);
        final Device device = wemore.search()
            // TODO Filter on friendlyName
            .toBlocking()
            .firstOrDefault(null);

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

    void markWidgetLoading(final int widgetId) {
        widgetMan.updateAppWidget(widgetId,
                new RemoteViews(this.getPackageName(), R.layout.widget_loading));
    }

    void markWidgetDone(final int widgetId, final String friendlyName) {
        widgetMan.updateAppWidget(widgetId,
                WidgetProvider.buildRemoteView(this, widgetId, friendlyName));
    }
}
