package net.dhleong.wemore;

import java.util.ArrayList;
import java.util.List;

import net.dhleong.wemore.Wemore.Device;

import rx.android.app.AppObservable;
import rx.functions.Action0;
import rx.functions.Action1;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class ConfigureWidgetActivity 
        extends Activity
        implements OnItemClickListener {

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
            return getItem(position).hashCode(); // shrug?
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final TextView view;
            if (convertView == null) {
                view = new TextView(parent.getContext());
            } else {
                view = (TextView) convertView;
            }

            final Wemore.Device d = getItem(position);
            final String friendlyName = d.getFriendlyName();
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.configure);

        ButterKnife.inject(this);

        list.setAdapter(adapter);
        list.setOnItemClickListener(this);

        queryDevices();
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        Wemore.Device device = adapter.getItem(position);
        Log.d("wemore", "Selected: " + device);
        // TODO
    }

    void queryDevices() {
        // TODO configure wifi multicast

        AppObservable.bindActivity(this, wemore.search())
            .doOnNext(new Action1<Device>() {

                @Override
                public void call(Device device) {
                    adapter.add(device);
                }
            })
            .doOnCompleted(new Action0() {

                @Override
                public void call() {
                    if (0 == adapter.getCount()) {
                        empty.setVisibility(View.VISIBLE);
                    }
                }
            });
    }
}
