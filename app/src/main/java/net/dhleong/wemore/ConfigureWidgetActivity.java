package net.dhleong.wemore;

import java.util.ArrayList;
import java.util.List;

import net.dhleong.wemore.Wemore.Device;

import rx.android.app.AppObservable;
import rx.functions.Action0;
import rx.functions.Action1;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ConfigureWidgetActivity 
        extends Activity
        implements OnItemClickListener {

    static final String TAG = "wemore:ConfigureWidgetActivity";

    static class DeviceAdapter extends BaseAdapter {

        List<Wemore.Device> devices = new ArrayList<Wemore.Device>();

        public void add(Device device) {
            devices.add(device);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Wemore.Device getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            Wemore.Device d = getItem(position);
            if (d == null)
                return 0;
            return d.hashCode(); // shrug?
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final TextView view;
            if (convertView == null) {
                final int padding = parent.getContext().getResources()
                    .getDimensionPixelOffset(R.dimen.padding);
                view = new TextView(parent.getContext());
                view.setPadding(padding, padding, padding, padding);
                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            } else {
                view = (TextView) convertView;
            }

            final Wemore.Device d = getItem(position);
            final String friendlyName = d == null 
                ? view.getContext().getString(R.string.all)
                : d.getFriendlyName();
            view.setText(TextUtils.isEmpty(friendlyName)
                    ? view.getContext().getString(R.string.unnamed)
                    : friendlyName);
            return view;
        }

    }

    @InjectView(android.R.id.list) ListView list;
    @InjectView(android.R.id.empty) View empty;

    DeviceAdapter adapter = new DeviceAdapter();
    Wemore wemore = new Wemore();
    MulticastHelper multicast;
    private int mAppWidgetId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure);
        ButterKnife.inject(this);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        adapter.add(null);

        multicast = MulticastHelper.acquire(this);
        queryDevices();

        // default
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        multicast.release();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        Wemore.Device device = adapter.getItem(position);
        Log.d(TAG, "Selected: " + device);
        // TODO
        configureWidget(device == null ? null : device.friendlyName);
    }

    void configureWidget(String friendlyName) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        final RemoteViews views = WidgetProvider.buildRemoteView(this, friendlyName);
        appWidgetManager.updateAppWidget(mAppWidgetId, views);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    void queryDevices() {
        AppObservable.bindActivity(this, wemore.search())
            .subscribe(new Action1<Device>() {

                @Override
                public void call(Device device) {
                    Log.v(TAG, "Found device:" + device);
                    adapter.add(device);
                }
            }, new Action1<Throwable>() {

                @Override
                public void call(Throwable e) {
                    Log.w(TAG, "ERROR!", e);
                }
            }, new Action0() {

                @Override
                public void call() {
                    Log.v(TAG, "Done!");
                    if (0 == adapter.getCount()) {
                        empty.setVisibility(View.VISIBLE);
                    }
                }
            });
    }
}
