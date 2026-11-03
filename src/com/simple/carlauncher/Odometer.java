package com.simple.carlauncher;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * GPS odometer for the oil-change reminder (the car's CAN box does not report mileage).
 *
 * The user sets the dashboard reading once; from then on distance between good GPS fixes
 * is added. Fixes while parked or with poor accuracy are ignored so drift does not count.
 */
public class Odometer extends Service implements LocationListener {

    private static final String TAG = "Odometer";
    static final int DEFAULT_INTERVAL_KM = 5000;
    static final int WARN_KM = 500;

    private LocationManager lm;
    private Location last;
    private float pendingMeters;
    private boolean locationUpdatesStarted;
    static final String ACTION_SPEED = "com.simple.carlauncher.GPS_SPEED";

    @Override
    public void onCreate() {
        super.onCreate();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        if (locationUpdatesStarted || lm == null
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            locationUpdatesStarted = true;
        } catch (Exception e) {
            Log.w(TAG, "no GPS", e);
        }
    }

    @Override
    public void onLocationChanged(Location l) {
        if (!l.hasAccuracy() || l.getAccuracy() > 25) return;
        float speedKmh = l.hasSpeed() ? l.getSpeed() * 3.6f : 0f;
        if (!l.hasSpeed() && last != null) {
            float seconds = Math.max(1f, (l.getTime() - last.getTime()) / 1000f);
            speedKmh = last.distanceTo(l) / seconds * 3.6f;
        }
        Intent speed = new Intent(ACTION_SPEED);
        speed.setPackage(getPackageName());
        speed.putExtra("speed_kmh", Math.round(speedKmh));
        speed.putExtra("speed_valid", true);
        sendBroadcast(speed);
        if (last != null) {
            float d = last.distanceTo(l);
            float secs = Math.max(1, (l.getTime() - last.getTime()) / 1000f);
            float kmh = d / secs * 3.6f;
            boolean moving = (l.hasSpeed() ? l.getSpeed() * 3.6f : kmh) > 4;
            // Count only real driving; drop parked drift and impossible jumps.
            if (moving && kmh < 200) pendingMeters += d;
        }
        last = l;
        if (pendingMeters >= 100) { // persist every 100 m
            SharedPreferences p = prefs(this);
            p.edit().putFloat("gps_km", p.getFloat("gps_km", 0) + pendingMeters / 1000f).apply();
            pendingMeters = 0;
        }
    }

    public void onStatusChanged(String p, int s, Bundle b) { }
    public void onProviderEnabled(String p) { }
    public void onProviderDisabled(String p) { }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (lm != null && locationUpdatesStarted) lm.removeUpdates(this);
        locationUpdatesStarted = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------- Shared state ----------

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("odometer", Context.MODE_PRIVATE);
    }

    static boolean isConfigured(Context c) {
        return prefs(c).contains("base_km");
    }

    /** Estimated dashboard reading: the value the user entered plus GPS distance since. */
    static int currentKm(Context c) {
        SharedPreferences p = prefs(c);
        return Math.round(p.getInt("base_km", 0) + p.getFloat("gps_km", 0));
    }

    static void setCurrentKm(Context c, int km) {
        prefs(c).edit().putInt("base_km", km).putFloat("gps_km", 0).apply();
    }

    static int lastChangeKm(Context c) {
        return prefs(c).getInt("last_change_km", currentKm(c));
    }

    static void setLastChangeKm(Context c, int km) {
        prefs(c).edit().putInt("last_change_km", km).apply();
    }

    static int intervalKm(Context c) {
        return prefs(c).getInt("interval_km", DEFAULT_INTERVAL_KM);
    }

    static void setIntervalKm(Context c, int km) {
        prefs(c).edit().putInt("interval_km", km).apply();
    }

    /** Km left until the next oil change (negative when overdue). */
    static int remainingKm(Context c) {
        return lastChangeKm(c) + intervalKm(c) - currentKm(c);
    }
}
