package net.dhleong.wemore;

import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;

public class MulticastHelper {

    private static final String TAG = "wemoreMulticast";
    static MulticastHelper instance_;

    final AtomicInteger refs = new AtomicInteger(1);
    final MulticastLock lock;

    private MulticastHelper(MulticastLock lock) {
        this.lock = lock;
    }

    public void release() {
        if (refs.decrementAndGet() <= 0) {
            instance_ = null;
            lock.release();
        }
    }

    public static MulticastHelper acquire(final Context context) {
        final MulticastHelper existing = instance_;
        if (existing != null && existing.lock.isHeld()) {
            existing.refs.incrementAndGet();
            return existing;
        }

        final WifiManager mgr = (WifiManager) context
            .getSystemService(Context.WIFI_SERVICE);
        final MulticastLock lock = mgr.createMulticastLock(TAG);
        lock.acquire();
        return instance_ = new MulticastHelper(lock);
    }
}
