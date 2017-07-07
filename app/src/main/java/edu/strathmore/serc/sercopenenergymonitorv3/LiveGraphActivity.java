package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    // Repetitive liveData
    private Handler timerHandler;
    private int intervalBalance = 500;
    private int fetchInterval;
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            setLink();
            drawLiveGraph(link);
            /*generateStartAndEndTime();
            setLink();*/
            timerHandler.postDelayed(timerRunnable, intervalBalance);
        }
    } ;

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

        timerHandler = new Handler();

        // Get root link, API key and interval from settings
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        ROOT_LINK = appSettings.getString("root_link_editpref", "");
        API_KEY = appSettings.getString("api_key_edit","");
        interval = appSettings.getString("pref_interval", "900");

        // Get the fetch interval from settings
        fetchInterval = Integer.valueOf(appSettings.getString("pref_update_frequency", "5")) * 1000 - intervalBalance;



        // Get LineChart
        lineChart = (LineChart) findViewById(R.id.full_page_live_graph);

        generateStartAndEndTime();
        setLink();
        drawLiveGraph(link);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab) ;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateStartAndEndTime();
                setLink();
                refreshLiveGraphData();

                //timerHandler.postDelayed(timerRunnable, fetchInterval);
            }
        });


        // OnClickListeners for the buttons on top of the graph
        FancyButton oneHourBack = (FancyButton) findViewById(R.id.live_graph_1_hour_back);
        FancyButton thirtyMinBack = (FancyButton) findViewById(R.id.live_graph_30_min_back);
        FancyButton fifteenMinBack = (FancyButton) findViewById(R.id.live_graph_15_min_back);
        FancyButton tenMinBack = (FancyButton) findViewById(R.id.live_graph_10_min_back);
        FancyButton oneMinBack = (FancyButton) findViewById(R.id.live_graph_1_min_back);

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

    private Long minutesToMilliseconds(int minutes){
        return (minutes * 60000L);
    }

    private void redrawGraphForUserTimeBack(int minutes){
        // Set Time Back
        timeBack = minutesToMilliseconds(minutes);
        endTime = getCurrentTimeAsString();
        startTime = String.valueOf(Long.valueOf(endTime) - timeBack);
        // Redraw Graph
        setLink();
        drawLiveGraph(link);
    }

    private void refreshLiveGraphData(){

        new CmsApiCall(getBaseContext(), new CmsApiCall.AsyncResponse() {
            @Override
            public void processFinish(String output) throws JSONException {
                JSONArray jsonArray = new JSONArray(output);
                if (!jsonArray.isNull(0) && jsonArray.length()>0) {
                    // Get a list of Entry objects from the output
                    List<Entry> latestEntries = jsonToEntryList(output);

                    // Makes sure there is new data
                    if (latestEntries.size()> 0){
                        LineData currentLineData = lineChart.getLineData();
                        ILineDataSet currentLineDataSet = currentLineData.getDataSetByIndex(0);

                        if (currentLineDataSet != null) {
                            // Add new entries at the end of the list
                            for (Entry entry:latestEntries){
                                currentLineDataSet.addEntry(entry);
                            }

                            // Remove old data that is being replaced
                            for (int i=0; i<latestEntries.size(); i++){
                                // remove first point in linedata
                                currentLineDataSet.removeFirst();
                            }

                            currentLineData.notifyDataChanged();
                            lineChart.notifyDataSetChanged();
                        }


                    }



                }

            }
        }).execute(link);

        lineChart.invalidate(); //refresh

    }


    public String getCurrentTimeAsString(){
        // Getting the current time from the system clock in milliseconds
        Long currentTimeMillis = System.currentTimeMillis();
        // Setting the current time as now and the start time as one week from that date
        String currentTime = currentTimeMillis.toString();

        return currentTime;

    }

    private void generateStartAndEndTime(){
        // Start and endTime
        if (firstTimeLaunch) {
            endTime = getCurrentTimeAsString();
            startTime = String.valueOf(Long.valueOf(endTime) - timeBack);
            firstTimeLaunch = false;
        }
        else{
            startTime = endTime;
            endTime = getCurrentTimeAsString();

        }

    }

    public void drawLiveGraph(final String liveGraphLink){

        new EmonCmsApiCall(this, new EmonCmsApiCall.AsyncResponse() {
            @Override
            public void processFinish(String output) throws JSONException {
                JSONArray jsonArray = new JSONArray(output);
                if (!jsonArray.isNull(0) && jsonArray.length()>0) {
                    // Get a list of Entry objects from the output
                    List<Entry> entries = jsonToEntryList(output);

                    // The following steps are done to prepare for the new data on the graph
                    lineChart.clear();
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
                }
                else{
                    Toast.makeText(getBaseContext(), "No data in server", Toast.LENGTH_LONG).show();
                }

            }
        }).execute(liveGraphLink);

    }


    // Creates a list of Entry objects to be used by the LineChart from a String contain the JSON
    // reply form the Emoncms server
    public List<Entry> jsonToEntryList (String jsonString) throws JSONException {

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
            Log.i("SERC Log", "Going through the JSON");

            boolean firstLoop = true;
            Long reference_timestamp = 0L;

            for (int i = 0; i < parentJSON.length(); i++) {
                // Take position i in the large array, which should contain the timestamp and value
                childJSONArray = parentJSON.getJSONArray(i);

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


        return entries;
    }


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

        // Removing right Y Axis labels
        YAxis rightYAxis = lineChart.getAxisRight();
        // Get from settings
        boolean allowRightYAxisLabel = appSettings.getBoolean("graph_y_axis_both_sides", false);
        rightYAxis.setDrawLabels(allowRightYAxisLabel);

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
        dataSet.setDrawCircles(true);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleRadius(1.6f);
        dataSet.setCircleHoleRadius(1.2f);

        // Make line smoother
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        // Shade underneath the graph
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#d3a303"));
        // Removes the values from the graph
        dataSet.setDrawValues(false);






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
            case R.id.full_screen_live_graph:

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    hideSystemUI();
                    getSupportActionBar().hide();
                } else{
                    getSupportActionBar().hide();
                }

            case R.id.action_reset_zoom_live_graph:
                lineChart.fitScreen();
                return true;
        }





        return super.onOptionsItemSelected(item);
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

    // Stop runnable once activity pauses
    @Override
    protected void onPause() {
        timerHandler.removeCallbacks(timerRunnable);
        super.onPause();
    }
}
