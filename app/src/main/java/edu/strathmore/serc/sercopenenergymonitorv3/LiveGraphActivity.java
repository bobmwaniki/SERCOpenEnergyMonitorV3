package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import mehdi.sakout.fancybuttons.FancyButton;

public class LiveGraphActivity extends AppCompatActivity {

    private String link = "";
    private String ROOT_LINK = "";
    private String API_KEY = "";
    private String interval;

    // Time period to fetch data
    private String startTime;
    private String endTime;
    private boolean firstTimeLaunch = true;

    private int stationID;
    private String stationTag;
    private String stationName;

    // Graph
    private LineChart lineChart;
    private Long timeBack = 600000L;
    //private int leftOffset = 0;

    // Repetitive liveData
    private int fetchInterval = 5000;
    private Handler mHandler;
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                generateStartAndEndTime();
                setLink();
                refreshLiveGraphData(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // refreshLiveGraphData() throws an exception
                mHandler.postDelayed(mStatusChecker, fetchInterval);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_graph);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Getting data from received intent to start GraphActivity
        Bundle extras = getIntent().getExtras();
        // Checks that there are extras in the intent
        if (extras != null) {
            stationID = extras.getInt("Station_ID");
            stationTag = extras.getString("Station_tag");
            stationName = extras.getString("Station_name");
        }

        TextView bottomText = (TextView) findViewById(R.id.live_graph_bottom_textview);
        bottomText.setText(stationTag + " - " + stationName);



        // Get root link, API key and interval from settings
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        ROOT_LINK = appSettings.getString("root_link_editpref", "");
        API_KEY = appSettings.getString("api_key_edit","");
        interval = appSettings.getString("pref_interval", "900");

        // Get the fetch interval from settings
        fetchInterval = Integer.valueOf(appSettings.getString("pref_update_frequency", "5")) * 1000;


        // Get LineChart
        lineChart = (LineChart) findViewById(R.id.full_page_live_graph);

        // Draw the graph for on lauch
        generateStartAndEndTime();
        setLink();
        drawLiveGraph(link);

        // Start repeating task
        mHandler = new Handler();
        startRepeatingTask();

       /* FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab) ;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                *//*generateStartAndEndTime();
                setLink();
                refreshLiveGraphData();*//*

                //timerHandler.postDelayed(timerRunnable, fetchInterval);

                mHandler = new Handler();
                startRepeatingTask();
            }
        });*/


        // OnClickListeners for the buttons on top of the graph
        FancyButton oneHourBack = (FancyButton) findViewById(R.id.live_graph_1_hour_back);
        FancyButton thirtyMinBack = (FancyButton) findViewById(R.id.live_graph_30_min_back);
        FancyButton fifteenMinBack = (FancyButton) findViewById(R.id.live_graph_15_min_back);
        FancyButton tenMinBack = (FancyButton) findViewById(R.id.live_graph_10_min_back);
        FancyButton oneMinBack = (FancyButton) findViewById(R.id.live_graph_1_min_back);


