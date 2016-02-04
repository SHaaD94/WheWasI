package com.example.shaad.dplm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.v4.app.INotificationSideChannel;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.internal.widget.ActivityChooserView;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Diagramm extends ActionBarActivity {
    HashMap<String, Double> StatTable = new HashMap<>();
    ArrayList<Integer> Colors = new ArrayList<>();

    private TextView Text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = this.getWindow().getDecorView();
        int color = Color.rgb(225,225,225);
        view.setBackgroundColor(color);

        setContentView(R.layout.activity_diagramm);
        LinearLayout linear=(LinearLayout) findViewById(R.id.chart);
        LinearLayout descr=(LinearLayout) findViewById(R.id.descr);

        Intent intent = getIntent();
        StatTable = (HashMap<String, Double>)intent.getSerializableExtra("hashMap");

        String StatSince = (String)intent.getSerializableExtra("StatSince");
        Text = (TextView) findViewById(R.id.Since);
        Text.setText("Statistics since: " + StatSince);

        /*
        Colors.add(Color.GREEN);
        Colors.add(Color.CYAN);
        Colors.add(Color.DKGRAY);
        Colors.add(Color.RED);
        Colors.add(Color.MAGENTA);
        Colors.add(Color.WHITE);
        Colors.add(Color.YELLOW);
        Colors.add(Color.BLUE);*/

        Colors.add(Color.parseColor("#4D4D4D"));
        Colors.add(Color.parseColor("#5DA5DA"));
        Colors.add(Color.parseColor("#60BD68"));
        Colors.add(Color.parseColor("#FAA43A"));
        Colors.add(Color.parseColor("#F17CB0"));
        Colors.add(Color.parseColor("#B2912F"));
        Colors.add(Color.parseColor("#B276B2"));
        Colors.add(Color.parseColor("#DECF3F"));
        Colors.add(Color.parseColor("#F15854"));


        linear.addView(new MyGraphview(this));

        int i=0;
        for (String AreaName: StatTable.keySet())
        {
            TextView txt = new TextView(this);
            txt.setTextColor(Colors.get(i));
            txt.setText(AreaName);
            descr.addView(txt);
            i++;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
   //     getMenuInflater().inflate(R.menu.menu_diagramm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class MyGraphview extends View
    {
        private Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private float[] value_degree;
        Display display = getWindowManager().getDefaultDisplay();

        RectF rectf = new RectF(40, 20, display.getWidth()-40, display.getWidth()-60);
        int temp=0;
        public MyGraphview(Context context) {

            super(context);
            value_degree=new float[StatTable.size()];

            int i=0;

            for (String AreaName: StatTable.keySet())
            {
                value_degree[i]=(float)(360*StatTable.get(AreaName));
                i++;
            }
        }
        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            temp=0;
            super.onDraw(canvas);

            for (int i = 0; i < value_degree.length; i++) {
                if (i == 0) {
                    if (value_degree.length>Colors.size()) {
                        Random rnd = new Random();
                        paint.setARGB(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                    }
                    else
                        paint.setColor(Colors.get(i));
                    canvas.drawArc(rectf, 0, value_degree[i], true, paint);
                }
                else
                {
                    temp += (int)value_degree[i - 1];
                    paint.setColor(Colors.get(i));
                    if (i==value_degree.length-1)
                        canvas.drawArc(rectf, temp, 360-temp, true, paint);
                    else
                        canvas.drawArc(rectf, temp, value_degree[i], true, paint);
                }
            }
        }

    }

}
