package com.example.shaad.dplm;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.widget.Button;
import android.widget.EditText;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener,
        LocationListener {

    private Button ShowHideZones;
    private Button FinishRecButton;
    private Button StartRecButton;
    private Button AddLocationDone;
    private Button CancelButton;
    private boolean AddLocationFlag = false;
    private String Shape;

    Map<Marker, Circle> MarkerCircle = new HashMap<Marker, Circle>();
    Map<Marker, Polygon> MarkerPolygon = new HashMap<Marker, Polygon>();

    private boolean AreasVisible = false;
    private boolean firstZoom;

    private ArrayList<LatLng> ListOfLatLng = new ArrayList<LatLng>();

    public static final String TAG = MapsActivity.class.getSimpleName();

    /*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    DataBase dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        Shape = "";

        MarkerCircle.clear();
        MarkerPolygon.clear();

        firstZoom = false;
        m_Text = "";

        dbHelper = new DataBase(this);

        ShowHideZones = (Button) findViewById(R.id.ShowArea);
        StartRecButton = (Button) findViewById(R.id.Start);
        FinishRecButton = (Button) findViewById(R.id.Close);
        AddLocationDone = (Button) findViewById(R.id.AddDone);
        CancelButton = (Button) findViewById(R.id.Cancel);

        CancelButton.setVisibility(View.INVISIBLE);
        AddLocationDone.setVisibility(View.INVISIBLE);

        Button secActivityButton = (Button) findViewById(R.id.button);
        OnClickListener SAB = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MapsActivity.this, StatActivity.class);
                //myIntent.putExtra("key", value); //Optional parameters
                MapsActivity.this.startActivity(myIntent);
            }
        };

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5 * 1000)
                .setFastestInterval(1 * 1000);

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        secActivityButton.setOnClickListener(SAB);

        GetAreaList();
        for (Marker marker : MarkerCircle.keySet())
            marker.setVisible(false);

        for (Marker marker : MarkerPolygon.keySet())
            marker.setVisible(false);

        Intent intent = new Intent(this, GetLocationService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        startService(intent);

        if (CheckRecordState()) {
            StartRecButton.setVisibility(View.VISIBLE);
            FinishRecButton.setVisibility(View.INVISIBLE);
        } else {
            StartRecButton.setVisibility(View.INVISIBLE);
            FinishRecButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setPadding(0, 0, 0, 100);
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();

        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        firstZoom = true;
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    public void onConnected(Bundle bundle) {
        AddLocationFlag = false;
        m_Text = "";
        Shape = "";

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public LatLng centroid(List<LatLng> points) {
        double[] centroid = {0.0, 0.0};
        for (int i = 0; i < points.size(); i++) {
            centroid[0] += points.get(i).latitude;
            centroid[1] += points.get(i).longitude;
        }
        int totalPoints = points.size();
        centroid[0] = centroid[0] / totalPoints;
        centroid[1] = centroid[1] / totalPoints;
        return new LatLng(centroid[0], centroid[1]);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch (keycode) {
            case KeyEvent.KEYCODE_MENU:
                //Toast.makeText( getApplicationContext(), "menu button clicked", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onKeyDown(keycode, e);
    }

    static public ArrayList<LatLng> ParseLatLng(String input) {
        ArrayList<LatLng> temp = new ArrayList<>();
        String TempString1 = new String();
        String TempString2 = new String();
        boolean InputFlag = false;
        for (int i = 0; i < input.length(); i++) {
            if (!InputFlag) {
                if (input.charAt(i) == '(') {
                    i++;
                    TempString1 += input.charAt(i);
                } else if (input.charAt(i) == ',')
                    InputFlag = true;
                else
                    TempString1 += input.charAt(i);
            } else if (input.charAt(i) == ')') {
                temp.add(new LatLng(Double.parseDouble(TempString1), Double.parseDouble(TempString2)));
                TempString1 = "";
                TempString2 = "";
                InputFlag = false;
            } else
                TempString2 += input.charAt(i);
        }
        return temp;
    }

    public boolean CheckRecordState() // true - finished, false - not finished
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selectQuery = "SELECT RecordDone FROM MovementTable WHERE RecordDone=?";
        boolean result;
        Cursor c = db.rawQuery(selectQuery, new String[]{"0"});
        if (c.moveToFirst())
            result = false;
        else
            result = true;
        c.close();

        dbHelper.close();
        return result;
    }

    public void StartRecord(View v) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        long time = System.currentTimeMillis();

        if (location == null) {
            Toast.makeText(getApplicationContext(),
                    "Can not start record: can't get current location", Toast.LENGTH_LONG).show();
            return;
        }

        cv.put("FirstDate", time);
        cv.put("SecondDate", "-");
        cv.put("FirstLat", location.getLatitude());
        cv.put("FirstLong", location.getLongitude());
        cv.put("SecondLat", "");
        cv.put("SecondLong", "");
        cv.put("RecordDone", 0);

        db.insert("MovementTable", null, cv);

        dbHelper.close();

        StartRecButton.setVisibility(View.INVISIBLE);
        FinishRecButton.setVisibility(View.VISIBLE);
    }

    public void CloseRecord(View v) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        long time = System.currentTimeMillis();

        db.execSQL("UPDATE MovementTable "
                + "SET SecondDate='" + time
                + "', SecondLat='" + location.getLatitude()
                + "', SecondLong='" + location.getLongitude()
                + "', RecordDone=1"
                + " WHERE RecordDone=0");

        dbHelper.close();
        StartRecButton.setVisibility(View.VISIBLE);
        FinishRecButton.setVisibility(View.INVISIBLE);
    }

    public void GetAreaList() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ArrayList<LatLng> LngArr = new ArrayList<>();
        Cursor c = db.query("AreaTable", null, null, null, null, null, null);

        if (c.moveToFirst()) {
            int NameIndex = c.getColumnIndex("Name");
            int ShapeIndex = c.getColumnIndex("Type");
            int DataIndex = c.getColumnIndex("Data");
            do {
                LngArr = ParseLatLng(c.getString(DataIndex));
                String Shape = c.getString(ShapeIndex);
                if (Shape.equals("Circle")) {
                    Circle circle = mMap.addCircle(new CircleOptions()
                            .center(LngArr.get(0))
                            .radius(GetRadius(LngArr))
                            .strokeColor(Color.GREEN)
                            .fillColor(Color.argb(30, 0, 127, 0))
                            .zIndex(55));
                    MarkerCircle.put(mMap.addMarker(new MarkerOptions().position(LngArr.get(0)).title(c.getString(NameIndex))), circle);
                    circle.setVisible(false);
                }
                if (Shape.equals("Polygon")) {
                    Polygon polygon = mMap.addPolygon(new PolygonOptions()
                            .addAll(LngArr)
                            .strokeColor(Color.RED)
                            .fillColor(Color.argb(30, 127, 0, 0))
                            .zIndex(55));
                    MarkerPolygon.put(mMap.addMarker(new MarkerOptions().position(centroid(LngArr)).title(c.getString(NameIndex))), polygon);
                    polygon.setVisible(false);
                }
            } while (c.moveToNext());
        } else {
            Toast.makeText(getApplicationContext(), "Location list is empty, please try again", Toast.LENGTH_SHORT).show();
        }
        c.close();

        dbHelper.close();
    }

    public void ShowHideAreas(View v) {
        if (MarkerCircle.isEmpty() && MarkerPolygon.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Area list is empty", Toast.LENGTH_SHORT).show();
        } else {
            AreasVisible = !AreasVisible;

            if (!AreasVisible)
                ShowHideZones.setText("Show Areas");
            else
                ShowHideZones.setText("Hide Areas");

            for (Marker key : MarkerCircle.keySet()) {
                key.setVisible(AreasVisible);
                MarkerCircle.get(key).setVisible(AreasVisible);
            }

            for (Marker key : MarkerPolygon.keySet()) {
                key.setVisible(AreasVisible);
                MarkerPolygon.get(key).setVisible(AreasVisible);
            }
        }
    }

    private void DB_Area_Paste(String Name, int Shape, ArrayList<LatLng> LatLngList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("Name", Name);
        if (Shape == 0) {
            cv.put("Type", "Circle");
        } else {
            cv.put("Type", "Polygon");
        }

        String temp = new String();
        for (int i = 0; i < LatLngList.size(); i++) {
            temp = temp + "(" + LatLngList.get(i).latitude + "," + LatLngList.get(i).longitude + ")";
        }
        cv.put("Data", temp);

        db.insert("AreaTable", null, cv);

        dbHelper.close();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (!firstZoom) {
            handleNewLocation(location);
        }
    }

    private void RemoveLocation(Marker marker) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        marker.remove();
        if (MarkerPolygon.containsKey(marker)) {
            MarkerPolygon.get(marker).setVisible(false);
            MarkerPolygon.remove(marker);
        } else if (MarkerCircle.containsKey(marker)) {
            MarkerCircle.get(marker).setVisible(false);
            MarkerCircle.remove(marker);
        }

        db.delete("AreaTable", "Name = ?", new String[]{marker.getTitle()});
        Toast.makeText(getApplicationContext(), marker.getTitle() + " deleted successfully", Toast.LENGTH_SHORT).show();
        dbHelper.close();
    }

    public void RenameLocation(Marker marker) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL("UPDATE AreaTable SET Name='" + m_Text + "' WHERE Name=?",
                new String[]{marker.getTitle()});
        Toast.makeText(getApplicationContext(), marker.getTitle() + " was successfully renamed to " + m_Text, Toast.LENGTH_SHORT).show();

        marker.setTitle(m_Text);
        marker.setVisible(false);
        marker.setVisible(true);

        dbHelper.close();
    }

    private void Cancel() {
        AddLocationDone.setVisibility(View.INVISIBLE);
        CancelButton.setVisibility(View.INVISIBLE);

        AddLocationFlag = false;
        ListOfLatLng.clear();

        for (int i = 0; i < TempMarkers.size(); i++)
            TempMarkers.get(i).remove();
        TempMarkers.clear();
    }

    private String m_Text = "";

    private class GetStringDialog extends AsyncTask<Void, Void, String> {
        public boolean state;
        public Marker marker;

        public GetStringDialog(boolean state, Marker marker) {
            this.state = state;
            this.marker = marker;
        }

        public GetStringDialog(boolean state) {
            this.state = state;
        }

        @Override
        protected void onPreExecute() {

            choose = 0;
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setTitle("Title");
            final EditText input = new EditText(MapsActivity.this);
            input.setBackgroundColor(Color.WHITE);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_Text = input.getText().toString();
                    if (state) {
                        RenameLocation(marker);
                    }
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    AddLocationDone.setVisibility(View.INVISIBLE);
                    CancelButton.setVisibility(View.INVISIBLE);
                    Cancel();
                }
            });
            builder.show();
        }

        @Override
        protected String doInBackground(Void... params) {

            return "";
        }

        @Override
        protected void onPostExecute(String result) {

        }
    }

    ;

    static public double GetRadius(ArrayList<LatLng> Arr) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(Arr.get(1).latitude - Arr.get(0).latitude);
        double dLng = Math.toRadians(Arr.get(1).longitude - Arr.get(0).longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(Arr.get(0).latitude)) * Math.cos(Math.toRadians(Arr.get(1).latitude)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        float dist = (float) (earthRadius * c);

        return dist;
    }

    private ArrayList<Marker> TempMarkers = new ArrayList<>();

    private boolean FirstTime = true;

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (AddLocationFlag) {
            if (FirstTime) {
                FirstTime = false;
                TempMarkers.clear();
                CancelButton.setVisibility(View.VISIBLE);
            }
            if (Shape == "Circle") {
                ListOfLatLng.add(latLng);
                TempMarkers.add(mMap.addMarker(new MarkerOptions().position(latLng).title(TempMarkers.size() + "")));

                if (ListOfLatLng.size() == 2) {
                    double Radius = GetRadius(ListOfLatLng);
                    Circle circle = mMap.addCircle(new CircleOptions()
                                    .center(ListOfLatLng.get(0))
                                    .radius(Radius)
                                    .strokeColor(Color.GREEN)
                                    .fillColor(Color.argb(30, 0, 127, 0))
                                    .zIndex(55)
                    );
                    MarkerCircle.put(
                            mMap.addMarker(new MarkerOptions().position(ListOfLatLng.get(0)).title(m_Text)), circle);
                    AddLocationFlag = false;
                    DB_Area_Paste(m_Text, 0, ListOfLatLng);
                    ListOfLatLng.clear();

                    for (int i = 0; i < TempMarkers.size(); i++)
                        TempMarkers.get(i).remove();
                    TempMarkers.clear();
                    CancelButton.setVisibility(View.INVISIBLE);
                    FirstTime = true;
                }
            } else if (Shape == "Polygon") {
                ListOfLatLng.add(latLng);
                TempMarkers.add(mMap.addMarker(new MarkerOptions().position(latLng).title(TempMarkers.size() + "")));
                AddLocationDone.setVisibility(View.VISIBLE);
                CancelButton.setVisibility(View.VISIBLE);
                ListOfLatLng.add(latLng);
            }
        }
    }

    public void AddingLocationComplete(View v) {
        if (ListOfLatLng.size() < 3) {
            Toast.makeText(getApplicationContext(), "Put more points, please!", Toast.LENGTH_SHORT).show();
        } else {
            AddLocationDone.setVisibility(View.INVISIBLE);
            Polygon polygon = mMap.addPolygon(new PolygonOptions()
                    .addAll(ListOfLatLng)
                    .strokeColor(Color.RED)
                    .fillColor(Color.argb(30, 127, 0, 0))
                    .zIndex(55));
            DB_Area_Paste(m_Text, 1, ListOfLatLng);
            MarkerPolygon.put(
                    mMap.addMarker(new MarkerOptions().position(centroid(ListOfLatLng)).title(m_Text)),
                    polygon);
            AddLocationFlag = false;
            ListOfLatLng.clear();
            CancelButton.setVisibility(View.INVISIBLE);

            for (int i = 0; i < TempMarkers.size(); i++)
                TempMarkers.get(i).remove();
            TempMarkers.clear();

            FirstTime = true;
        }
    }

    public void AddLocation(View v) {
        NewLocationDialogBox();
    }

    public void MarkerClick(final Marker marker) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
        alertDialogBuilder.setMessage("What are you going to do with that location?");
        alertDialogBuilder.setNeutralButton("Remove",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        RemoveLocation(marker);
                    }
                });
        alertDialogBuilder.setPositiveButton("Rename",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        final GetStringDialog getStringDialog = new GetStringDialog(true, marker);
                        getStringDialog.execute();
                    }
                });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    ;

    public void NewLocationDialogBox() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("What type of location do you want to add?");
        alertDialogBuilder.setNeutralButton("Circle",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Cancel();
                        AddLocationFlag = true;
                        Shape = "Circle";
                        new GetStringDialog(false).execute();
                        Toast.makeText(getApplicationContext(), "Now set center and radius by long touch on map", Toast.LENGTH_LONG).show();
                    }
                });
        alertDialogBuilder.setPositiveButton("Polygon",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Cancel();
                        AddLocationFlag = true;
                        Shape = "Polygon";
                        new GetStringDialog(false).execute();
                        Toast.makeText(getApplicationContext(), "Now put points at the corners by long touch on map", Toast.LENGTH_LONG).show();
                    }
                });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        Cancel();
                        AddLocationFlag = false;
                        Shape = "";
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void CancelButton(View v) {
        AddLocationDone.setVisibility(View.INVISIBLE);
        CancelButton.setVisibility(View.INVISIBLE);

        AddLocationFlag = false;
        ListOfLatLng.clear();

        for (int i = 0; i < TempMarkers.size(); i++)
            TempMarkers.get(i).remove();
        TempMarkers.clear();
    }

    int choose = 0;

    @Override
    public void onMapClick(LatLng latLng) {
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        choose = 0;
        if (!AddLocationFlag)
            MarkerClick(marker);
        return false;
    }

    public static class DataBase extends SQLiteOpenHelper {
        private static final String LOG_TAG = "my logs";

        public DataBase(Context context) {
            super(context, "DB", null, 3);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(LOG_TAG, "--- onCreate database ---");
            db.execSQL("create table mytable1 ("
                    + "id integer primary key autoincrement,"
                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "Latitude real,"
                    + "Longitude text"
                    + ");");

            db.execSQL("create table AreaTable ("
                    + "Name text,"
                    + "Type text,"
                    + "Data text"
                    + ");");

            db.execSQL("create table MovementTable ("
                    + "FirstDate text,"
                    + "SecondDate text,"
                    + "FirstLat text,"
                    + "FirstLong text,"
                    + "SecondLat text,"
                    + "SecondLong text,"
                    + "RecordDone integer"
                    + ");");
        }
    }
}