        /******* Actions for the the buttons *****/
        oneHourBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redrawGraphForUserTimeBack(60);
            }
        });

        thirtyMinBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redrawGraphForUserTimeBack(30);
            }
        });

        fifteenMinBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redrawGraphForUserTimeBack(15);
            }
        });

        tenMinBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redrawGraphForUserTimeBack(10);
            }
        });

        oneMinBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redrawGraphForUserTimeBack(1);
            }
        });





    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //return super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_live_graph, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            /*case R.id.full_screen_live_graph:

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    hideSystemUI();
                    getSupportActionBar().hide();
                } else{
                    getSupportActionBar().hide();
                }c
                return true;*/

            case R.id.action_reset_zoom_live_graph:
                lineChart.fitScreen();
                return true;

            case R.id.action_settings:
                Intent openSettings = new Intent(LiveGraphActivity.this, SettingsActivity.class);
                openSettings.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GraphPreferenceFragment.class.getName());
                openSettings.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                startActivity(openSettings);
                return true;
        }





        return super.onOptionsItemSelected(item);
    }


    // Method used to call refresh data repeatedly
    void startRepeatingTask() {
        mStatusChecker.run();
    }

    // Method used to stop the Handler
    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    // Method used to convert minutes to milliseconds
    private Long minutesToMilliseconds(int minutes){
        return (minutes * 60000L);
    }

    // Method used to get the current time from the device and return that UNIX time as a String
    public String getCurrentTimeAsString(){
        // Getting the current time from the system clock in milliseconds
        Long currentTimeMillis = System.currentTimeMillis();
        // Setting the current time as now and the start time as one week from that date
        String currentTime = String.valueOf(currentTimeMillis);

        return currentTime;

    }

    // Method used to generate appropriate start and end times for link
    private void generateStartAndEndTime(){
        // If the activity is being created, the start time will be the current time and end time
        // will be some time back that is set in the timeback variable. By default this is 10 min
        if (firstTimeLaunch) {
            endTime = getCurrentTimeAsString();
            startTime = String.valueOf(Long.valueOf(endTime) - timeBack);
            firstTimeLaunch = false;
        }
        else{
            // This is to make sure the API calls are for time ranges that have not been already checked
            startTime = endTime;
            endTime = getCurrentTimeAsString();

        }

    }

    // Helper method for the buttons on the top of the Live Page Activity
    private void redrawGraphForUserTimeBack(int minutes){
        // Set Time Back
        timeBack = minutesToMilliseconds(minutes);
        endTime = getCurrentTimeAsString();
        startTime = String.valueOf(Long.valueOf(endTime) - timeBack);
        // Redraw Graph
        setLink();
        drawLiveGraph(link);
    }

    // Method used to refresh the data for the graph if the time range is not changing
    private void refreshLiveGraphData(){

        new CmsApiCall(getBaseContext(), new CmsApiCall.AsyncResponse() {
            @Override
            public void processFinish(String output) throws JSONException {
                JSONArray jsonArray = new JSONArray(output);
                // Checks that the array is not empty
                if (!jsonArray.isNull(0) && jsonArray.length()>0) {
                    // Get a list of Entry objects from the output
                    List<Entry> latestEntries = jsonToEntryList(output, true);

                    // Makes sure there is new data
                    if (latestEntries.size()> 0){

                        Log.i("SERC Log", String.valueOf(latestEntries.size()) + " entries to add");

                        // Get the lineData object
                        LineData currentLineData = lineChart.getLineData();
                        // Since there's only one data set, it will be at position 0
                        ILineDataSet currentLineDataSet = currentLineData.getDataSetByIndex(0);

                        for (int k=0; k<currentLineDataSet.getEntryCount();k++){
                            Log.i("SERC Log", "Current Entries: " + currentLineDataSet.getEntryForIndex(k).toString());
                        }

                        // Add all the new entries from the list of Entry objects
                        for(int j=0; j<latestEntries.size(); j++){
                            currentLineDataSet.addEntry(latestEntries.get(j));
                            Log.i("SERC Log", "New entry added: " + latestEntries.get(j).toString());
                        }
                        currentLineData.notifyDataChanged(); // Notify LineData of the changes

                        // Remove the oldest entries according to how many Entry objects have been added.
                        // This ensures that the number of data points in the graph remain the same
                        for(int j=0; j<latestEntries.size(); j++){
                            currentLineDataSet.removeFirst(); // Notify LineData of the changes
                        }
                        currentLineData.notifyDataChanged();

                        // Notify the LineChart of the changes
                        lineChart.notifyDataSetChanged();
                        // Move the ViewPoint to the first entry in the current List and refresh the chart.
                        // This ViewPoint keeps on changing because of the old entries are removed
                        lineChart.moveViewToX(currentLineDataSet.getEntryForIndex(0).getX());

                    }

                }

            }
        }).execute(link);


    }

    // Method used to draw the initial graph or when the time range changes
    public void drawLiveGraph(final String liveGraphLink){

        new EmonCmsApiCall(this, new EmonCmsApiCall.AsyncResponse() {
            @Override
            public void processFinish(String output) throws JSONException {
                JSONArray jsonArray = new JSONArray(output);
                // Check the JSON array is not empty
                if (!jsonArray.isNull(0) && jsonArray.length()>0) {

                    SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

                    // Get a list of Entry objects from the output
                    List<Entry> entries = jsonToEntryList(output, false);

                    // The following steps are done to prepare for the new data on the graph
                    lineChart.clear(); // Remove all old points
                    lineChart.invalidate(); //refresh the data
                    lineChart.fitScreen();  // set the zoom level back to the default


                /* DataSet objects hold data which belongs together, and allow individual styling
                 * of that data. For example, below the color of the line set to RED (by default)
                 * and the drawing of individual circles for each data point is turned off.
                 */
                    LineDataSet dataSet = new LineDataSet(entries, "Power");

                    // Create line data from the stylised LineDataSet
                    LineData lineData = new LineData(styleGraphFromSettings(dataSet)) ;

                    // Sets the LineData object to the LineChart object lineChart that is part of the view
                    lineChart.setData(lineData);
                    lineChart.notifyDataSetChanged();
                    boolean disableAnimation = appSettings.getBoolean("pref_general_disable_animations", false);
                    if (!disableAnimation) {
                        lineChart.animateX(1000);
                    }
                }
                else{
                    // If JSON array is empty, show the user that there is no data
                    Toast.makeText(getBaseContext(), "No data in server", Toast.LENGTH_LONG).show();
                }

            }
        }).execute(liveGraphLink);

    }


    // Creates a list of Entry objects to be used by the LineChart from a String contain the JSON
    // reply form the Emoncms server
    public List<Entry> jsonToEntryList (String jsonString, boolean isRefreshData) throws JSONException {

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        /* For this call, the JSON consists of one large parent JSON Array with multiple children
         * arrays each containing 2 data points. The time at position 0 and the power reading at
         * position 1 in the child arrays.
         */
        JSONArray parentJSON = new JSONArray(jsonString);
        JSONArray childJSONArray;


        // Array list of entry objects needed that will be used by the LineDataSet object
        List<Entry> entries = new ArrayList<>();


        // First make sure the jsonString is not empty
        if (!parentJSON.isNull(0) && parentJSON.length()>0){

            /**
             * The entry object can only take floats. For the x Axis which has timestamps, the
             * precision is lost when it converted from Long to Float (and back to long again in
             * DayAxisValueFormatter when converting the UNIX time back to human readable format).
             * This leads to instances where the value reading appear on the same timestamp on the graph
             * (undesirable). To counter this, since the timestamps are arranged in ascending order
             * the first is stored in settings and the differences to this first timestamp (which
             * are much smaller compared to the full timestamp, hence no loss in precision) are
             * stored in the entries. This is the "reference_timestamp". In DayAxisValueFormatter
             * the"reference_timestamp" is added back to these differences to get back the correct
             * timestamp.
             */



            boolean firstLoop = true;
            Long reference_timestamp = 0L;


            /**
             * This checks if the data is for refreshing the graph. If so, then there is already an
             * existing reference timestamp and creating a new one would mean that the refresh data
             * would appear at the start of the graph rather than the end.
             */
            if(isRefreshData){

                reference_timestamp = appSettings.getLong("reference_timestamp",0L);

                for (int i = 0; i < parentJSON.length(); i++) {
                    childJSONArray = parentJSON.getJSONArray(i);

                    String xValue = childJSONArray.getString(0);
                    xValue = String.valueOf (Long.valueOf(xValue) - reference_timestamp);

                    entries.add(new Entry(Float.parseFloat(xValue) , Float.parseFloat(childJSONArray.getString(1)) ));
                }


            }
            else{

                for (int i = 0; i < parentJSON.length(); i++) {
                    // Take position i in the large array, which should contain the timestamp and value
                    childJSONArray = parentJSON.getJSONArray(i);


                    // Checks if its the first loop to store the first time stamp as the "reference_timestamp"
                    if (firstLoop) {
                        String xValue = childJSONArray.getString(0);
                        reference_timestamp = Long.valueOf(xValue);

                        // Save settings
                        SharedPreferences.Editor editor = appSettings.edit();
                        editor.putLong("reference_timestamp", reference_timestamp);
                        editor.apply();

                        entries.add(new Entry(0 , Float.parseFloat(childJSONArray.getString(1)) ));

                        firstLoop = false;

                    } else {
                        // Add the values to the X and Y axis ArrayLists
                        String xValue = childJSONArray.getString(0);
                        xValue = String.valueOf (Long.valueOf(xValue) - reference_timestamp);


                        entries.add(new Entry(Float.parseFloat(xValue) , Float.parseFloat(childJSONArray.getString(1)) ));


                    }

                }

            }
        }


        return entries;
    }

    // Method used to style the graph
    public LineDataSet styleGraphFromSettings(LineDataSet dataSet){
        // Get the applications settings
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Gets the preference for whether or not the grid will be drawn
        boolean toDrawGrid = appSettings.getBoolean("graph_draw_grid_pref", true);
        // For the X Axis grid
        lineChart.getXAxis().setDrawGridLines(toDrawGrid);
        // For both sets of the Y axis
        lineChart.getAxisLeft().setDrawGridLines(toDrawGrid);
        lineChart.getAxisRight().setDrawGridLines(toDrawGrid);

        // Gets the x axis
        XAxis styledXAxis = lineChart.getXAxis();
        // Sets the x axis labels to appear in the according to settings
        int xAxisLabelPosition = Integer.valueOf(appSettings.getString("graph_x_axis_position_listpref","1"));
        switch (xAxisLabelPosition){
            case 1:
                styledXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                break;
            case 2:
                styledXAxis.setPosition(XAxis.XAxisPosition.TOP);
                break;
            case 3:
                styledXAxis.setPosition(XAxis.XAxisPosition.BOTH_SIDED);
                break;

        }


        // Sets the rotation angle of the x axis labels
        String xAxisAngle = appSettings.getString("graph_x_axis_angle_listpref", "45");
        styledXAxis.setLabelRotationAngle(Float.valueOf(xAxisAngle));
        int userChoice = Integer.valueOf(appSettings.getString("graph_x_axis_time_date", "3")) ;
        // Checks if screen size
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Calculate screen size
        float yInches= metrics.heightPixels/metrics.ydpi;
        float xInches= metrics.widthPixels/metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches*xInches + yInches*yInches);

        if(Float.valueOf(xAxisAngle) == 0f && userChoice ==3){
            if (diagonalInches<5d) {
                styledXAxis.setLabelCount(2, true);
            }
            else if (diagonalInches>5d && diagonalInches<6d) {
                styledXAxis.setLabelCount(3, true);
            } else if (diagonalInches>6d) {
                styledXAxis.setLabelCount(5, true);
            }

        }

        // Removing right Y Axis labels
        YAxis rightYAxis = lineChart.getAxisRight();
        // Get from settings
        boolean allowRightYAxisLabel = appSettings.getBoolean("graph_y_axis_both_sides", false);
        rightYAxis.setDrawLabels(allowRightYAxisLabel);

        // Check if Y Axis should start from zero
        boolean startFromZero = appSettings.getBoolean("pref_live_graph_y_axis_start_from_zero", false);
        if (startFromZero) {
            lineChart.getAxisLeft().setAxisMinimum(0f);
        }

        // Getting the color of the line and setting it
        int graphColor = Integer.valueOf(appSettings.getString("graph_line_color_listpref", "1"));
        switch (graphColor){
            case 1:
                dataSet.setColor(Color.RED);
                dataSet.setCircleColor(Color.RED);
                break;
            case 2:
                dataSet.setColor(Color.CYAN);
                dataSet.setCircleColor(Color.CYAN);
                break;
            case 3:
                dataSet.setColor(Color.BLACK);
                dataSet.setCircleColor(Color.BLACK);
                break;
            case 4:
                dataSet.setColor(Color.BLUE);
                dataSet.setCircleColor(Color.BLUE);
                break;
            case 5:
                dataSet.setColor(Color.GREEN);
                dataSet.setCircleColor(Color.GREEN);
                break;
            case 6:
                dataSet.setColor(Color.MAGENTA);
                dataSet.setCircleColor(Color.MAGENTA);
                break;
            case 7:
                dataSet.setColor(Color.YELLOW);
                dataSet.setCircleColor(Color.YELLOW);
                break;

        }

        // Circles at every data point
        boolean showCircles = appSettings.getBoolean("pref_live_graph_show_circles", false);
        dataSet.setDrawCircles(showCircles);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleRadius(1.6f);
        dataSet.setCircleHoleRadius(1.2f);

        // Make line smoother
        boolean showSmoothline = appSettings.getBoolean("pref_live_graph_smooth_line",true);
        if (showSmoothline) {
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        }
        // Shade underneath the graph
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#d3a303"));
        // Removes the values from the graph
        boolean showDataPoints = appSettings.getBoolean("pref_live_graph_show_data_values", false);
        dataSet.setDrawValues(showDataPoints);



        /**
         * This sets the styling of the x axis according to how it has been defined in the
         * DayAxisValueFormatter class. In this instance, the UNIX timestamp is converted to
         * human readable time in the DayAxisValueFormatter class
         */
        styledXAxis.setValueFormatter(new DayAxisValueFormatter(lineChart, getBaseContext()));

        // Sets the size and position of the graph's legend
        Legend legend = lineChart.getLegend();
        legend.setXEntrySpace(5f);
        legend.setFormSize(5f);
        legend.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);

        // Helps to clear the extra whitespace in the graph
        lineChart.getDescription().setText("");



        return dataSet;

    }



    // This snippet hides the system bars.
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void hideSystemUI() {
        View mDecorView = getWindow().getDecorView();
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showSystemUI() {
        View mDecorView = getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getSupportActionBar().show();
    }

    // Used to update the String URL
    private void setLink(){
        link = ROOT_LINK + "feed/data.json?id=" + String.valueOf(stationID) + "&start=" + startTime + "&end=" + endTime
                + "&interval=" + interval + "&skipmissing=1&limitinterval=1&apikey=" + API_KEY;

    }

    // Stop runnable once activity stops
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
    }

    // Stop runnable once activity stops
    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingTask();
    }

    // Start runnable once activity stops
    @Override
    protected void onResume() {
        super.onResume();
        startRepeatingTask();
    }
}
