package net.dhleong.wemore;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {

    static final String TAG = "wemore:WidgetProvider";
    static final String EXTRA_WIDGET_IDS = "widget_ids";

    static final String OPT_FRIENDLY_NAME = "friendly";

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
            final int[] appWidgetIds) {
        Log.d(TAG, "onUpdate()");
        // To prevent any ANR timeouts, we perform the update in a service
        final Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds);
        context.startService(intent);
    }

    public static class UpdateService extends IntentService {

        public UpdateService() {
            super(TAG);
        }

        @Override
        public void onHandleIntent(final Intent intent) {
            Log.d(TAG, "onHandleIntent()");

            final int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);

            for (final int id : widgetIds) {

                // Build the widget update
                final RemoteViews updateViews = buildRemoteView(this, id);
                Log.d(TAG, "update built");

                // Push update for this widget to the home screen
                final AppWidgetManager manager = AppWidgetManager.getInstance(this);
                manager.updateAppWidget(id, updateViews);
                Log.d(TAG, "widget updated");
            }
        }


    }

    public static RemoteViews buildRemoteView(final Context context, final int widgetId) {
        // TODO listen for wifi state change, probably...
        final int layout;
        if (!isOnWifi(context)) {
            layout = R.layout.widget_disabled;
        } else {
            layout = R.layout.widget_enabled;
        }

        final Intent intent = new Intent(context, WidgetTogglerService.class);
        intent.putExtra(WidgetTogglerService.EXTRA_ID, widgetId);
        final RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        views.setOnClickPendingIntent(R.id.icon,
                PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        return views;
    }

    private static boolean isOnWifi(final Context context) {

        final ConnectivityManager conn = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        for (final NetworkInfo network : conn.getAllNetworkInfo()) {
            if (network.isConnectedOrConnecting() 
                    && network.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }

        return false;
    }
}
