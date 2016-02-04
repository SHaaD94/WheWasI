package com.example.shaad.dplm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;

import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.example.shaad.dplm.MapsActivity.DataBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class GetLocationService extends Service {
    private final IBinder mBinder = new MyBinder();

    public LocationManager locationManager;
    public MyLocationListener listener;
    DataBase dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("myLog","onCreate");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent){
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 1000,
                restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("myLog","onStartCommand");
        super.onStartCommand(intent,flags,startId);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

        Notification note=new Notification(R.drawable.ic_launcher,
                "WhereAmI",
                System.currentTimeMillis());
        Intent i=new Intent(this, MapsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this, 0,
                i, 0);
        note.setLatestEventInfo(this, "Service is running","getting current location",pi);
        note.flags|=Notification.FLAG_NO_CLEAR;
        startForeground(1337, note);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        GetLocationService getService() {
            return GetLocationService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("STOP_SERVICE", "DONE");
        //locationManager.removeUpdates(listener);
    }

    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    public class MyLocationListener implements LocationListener
    {
        public void onLocationChanged(final Location location) {
            dbHelper = new DataBase(GetLocationService.this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();

            long time = System.currentTimeMillis();
            Date LastDate = new Date();
            Date CurrentDate = new Date (time);

            Cursor c = db.query("mytable1", null, null, null, null, null, null);

            if (c.moveToLast()) {
                LastDate = new Date(c.getLong(c.getColumnIndex("created_at")));

                if (!(CurrentDate.getHours() == LastDate.getHours() && LastDate.getMinutes() == CurrentDate.getMinutes())) {
                    cv.put("created_at", time);
                    cv.put("Latitude", location.getLatitude());
                    cv.put("Longitude", location.getLongitude());
                    db.insert("mytable1", null, cv);
                }
            } else {
                cv.put("created_at", time);
                cv.put("Latitude", location.getLatitude());
                cv.put("Longitude", location.getLongitude());
                db.insert("mytable1", null, cv);
            }

            c.close();
            dbHelper.close();
        }

        public void onProviderDisabled(String provider)
        {
            //  Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider)
        {
            //Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }
    }
}