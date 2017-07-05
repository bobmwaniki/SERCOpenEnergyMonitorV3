package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class LiveGraphActivity extends AppCompatActivity {

    private String link = "";
    private String ROOT_LINK = "";
    private String API_KEY = "";
    private String startTime;
    private String endTime;
    private String interval;

    private int stationID;
    private String stationTag;
    private String stationName;

    // Graph
    private LineChart lineChart;

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

        // Get root link, API key and interval from settings
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        ROOT_LINK = appSettings.getString("root_link_editpref", "");
        API_KEY = appSettings.getString("api_key_edit","");
        interval = appSettings.getString("pref_interval", "900");

        // Start and endTime
        endTime = getCurrentTimeAsString();
        startTime = String.valueOf(Long.valueOf(endTime) - 1200000l);

        // Get LineChart
        lineChart = (LineChart) findViewById(R.id.full_page_live_graph);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab) ;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLink();
                Log.i("SERC Log", "Link: " + link);
                drawLiveGraph(link);
            }
        });


    }

    public String getCurrentTimeAsString(){
        // Getting the current time from the system clock in milliseconds
        Long currentTimeMillis = System.currentTimeMillis();
        // Setting the current time as now and the start time as one week from that date
        String currentTime = currentTimeMillis.toString();

        return currentTime;

    }

    public void drawLiveGraph(final String liveGraphLink){
        new CmsApiCall(getBaseContext(), new CmsApiCall.AsyncResponse() {
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

        /* For this call, the JSON consists of one large parent JSON Array with multiple children
         * arrays each containing 2 data points. The time at position 0 and the power reading at
         * position 1 in the child arrays.
         */
        Log.i("SERC Log", "Server response: " + jsonString);
        JSONArray parentJSON = new JSONArray(jsonString);
        JSONArray childJSONArray;


        // Array list of entry objects needed that will be used by the LineDataSet object
        List<Entry> entries = new ArrayList<>();


        // First make sure the jsonString is not empty
        if (!parentJSON.isNull(0) && parentJSON.length()>0){
            Log.i("SERC Log", "Going through the JSON");
            for (int i = 0; i < parentJSON.length(); i++) {
                // Take position i in the large array, which should contain the timestamp and value
                childJSONArray = parentJSON.getJSONArray(i);

                for (int j = 0; j < childJSONArray.length(); j++) {
                    // Check if value/reading is null and adds 0 if so to avoid NullException error
                    if (childJSONArray.get(1) == null) {
                        /*yAxis.add(0d);
                        xAxis.add(childJSONArray.getLong(0));*/

                        entries.add(new Entry(childJSONArray.getLong(0), 0l ));
                    } else {
                        // Add the values to the X and Y axis ArrayLists
                        entries.add(new Entry(childJSONArray.getLong(0), (float) childJSONArray.getDouble(1)));
                        /*xAxis.add(childJSONArray.getLong(0));
                        yAxis.add(childJSONArray.getDouble(1));*/

                    }
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
                break;
            case 2:
                dataSet.setColor(Color.CYAN);
                break;
            case 3:
                dataSet.setColor(Color.BLACK);
                break;
            case 4:
                dataSet.setColor(Color.BLUE);
                break;
            case 5:
                dataSet.setColor(Color.GREEN);
                break;
            case 6:
                dataSet.setColor(Color.MAGENTA);
                break;
            case 7:
                dataSet.setColor(Color.YELLOW);
                break;

        }

        // Removes circles at every data point
        dataSet.setDrawCircles(false);

        /**
         * This sets the styling of the x axis according to how it has been defined in the
         * DayAxisValueFormatter class. In this instance, the UNIX timestamp is converted to
         * human readable time in the DayAxisValueFormatter class
         */
        styledXAxis.setValueFormatter(new DayAxisValueFormatter(lineChart));

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
            case (R.id.full_screen_live_graph):

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    hideSystemUI();
                    getSupportActionBar().hide();
                } else{
                    getSupportActionBar().hide();
                }
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
}
