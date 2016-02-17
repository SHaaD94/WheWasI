package com.example.shaad.dplm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Used for drawing statistics diagram.
 */
public class Diagram extends ActionBarActivity {
    /**
     * Stat list: zone - time spent in %.
     */
    private HashMap<String, Double> statTable = new HashMap<>();
    /**
     * Colors.
     */
    private ArrayList<Integer> colors = new ArrayList<>();

    /**
     * text view with sectors description.
     */
    private TextView textView;

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int maxColor = 255;
        View view = this.getWindow().getDecorView();
        int color = Color.rgb(maxColor, maxColor, maxColor);
        view.setBackgroundColor(color);

        setContentView(R.layout.activity_diagramm);
        LinearLayout linear = (LinearLayout) findViewById(R.id.chart);
        LinearLayout description = (LinearLayout) findViewById(R.id.descr);

        Intent intent = getIntent();
        statTable = (HashMap<String, Double>)
                intent.getSerializableExtra("hashMap");

        String statSince = (String) intent.getSerializableExtra("StatSince");
        textView = (TextView) findViewById(R.id.Since);
        textView.setText("Statistics since: " + statSince);

        colors.add(Color.parseColor("#4D4D4D"));
        colors.add(Color.parseColor("#5DA5DA"));
        colors.add(Color.parseColor("#60BD68"));
        colors.add(Color.parseColor("#FAA43A"));
        colors.add(Color.parseColor("#F17CB0"));
        colors.add(Color.parseColor("#B2912F"));
        colors.add(Color.parseColor("#B276B2"));
        colors.add(Color.parseColor("#DECF3F"));
        colors.add(Color.parseColor("#F15854"));

        while (statTable.size() >= colors.size()) {
            Random rnd = new Random();
            colors.add(Color.argb(maxColor, rnd.nextInt(maxColor),
                    rnd.nextInt(maxColor), rnd.nextInt(maxColor)));
        }
        linear.addView(new MyGraphview(this));

        int i = 0;
        for (String areaName : statTable.keySet()) {
            TextView txt = new TextView(this);
            txt.setTextColor(colors.get(i));
            txt.setText(areaName);
            description.addView(txt);
            i++;
        }
    }

    @Override
    public final boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //     getMenuInflater().inflate(R.menu.menu_diagramm, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(final MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Diagram.
     */
    private class MyGraphview extends View {
        /**
         * Painter.
         */
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        /**
         * Used to define sectors.
         */
        private float[] valueDegree;
        /**
         * Display.
         */
        private Display display = getWindowManager().getDefaultDisplay();

        /**
         * Drawing rectangle.
         */
        private RectF rectf = new RectF(40, 20, display.getWidth() - 40,
                display.getWidth() - 60);
        private int temp = 0;

        public MyGraphview(final Context context) {
            super(context);
            valueDegree = new float[statTable.size()];

            int i = 0;

            for (String areaName : statTable.keySet()) {
                valueDegree[i] = (float) (360 * statTable.get(areaName));
                i++;
            }
        }

        @Override
        protected final void onDraw(final Canvas canvas) {
            // TODO Auto-generated method stub
            temp = 0;
            super.onDraw(canvas);

            for (int i = 0; i < valueDegree.length; i++) {
                if (i == 0) {
                    if (valueDegree.length > colors.size()) {
                        Random rnd = new Random();
                        paint.setARGB(255, rnd.nextInt(256),
                                rnd.nextInt(256), rnd.nextInt(256));
                    } else {
                        paint.setColor(colors.get(i));
                    }
                    canvas.drawArc(rectf, 0, valueDegree[i], true, paint);
                } else {
                    temp += (int) valueDegree[i - 1];
                    paint.setColor(colors.get(i));
                    if (i == valueDegree.length - 1) {
                        canvas.drawArc(rectf, temp, 360 - temp, true, paint);
                    } else {
                        canvas.drawArc(rectf, temp, valueDegree[i],
                                true, paint);
                    }
                }
            }
        }
    }
}
