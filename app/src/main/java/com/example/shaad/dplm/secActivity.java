package com.example.shaad.dplm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteQuery;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.shaad.dplm.MapsActivity.DataBase;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class secActivity extends ActionBarActivity {
    private Button GoBackButton;
    private TextView Text;
    public static EditText Date;
    public static boolean DateSet = false;
    DataBase dbHelper;
    public long amountOfRecords = 0;

    final public Date SelectedDate = new Date();

    public Map<String, ArrayList<LatLng>> ListOfAreas = new HashMap<>();

    private String getDateTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    public void GetAreaList() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ArrayList<LatLng> LngArr = new ArrayList<>();
        amountOfRecords = DatabaseUtils.queryNumEntries(db, "mytable1");
        Cursor c = db.query("AreaTable", null, null, null, null, null, null);
        //   db.delete("AreaTable", null, null);
        if (c.moveToFirst()) {
            int NameIndex = c.getColumnIndex("Name");
            int DataIndex = c.getColumnIndex("Data");
            do {
                LngArr = MapsActivity.ParseLatLng(c.getString(DataIndex));
                ListOfAreas.put(c.getString(NameIndex), LngArr);
            } while (c.moveToNext());
        }
        c.close();

        dbHelper.close();
    }

    public void showMovementTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor c = db.query("MovementTable", null, null, null, null, null, null);
        String str = "";

        if (c.moveToFirst()) {
            Date d1 = new Date();
            d1.setTime(c.getLong(0));
            Date d2 = new Date();
            d2.setTime(c.getLong(1));
            do {
                if (!c.getString(6).equals("0"))
                    str = str + d1.toString() + " - " + d2.toString() + CheckPoint(new LatLng(c.getDouble(2), c.getDouble(3)))
                            + " to " + CheckPoint(new LatLng(c.getDouble(4), c.getDouble(5))) + " " + c.getString(6) + "\n";
            } while (c.moveToNext());
        }
        c.close();

        Text.setText(str);
        Text.setVisibility(View.VISIBLE);
        dbHelper.close();
    }

    String FirstString, SecondString;

    public void ListDialog(final int Location) {
        final ArrayList<String> Locations = new ArrayList<>();
        for (String Name : ListOfAreas.keySet()) {
            Locations.add(Name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Locations);

        AlertDialog.Builder builder = new AlertDialog.Builder(secActivity.this);
        if (Location == 0)
            builder.setTitle("Please select first location");
        else
            builder.setTitle("Please select second location");
        ListView list = new ListView(secActivity.this);
        list.setAdapter(adapter);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setAdapter(adapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Location == 0)
                            FirstString = Locations.get(which);
                        if (Location == 1)
                            SecondString = Locations.get(which);
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    public void TimeBetweenZones(View v) {
        FirstString = "";
        SecondString = "";
        ThreadState = true;

        ListDialog(1);
        ListDialog(0);
        final Handler h;
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Text.setText(GetTimeBetweenZones(FirstString, SecondString));
            }

            ;
        };

        Thread myThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (ThreadState)
                    if ((!FirstString.isEmpty()) && (!SecondString.isEmpty())) {
                        h.sendEmptyMessage(1);
                        ThreadState = false;
                    }
            }
        });

        myThread.start(); // запускаем
    }

    /**
     * reset table with tableName.
     *
     * @param tableName name of table
     */
    public void resetTable(final String tableName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(tableName, null, null);

        dbHelper.close();
    }

    public class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current time as the default values for the picker
            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Do something with the time chosen by the user
            Date = (EditText) findViewById(R.id.editText);
            String temp;

            if (hourOfDay < 10)
                temp = "0" + hourOfDay + ":";
            else
                temp = hourOfDay + ":";
            if (minute < 10)
                temp = temp + "0" + minute;
            else
                temp = temp + minute;

            Date.setText(temp);
            SelectedDate.setMinutes(minute);
            SelectedDate.setHours(hourOfDay);
            DateSet = true;
        }
    }

    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getSupportFragmentManager(), "timePicker");
    }

    public static boolean CheckPointPolygon(LatLng location, ArrayList<LatLng> polyLoc) {
        if (location == null)
            return false;

        LatLng lastPoint = polyLoc.get(polyLoc.size() - 1);
        boolean isInside = false;
        double x = location.longitude;

        for (LatLng point : polyLoc) {
            double x1 = lastPoint.longitude;
            double x2 = point.longitude;
            double dx = x2 - x1;

            if (Math.abs(dx) > 180.0) {
                if (x > 0) {
                    while (x1 < 0)
                        x1 += 360;
                    while (x2 < 0)
                        x2 += 360;
                } else {
                    while (x1 > 0)
                        x1 -= 360;
                    while (x2 > 0)
                        x2 -= 360;
                }
                dx = x2 - x1;
            }

            if ((x1 <= x && x2 > x) || (x1 >= x && x2 < x)) {
                double grad = (point.latitude - lastPoint.latitude) / dx;
                double intersectAtLat = lastPoint.latitude + ((x - x1) * grad);

                if (intersectAtLat > location.latitude)
                    isInside = !isInside;
            }
            lastPoint = point;
        }
        return isInside;
    }

    public static boolean CheckPointCircle(LatLng location, ArrayList<LatLng> polyLoc) {
        ArrayList<LatLng> Circle = new ArrayList<>();
        double Radius = MapsActivity.GetRadius(polyLoc);
        Circle.add(polyLoc.get(0));
        Circle.add(location);
        double Distance = MapsActivity.GetRadius(Circle);
        if (Distance <= Radius)
            return true;
        else
            return false;
    }

    public ArrayList<Integer> DateDifference(Date startDate, Date endDate) {
        //milliseconds
        ArrayList<Integer> result = new ArrayList<>();
        result.clear();

        long different = endDate.getTime() - startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        String temp = "";

        result.add((int) elapsedDays);
        result.add((int) elapsedHours);
        result.add((int) elapsedMinutes);
        result.add((int) elapsedSeconds);

        return result;
    }

    public String DateDifferenceString(Date startDate, Date endDate) {
        ArrayList<Integer> result = DateDifference(startDate, endDate);
        String temp = "";
        if (result.get(0) != 0)
            temp = temp + result.get(0) + " days ";
        if (result.get(1) != 0)
            temp = temp + result.get(1) + " hours ";
        if (result.get(2) != 0)
            temp = temp + result.get(2) + " minutes ";
        if (result.get(3) != 0)
            temp = temp + result.get(3) + " seconds";

        return temp;
    }

    public String GetTimeBetweenZones(String FirstZone, String SecondZone) {
        Text = (TextView) findViewById(R.id.DataBaseString);

        if (FirstZone.equals(SecondZone))
            return "You choose same locations";

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query("mytable1", null, null, null, null, null, null);

        ArrayList<String> Temp = new ArrayList<>();

        String DBdata = new String();
        Date firstZoneTime = new Date();
        Date secondZoneTime = new Date();

        boolean searchFlag = false;

        ArrayList<Integer> AverageTime = new ArrayList<>();
        AverageTime.add(0);
        AverageTime.add(0);
        AverageTime.add(0);
        AverageTime.add(0);

        if (c.moveToFirst()) {
            int timeIndex = c.getColumnIndex("created_at");
            int latColIndex = c.getColumnIndex("Latitude");
            int longColIndex = c.getColumnIndex("Longitude");

            boolean first = true;

            do {
                LatLng point = new LatLng(c.getDouble(latColIndex), c.getDouble(longColIndex));
                String Area = CheckPoint(point);
                if (Area.equals(FirstZone)) {
                    firstZoneTime = new Date(c.getLong(timeIndex));
                    searchFlag = true;
                }

                if (searchFlag) {
                    if (Area.equals(SecondZone)) {
                        secondZoneTime = new Date(c.getLong(timeIndex));
                        searchFlag = false;
                        ArrayList<Integer> temp = DateDifference(firstZoneTime, secondZoneTime);
                        if (temp.get(0) == 0 && temp.get(1) <= 3) {
                            if (first) {
                                AverageTime = temp;
                                first = !first;
                            } else if (first) {
                                AverageTime.set(1, (Integer) ((AverageTime.get(1) + temp.get(1) / 2)));
                                AverageTime.set(2, (Integer) ((AverageTime.get(2) + temp.get(2) / 2)));
                                AverageTime.set(3, (Integer) ((AverageTime.get(3) + temp.get(3) / 2)));
                            }
                        }
                    }
                }
            } while (c.moveToNext());
        }
        String temp = "";
        if (AverageTime.get(0) != 0)
            temp = temp + AverageTime.get(0) + " days ";
        if (AverageTime.get(1) != 0)
            temp = temp + AverageTime.get(1) + " hours ";
        if (AverageTime.get(2) != 0)
            temp = temp + AverageTime.get(2) + " minutes ";
        if (AverageTime.get(3) != 0)
            temp = temp + AverageTime.get(3) + " seconds";

        if (temp.isEmpty()) {
            return "You have never used this route";
        } else
            DBdata = DBdata + "Expected time is " + temp + "\n";

        c.close();

        c = db.query("MovementTable", null, null, null, null, null, null);

        DBdata = DBdata + "\nUsers measurements\n";

        if (c.moveToFirst()) {
            do {
                Date d1 = new Date();
                d1.setTime(c.getLong(0));
                Date d2 = new Date();
                d2.setTime(c.getLong(1));
                if ((!c.getString(6).equals("0")) && FirstZone.equals(CheckPoint(new LatLng(c.getDouble(2), c.getDouble(3))))
                        && SecondZone.equals(CheckPoint(new LatLng(c.getDouble(4), c.getDouble(5)))))
                    DBdata = DBdata + getDateTime(d1) + " - " + DateDifferenceString(d1, d2) + " - " + FirstZone + " to " + " " + SecondZone + "\n";
            } while (c.moveToNext());
        }
        c.close();

        dbHelper.close();

        return DBdata;
    }

    public String CheckPoint(LatLng location) {
        boolean result = false;
        for (Map.Entry<String, ArrayList<LatLng>> entry : ListOfAreas.entrySet()) {
            if (entry.getValue().size() > 2)
                result = CheckPointPolygon(location, entry.getValue());
            else
                result = CheckPointCircle(location, entry.getValue());
            if (result)
                return entry.getKey();
        }
        return "Unknown";
    }

    public void ShowStatMethod(View v) {
        Text = (TextView) findViewById(R.id.DataBaseString);
        HashMap<String, Double> StatTable = new HashMap<>();
        StatTable.clear();
        String SinceDateString = new String();

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query("mytable1", null, null, null, null, null, null);

        String DBdata = new String();

        boolean first = true;

        if (c.moveToFirst()) {
            int latColIndex = c.getColumnIndex("Latitude");
            int longColIndex = c.getColumnIndex("Longitude");

            if (first) {
                SinceDateString = getDateTime(new Date(c.getLong(c.getColumnIndex("created_at"))));
                first = false;
            }

            do {
                LatLng point = new LatLng(c.getDouble(latColIndex), c.getDouble(longColIndex));
                String Area = CheckPoint(point);
                if (StatTable.containsKey(Area))
                    StatTable.put(Area, StatTable.get(Area) + 1);
                else
                    StatTable.put(Area, 1.0);
            } while (c.moveToNext());
        }

        c.close();

        double Sum = 0;

        for (String AreaName : StatTable.keySet())
            Sum += StatTable.get(AreaName);

        for (String AreaName : StatTable.keySet()) {
            StatTable.put(AreaName, StatTable.get(AreaName) / Sum);
        }

        for (String AreaName : StatTable.keySet()) {
            DBdata = DBdata + AreaName + " - " + (int) (StatTable.get(AreaName) * 100) + "%\n";
        }

        Intent myIntent = new Intent(secActivity.this, Diagramm.class);
        myIntent.putExtra("hashMap", StatTable); //Optional parameters
        myIntent.putExtra("StatSince", SinceDateString);
        secActivity.this.startActivity(myIntent);
    }

    public class GetDataProgress extends AsyncTask<Void, Void, Void> {
        String DBData;
        ProgressDialog progressDialog;

        //declare other objects as per your need
        @Override
        protected void onPreExecute() {
            DBData = "";
            progressDialog = new ProgressDialog(secActivity.this);
            progressDialog.setTitle("Please wait a second");
            progressDialog.setMessage("Loading database");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax((int) amountOfRecords);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        ;

        @Override
        protected Void doInBackground(Void... params) {
            progressDialog.setIndeterminate(false);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            amountOfRecords = DatabaseUtils.queryNumEntries(db, "mytable1");
            Date time = new Date();
            Cursor c = db.query("mytable1", null, null, null, null, null, null);
            if (c.moveToFirst()) {
                int created_atColIndex = c.getColumnIndex("created_at");
                int latColIndex = c.getColumnIndex("Latitude");
                int longColIndex = c.getColumnIndex("Longitude");
                do {
                    time = new Date(c.getLong(created_atColIndex));
                    if (time.getHours() == SelectedDate.getHours() && time.getMinutes() == SelectedDate.getMinutes()) {
                        LatLng point = new LatLng(c.getDouble(latColIndex), c.getDouble(longColIndex));
                        DBData = DBData + getDateTime(time) + " " + CheckPoint(point) + "\n";
                    }
                    progressDialog.incrementProgressBy(1);
                    progressDialog.incrementSecondaryProgressBy(1);
                } while (c.moveToNext());
            } else
                DBData = "Table is Empty";

            if (DBData == "")
                DBData = "No location in this time found";

            c.close();
            dbHelper.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Text.setText(DBData);
            super.onPostExecute(result);
            progressDialog.dismiss();
        }

        ;
    }

    public void showSelected(View v) {
        if (DateSet) {
            GetDataProgress task = new GetDataProgress();
            task.execute();
        } else {
            Context context = getApplicationContext();
            CharSequence text = "Select time first";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sec);

        Text = (TextView) findViewById(R.id.DataBaseString);
        Date = (EditText) findViewById(R.id.editText);
        Date.setActivated(false);

        dbHelper = new DataBase(secActivity.this);

        DateSet = false;

        GoBackButton = (Button) findViewById(R.id.GoBack);

        View.OnClickListener GBB = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                secActivity.this.finish();
            }
        };

        GoBackButton.setOnClickListener(GBB);
        GetAreaList();
        //ShowMovementTable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sec, menu);
        return true;
    }

    boolean ThreadState = true;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent myIntent = new Intent(secActivity.this, SettingsActivity.class);
            secActivity.this.startActivity(myIntent);
            return true;
        }
        if (id == R.id.clear_location) {
            AlertDialog.Builder builder = new AlertDialog.Builder(secActivity.this);
            builder.setTitle("Do you really want to clear Location Table")
                    .setMessage("You would not be able to get statistics of your locations after doing this")
                    .setCancelable(false)
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    resetTable("mytable1");

                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        if (id == R.id.clear_movement) {
            AlertDialog.Builder builder = new AlertDialog.Builder(secActivity.this);
            builder.setTitle("Do you really want to clear Movement Table")
                    .setMessage("You would not be able to get your movement data after doing this")
                    .setCancelable(false)
                    .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    resetTable("MovementTable");

                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();
        }

        return super.onOptionsItemSelected(item);
    }
}

