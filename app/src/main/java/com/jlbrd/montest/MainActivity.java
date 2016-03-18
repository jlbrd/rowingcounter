package com.jlbrd.montest;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.renderscript.Element;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

import static android.view.WindowManager.*;

public class MainActivity extends /*ActionBarActivity  */ Activity implements SensorEventListener {

    public class UI {
        TextView compteur, moyenne, heure, vitesse;
        TextClock dureeText;
        Button stepButton;
        Chronometer chrono, chronoHeure;
        ImageView imageView;
        UI() {
            this.compteur = (TextView) findViewById(R.id.compteur);
            this.heure = (TextView) findViewById(R.id.heure);
            this.moyenne = (TextView) findViewById(R.id.moyenne);
            this.chronoHeure = (Chronometer) findViewById(R.id.chronoHeure);
            this.chrono = (Chronometer) findViewById(R.id.chronometer);
            this.stepButton = (Button) findViewById(R.id.stepButton);
            this.vitesse = (TextView) findViewById(R.id.vitesse);
            this.imageView = (ImageView) findViewById(R.id.imageView);
        }
    }

    private static Context context;
    private UI ui;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Integer m_compteur = -1;
    private Calendar calendar = null;
    private long debut = 0;
    private long debutMoyenne = 0;
    private Path mPath;
    private List<Long> listeMoyenne= new ArrayList();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ui = new UI();
        //FitApi fitApi = new FitApi(this);
        mPath = new Path();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        ui.compteur.setVisibility(View.INVISIBLE);
        ui.moyenne.setVisibility(View.INVISIBLE);
        ui.chrono.setVisibility(View.INVISIBLE);
        ui.chronoHeure.setVisibility(View.INVISIBLE);
        ui.vitesse.setVisibility(View.INVISIBLE);
        ui.chronoHeure.setOnChronometerTickListener(
                new Chronometer.OnChronometerTickListener() {
                    public void onChronometerTick(Chronometer chronometer) {
                        String currentDateTimeString = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.FRANCE).format(new Date());
                        ui.heure.setText(currentDateTimeString);
                        if( System.currentTimeMillis() - debut > 10000 ) {
                            ui.chrono.stop();
                            //this.isChronometerRunning = false;
                        }
                    }
                }

        );
        ui.chronoHeure.start();
        if(Build.FINGERPRINT.startsWith("generic") == false) {
            ui.stepButton.setVisibility(View.INVISIBLE);
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public void calculs(View target) {
        long elapsed;
        final long now = System.currentTimeMillis();
        m_compteur++;
        if (m_compteur == 0) {
            calendar = Calendar.getInstance();
            ui.chrono.start();
            debutMoyenne = System.currentTimeMillis();
            debut = System.currentTimeMillis();
            ui.heure.setVisibility(View.VISIBLE);
            ui.chrono.setVisibility(View.VISIBLE);
            ui.moyenne.setVisibility(View.VISIBLE);
            ui.compteur.setVisibility(View.VISIBLE);
            ui.vitesse.setVisibility(View.VISIBLE);
        } else {
            ui.chrono.start();
            elapsed = now - debut;
            final long cpm = Math.round(((double) 1 / (double) elapsed) * 60000);
            ui.vitesse.setText(cpm > 0 ? String.valueOf(cpm) : "");
            listeMoyenne.add(cpm);
        }

        elapsed = now - debutMoyenne;
        if (elapsed <= 0) {
            return;
        }

        ui.compteur.setText(m_compteur.toString());

        final long cpmMoyenne = Math.round(((double) m_compteur / (double) elapsed) * 60000);
        ui.moyenne.setText(cpmMoyenne > 0 ? String.valueOf(cpmMoyenne) : "");

        dessineCourbe();
        debut = System.currentTimeMillis();
    }

    private void dessineCourbe() {
        Bitmap bitmap = Bitmap.createBitmap(ui.imageView.getWidth() /*width*/, ui.imageView.getHeight() /*height*/, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint pBackground = new Paint();

        pBackground = new Paint();
        pBackground.setAntiAlias(true);
        pBackground.setDither(true);
        pBackground.setColor(0xFFFF0000);
        pBackground.setStyle(Paint.Style.STROKE);
        pBackground.setStrokeJoin(Paint.Join.ROUND);
        pBackground.setStrokeWidth(10);

        mPath.reset();

        int mode = 0;
        switch(mode) {
            case 0:
                int w;
                Float h;
                w = (bitmap.getWidth() / listeMoyenne.size()) ;
                h = new Float(0);
                for(Long cpm : listeMoyenne) {
                    h = Math.max(h, cpm);
                }
                h = bitmap.getHeight() / h;
                mPath.moveTo(0, bitmap.getHeight()-(h *listeMoyenne.get(0)));
                for(Integer numCpm = 1; numCpm < listeMoyenne.size(); numCpm++) {
                    Long cpm = listeMoyenne.get(numCpm);
                    mPath.lineTo(w * numCpm, bitmap.getHeight()-(h * cpm));
                }
                break;
            case 1:
                Float vmin = new Float(65535), vmax = new Float(0), vtot = new Float(0);
                for(Long cpm : listeMoyenne) {
                    vmin = Math.min(vmin, cpm);
                    vmax = Math.max(vmax, cpm);
                    vtot += cpm;
                }
                Float vmoyenne1 = vtot / listeMoyenne.size();
                break;
        }


        canvas.drawColor(Color.LTGRAY);
        if( listeMoyenne.size() > 1 ) {
            canvas.drawPath(mPath, pBackground);
        }

        ui.imageView.setImageBitmap(bitmap);
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] == 0) {
            calculs(this.findViewById(android.R.id.content));
        } else {
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
