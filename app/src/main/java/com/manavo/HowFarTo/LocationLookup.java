package com.manavo.HowFarTo;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

public class LocationLookup {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final WeakReference<main> activity;
    private final Geocoder geocoder;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public LocationLookup(main activity) {
        this.activity = new WeakReference<>(activity);
        this.geocoder = new Geocoder(activity.getApplicationContext());
    }

    public void cancel() {
        this.cancelled.set(true);
    }

    public void execute(String location) {
        EXECUTOR.execute(() -> {
            List<Address> addresses = null;
            String error = null;

            try {
                addresses = this.geocoder.getFromLocationName(location.trim(), 5);
            } catch (IOException e) {
                error = "Could not connect. Please try again!";
            } catch (Exception e) {
                error = "Oops! Something went wrong! Please try again!";
            }

            final List<Address> result = addresses;
            final String resultError = error;
            this.mainHandler.post(() -> {
                main currentActivity = this.activity.get();
                if (this.cancelled.get() || currentActivity == null || currentActivity.isDestroyed()) {
                    return;
                }
                if (result != null) {
                    currentActivity.searchLocationCallback(result);
                } else if (resultError != null) {
                    currentActivity.searchLocationError(resultError);
                }
                currentActivity.hideDialog();
            });
        });
    }
}
