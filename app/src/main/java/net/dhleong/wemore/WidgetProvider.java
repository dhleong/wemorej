package net.dhleong.wemore;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetProvider extends AppWidgetProvider {

    static final String TAG = "wemore:WidgetProvider";
    static final String EXTRA_WIDGET_IDS = "widget_ids";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        Log.d(TAG, "onUpdate()");
        // To prevent any ANR timeouts, we perform the update in a service
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds);
        context.startService(intent);
    }

    public static class UpdateService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onStart(final Intent intent, final int startId) {
            Log.d(TAG, "onStart()");

            final int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);

            for (final int id : widgetIds) {

                // Build the widget update
                // TODO get the friendlyName
                RemoteViews updateViews = buildRemoteView(this, id, null);
                Log.d(TAG, "update built");

                // Push update for this widget to the home screen
                // TODO we actually need to respect the widget id
                //  and fetch the friendlyName, etc.
                ComponentName thisWidget = new ComponentName(this,
                        WidgetProvider.class);
                AppWidgetManager manager = AppWidgetManager.getInstance(this);
                manager.updateAppWidget(thisWidget, updateViews);
                Log.d(TAG, "widget updated");
            }
        }


    }

    public static RemoteViews buildRemoteView(final Context context, final int widgetId, final String friendlyName) {
        // TODO listen for wifi state change, probably...
        final int layout;
        if (!isOnWifi(context)) {
            layout = R.layout.widget_disabled;
        } else {
            layout = R.layout.widget_enabled;
        }

        final Intent intent = new Intent(context, WidgetTogglerService.class);
        intent.putExtra(WidgetTogglerService.EXTRA_ID, widgetId);
        intent.putExtra(WidgetTogglerService.EXTRA_FRIENDLY, friendlyName);
        final RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        views.setOnClickPendingIntent(R.id.icon,
                PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));

        return views;
    }

    private static boolean isOnWifi(final Context context) {

        ConnectivityManager conn = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        for (NetworkInfo network : conn.getAllNetworkInfo()) {
            if (network.isConnectedOrConnecting() 
                    && network.getType() == ConnectivityManager.TYPE_WIFI) {
                return true;
            }
        }

        return false;
    }
}
