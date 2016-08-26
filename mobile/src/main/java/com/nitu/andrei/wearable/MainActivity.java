package com.nitu.andrei.wearable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.formatter.AxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends Activity {

    private static final String TAG = "PHN";

    // UI references.
    private TextView mHRView;

    private BroadcastReceiver receiver;
    private boolean receiverRegistered = false;
    private String showPeriod = "day";

    // db
    DatabaseHandler db = new DatabaseHandler(this);

    // chart
    private LineDataSet dataSet;
    private LineData lineData;
    private LineChart chart;
    private boolean setChart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get heartbeats from local database
        final ArrayList<Heartbeat> data = db.getHeartbeats(100, showPeriod);
        Collections.reverse(data);
        chart = (LineChart) findViewById(R.id.chart);

        final List<Entry> entries = new ArrayList<Entry>();

        Heartbeat heartbeat;
        for (int i = 0, n = data.size(); i < n; i++) {
            heartbeat = data.get(i);
            entries.add(new Entry(i, heartbeat.value));
        }

        if (entries.size() > 0) {
            dataSet = new LineDataSet(entries, "Label");
            lineData = new LineData(dataSet);
            lineData.setValueFormatter(new LargeValueFormatter());

            XAxis xAxis = chart.getXAxis();
            xAxis.setValueFormatter(new MyYAxisValueFormatter(data));
            xAxis.setLabelCount(3, true);
            xAxis.setTextSize(8f);

            YAxis leftAxis = chart.getAxisLeft();

            LimitLine ll = new LimitLine(140f, "Critical Blood Pressure");
            ll.setLineColor(Color.RED);
            ll.setLineWidth(1f);
            ll.setTextColor(Color.BLACK);
            ll.setTextSize(8f);
            leftAxis.addLimitLine(ll);

            chart.setData(lineData);
            chart.getAxisLeft().setDrawLabels(false);
            chart.setDescription("");
            chart.getLegend().setEnabled(false);

            chart.moveViewToX(data.size());
            chart.setVisibleXRangeMaximum(15);
            chart.setAutoScaleMinMaxEnabled(true);
            chart.invalidate(); // refresh
            setChart = true;
        }

        mHRView = (TextView) findViewById(R.id.heartRate);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(WearListenerService.PHN_MESSAGE);
                mHRView.setText(s);
                setChartData(100);
            }
        };
    }

    @Override
    protected void onStart() {
        onLive();
        super.onStart();
    }

    @Override
    protected void onStop() {
        offLive();
        super.onStop();
    }

    private void onLive() {
        receiverRegistered = true;
        registerReceiver(receiver, new IntentFilter(WearListenerService.PHN_RESULT));
    }

    private void offLive() {
        receiverRegistered = false;
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {

        }
    }

    private void setChartData(int limit) {
        ArrayList<Heartbeat> data = db.getHeartbeats(limit, showPeriod);
        Collections.reverse(data);

        Heartbeat heartbeat;
        List<Entry> entries = new ArrayList<Entry>();
        for (int i = 0, n = data.size(); i < n; i++) {
            heartbeat = data.get(i);
            entries.add(new Entry(i, heartbeat.value));
        }
        if (entries.size() > 0) {
            if (!setChart) {
                dataSet = new LineDataSet(entries, "Label");
                lineData = new LineData(dataSet);
                lineData.setValueFormatter(new LargeValueFormatter());

                XAxis xAxis = chart.getXAxis();
                xAxis.setValueFormatter(new MyYAxisValueFormatter(data));
                xAxis.setLabelCount(3, true);
                xAxis.setTextSize(8f);

                YAxis leftAxis = chart.getAxisLeft();

                LimitLine ll = new LimitLine(140f, "Critical Blood Pressure");
                ll.setLineColor(Color.RED);
                ll.setLineWidth(1f);
                ll.setTextColor(Color.BLACK);
                ll.setTextSize(8f);
                leftAxis.addLimitLine(ll);

                chart.setData(lineData);
                chart.getAxisLeft().setDrawLabels(false);
                chart.setDescription("");
                chart.getLegend().setEnabled(false);

                chart.moveViewToX(data.size());
                chart.setVisibleXRangeMaximum(15);
                chart.setAutoScaleMinMaxEnabled(true);
                chart.invalidate(); // refresh
                setChart = true;
            } else {
                if (lineData.getDataSetCount() > 0) {
                    lineData.removeDataSet(0);
                }
                LineDataSet dataSet = new LineDataSet(entries, "Label");
                lineData.addDataSet(dataSet);

                chart.getXAxis().setValueFormatter(new MyYAxisValueFormatter(data));
                chart.moveViewToX(data.size());
                chart.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_live) {
            if (receiverRegistered) {
                offLive();
            } else {
                onLive();
            }
            return true;
        }
        if (id == R.id.action_set_day) {
            showPeriod = "day";
            setChartData(100);
            return true;
        }
        if (id == R.id.action_set_month) {
            showPeriod = "month";
            setChartData(100);
            return true;
        }
        if (id == R.id.action_set_year) {
            showPeriod = "year";
            setChartData(100);
            return true;
        }
        if (id == R.id.action_logout) {
            Settings.deleteLoginToken(this);
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class MyYAxisValueFormatter implements AxisValueFormatter {
        ArrayList<Heartbeat> data;

        public MyYAxisValueFormatter(ArrayList<Heartbeat> data) {
            this.data = data;
        }

        public String getFormattedValue(float value, AxisBase axis) {
            // "value" represents the position of the label on the axis (x or y)
            try {
                Heartbeat hb = this.data.get((int) value);
                return hb.created;
            } catch (Exception e) {
                return "0";
            }
        }

        /** this is only needed if numbers are returned, else return 0 */
        public int getDecimalDigits() { return 1; }
    }
}
