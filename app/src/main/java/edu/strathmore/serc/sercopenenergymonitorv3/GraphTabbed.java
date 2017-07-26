package edu.strathmore.serc.sercopenenergymonitorv3;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.util.Calendar;
import java.util.List;

import mehdi.sakout.fancybuttons.FancyButton;

/**
 * This is the Graph Page that is inflated when a user clicks on a location in the main screen. It
 * consists of one Activity and 5 Fragments.
 * The First Fragment (GraphParametersFragment) contains handles the first tab with the graph parameters to be drawn.
 * The second fragment (GraphFragment) contains the MPAndroidChart only in the second tab
 * The other fragments deal with the Time/Date Pickers for the start/end time and date.
 *
 * Note that this activity uses:
 * MPAndroidChart to graph. (https://github.com/PhilJay/MPAndroidChart)
 * Custom buttons, FancyButtons from https://github.com/medyo/fancybuttons as the buttons
 */

public class GraphTabbed extends AppCompatActivity {

    // Used to construct the link that makes the API calls
    String ROOT_LINK = "";
    String API_KEY = "";
    /* For SERC website
    ROOT_LINK = "https://serc.strathmore.edu/emoncms/feed/data.json?id=";
    API_KEY= "36ec19e2a135f22b50883d555eea2114";
    */

    // Placeholder. Not used currently
    /*final static int INTERVAL = 900;*/
    // Interval to poll data
    private String interval;

    // Needed for link and is meant to be changed depending on the values input by the user
    // Made global variable so that they can be changed from any method
    private String startTime = "";
    private String endTime = "";
    private int stationID = 0;

    private String stationName = "";
    private String stationTag = "";
    private String link;


    // Needed for calendar dialog
    private int year_start, year_end, month_start, month_end, day_start, day_end;

    // Needed for time dialog
    private int hour_start, hour_end, minute_start, minute_end;

    // For the MPAndroidChart LineChart that displays the graph
    private LineChart lineChart;


    private AppBarLayout appBarLayout;


    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph_tabbed);

        Toolbar toolbar = (Toolbar) findViewById(R.id.graph_tabbed_toolbar);
        setSupportActionBar(toolbar);

        final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar_graph_tabbed);




        // Create the adapter that will return a fragment for each of the two
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.graph_tabbed_viewpager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        // Used to collapse the toolbar when on the graph tab and expand it on the parameters tab
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                mViewPager.setCurrentItem(position);
                if (position == 0){
                    appBarLayout.setExpanded(true,true);
                }
                else if (position == 1){
                    appBarLayout.setExpanded(false,true);
                }

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });



        // Get API Key and Root Link from settings
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        ROOT_LINK = appSettings.getString("root_link_editpref", "");
        ROOT_LINK = ROOT_LINK + "feed/data.json?id=";
        API_KEY = appSettings.getString("api_key_edit","");

        //Get interval from settings
        interval = appSettings.getString("pref_interval", "900");

        //Getting data from received intent to start GraphActivity
        Bundle extras = getIntent().getExtras();
        // Checks that there are extras in the intent
        if (extras != null) {
            stationID = extras.getInt("Station_ID");
            stationTag = extras.getString("Station_tag");
            stationName = extras.getString("Station_name");
            Log.i("StationExtras in intent", "Station_ID" + String.valueOf(stationID) +
                    "Station_Tag" + String.valueOf(stationTag)+"Station_Name" + String.valueOf(stationName));
        }


        /* Setting today's date and time as default values when the calendar dialog first shows up
         * Otherwise this will default to 01 Jan 1970 (UNIX = 0) and result in a lot of swiping for
         * the user to get to today's date
         */
        Calendar cal;
        // Setting the date for the calendar dialog to be today
        cal = Calendar.getInstance(); // Get current time on the device
        // Sets the end date variables to be current time on the device
        year_end = cal.get(Calendar.YEAR);
        month_end = cal.get(Calendar.MONTH);
        day_end = cal.get(Calendar.DAY_OF_MONTH);



        // Sets the current time of the device as the start and end time variables
        hour_start = cal.get(Calendar.HOUR_OF_DAY);
        hour_end = cal.get(Calendar.HOUR_OF_DAY);
        minute_start = cal.get(Calendar.MINUTE);
        minute_end = cal.get(Calendar.MINUTE);


        /**
         * When the user presses the "Draw Graph" button without setting a custom start/end date and/or
         * time, the app should draw the graph for the past week. Because of this, the default values
         * for the UNIX start time should be a week from the device's current time. As exactly one week
         * from the device's current time will have the same time variables, only the date variables
         * need to be changed
         */
        // Getting the current time from the system clock in milliseconds
        Long tsLong = System.currentTimeMillis();
        // Setting the current time as now and the start time as one week from that date
        endTime = tsLong.toString();
        String defDays = appSettings.getString("graph_default_duration_listpref", "7");
        startTime = String.valueOf(Long.parseLong(endTime) - (Long.parseLong(defDays))*86400000L); //86,400,000 is one day in milliseconds


        // Setting the date for the calendar dialog to be a week from today
        cal.setTimeInMillis(Long.parseLong(startTime));
        day_start = cal.get(Calendar.DAY_OF_MONTH);
        month_start = cal.get(Calendar.MONTH);
        year_start = cal.get(Calendar.YEAR);


        // Updating the link to include the change in new UNIX start time and end time
        setLink();

    }


    // Method for drawing the graph. Requires the HTTP link to the JSON file
    private void drawGraph(String graphLink){
        Log.i("SERC Log", "link: " + graphLink);

        // Graph Drawn in onPostExecute to avoid out of sync data
        new EmonCmsApiCall(GraphTabbed.this, new EmonCmsApiCall.AsyncResponse() {
            @Override
            public void processFinish(String output) throws JSONException {

                // Get app settings
                SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(GraphTabbed.this);
                // First update the link
                interval = appSettings.getString("pref_interval", "900"); //get latest interval setting
                setLink();

                // Gets the amount of time before Graph is zeroed from settings
                float minutesInactivity = Float.valueOf(appSettings.getString("graph_zero_listpref", "-1"));

                /* For this call, the JSON consists of one large parent JSON Array with multiple children
                 * arrays each containing 2 data points. The time at position 0 and the power reading at
                 * position 1 in the child arrays.
                 */
                Log.i("SERC Log", "Result in JSON: " + output);
                JSONArray parentJSON = new JSONArray(output);
                JSONArray childJSONArray;


                // Array list of entry objects needed that will be used by the LineDataSet object
                List<Entry> entries = new ArrayList<>();
                // Array list for the values of x (timestamp) and y (power values) coordinates of the graph
                ArrayList<Long> xAxis = new ArrayList<>();
                ArrayList<Double> yAxis = new ArrayList<>();

                /**
                 * This cycles through each JSON array in the main array and adds the element in the first
                 * position (the timestamp in milliseconds) to xAxis array list and the second element in
                 * the array (the power reading ) to the yAxis array list. These 2 ArrayLists are then used
                 * to create an ArrayList of Entry objects stored in the variable entries (i.e. each Entry
                 * object contains the xAxis and yAxis values from one JSON array)
                 */
                // First checks if any data is being sent (i.e. it is not an empty array)

                if(!parentJSON.isNull(0)) {
                    Log.i("SERC Log", "Not null array: Response from API Call not null");
                    boolean firstLoop = true;
                    Long reference_timestamp = 0L;


                    for (int i = 0; i < parentJSON.length(); i++) {
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

                    Log.i("SERC Log", "Adding to entries: Changing the elements of the array into Entry objects");
                    /**
                     * This is used to add the x and y axis values to as Entry objects to an ArrayList.
                     * It also 'zeros' the graph by adding 0 as y axis reading just before and after the
                     * the 2 x axis values the are further apart than the threshold value
                     */
                    Float threshold = minutesInactivity * 60000f; //Converts minutes to milliseconds
                    long previousX = Long.valueOf(startTime);
                    Long absTimeDiff;
                    Long zeroOffset = 1000l;
                    if (threshold > 0f) {
                        // Since the xAxis and yAxis ArrayList are the same length either xAxis.size() or yAxis.size()
                        // could have been used
                        for (int i = 0; i < xAxis.size(); i++) {
                            Long currentX = xAxis.get(i);
                            absTimeDiff = Math.abs(currentX - previousX);


                            if(absTimeDiff>threshold){

                                entries.add(new Entry((float) (previousX+zeroOffset), 0f));
                                entries.add(new Entry((float) (currentX-zeroOffset), 0f));
                                entries.add(new Entry((float) xAxis.get(i), yAxis.get(i).floatValue()));

                            } else{
                                entries.add(new Entry((float) xAxis.get(i), yAxis.get(i).floatValue()));
                            }


                            previousX = currentX;
                        }
                    } else{
                        for (int i = 0; i < xAxis.size(); i++) {
                            entries.add(new Entry((float) xAxis.get(i), yAxis.get(i).floatValue()));
                        }
                    }




                    // The following steps are done to prepare for the new data on the graph
                    lineChart.clear();
                    lineChart.invalidate(); //refresh the data
                    lineChart.fitScreen();  // set the zoom level back to the default

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


                    // Checks if screen size
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    // Calculate screen size
                    float yInches= metrics.heightPixels/metrics.ydpi;
                    float xInches= metrics.widthPixels/metrics.xdpi;
                    double diagonalInches = Math.sqrt(xInches*xInches + yInches*yInches);

                    int userChoice = Integer.valueOf(appSettings.getString("graph_x_axis_time_date", "3")) ;
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

                /* DataSet objects hold data which belongs together, and allow individual styling
                 * of that data. For example, below the color of the line set to RED (by default) and
                 * the drawing of individual circles for each data point is turned off.
                 */
                    Log.i("SERC Log", "Configuring the Data Set");
                    LineDataSet dataSet = new LineDataSet(entries, "Power");


                    // Getting the color of the line and setting it
                    int graphColor = Integer.valueOf(appSettings.getString("graph_line_color_listpref", "1"));
                    int currentLineColor = Color.RED;
                    switch (graphColor){
                        case 1:
                            currentLineColor = Color.RED;
                            break;
                        case 2:
                            currentLineColor = Color.CYAN ;
                            break;
                        case 3:
                            currentLineColor = Color.BLACK;
                            break;
                        case 4:
                            currentLineColor = Color.BLUE;
                            break;
                        case 5:
                            currentLineColor = Color.GREEN;
                            break;
                        case 6:
                            currentLineColor = Color.MAGENTA;
                            break;
                        case 7:
                            currentLineColor = Color.YELLOW;
                            break;

                    }
                    dataSet.setColor(currentLineColor);

                    // Fill color underneath the graph
                    int fillColor = Integer.valueOf(appSettings.getString("graph_fill_color_listpref","1"));
                    if (fillColor>1) {
                        dataSet.setDrawFilled(true);
                    }
                    switch (fillColor){
                        case 2:
                            dataSet.setFillColor(Color.parseColor("#d3a303")); //Orange
                            break;
                        case 3:
                            dataSet.setFillColor(Color.CYAN);
                            break;
                        case 4:
                            dataSet.setFillColor(Color.BLUE);
                            break;
                        case 5:
                            dataSet.setFillColor(Color.GREEN);
                            break;
                        case 6:
                            dataSet.setFillColor(Color.MAGENTA);
                            break;
                        case 7:
                            dataSet.setFillColor(Color.YELLOW);
                            break;
                    }

                    // Make line smoother
                    boolean showSmoothline = appSettings.getBoolean("pref_graph_smoothen_line",true);
                    if (showSmoothline) {
                        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    }


                    // Line Style
                    int lineStyle = Integer.valueOf(appSettings.getString("pref_graph_line_style", "1"));
                    switch (lineStyle){
                        case 0:
                            dataSet.setDrawCircles(false);
                            dataSet.setDrawValues(false);
                            break;

                        case 1:
                            dataSet.setDrawCircles(false);
                            dataSet.setDrawValues(true);
                            break;

                        case 2:
                            dataSet.setDrawCircles(true);
                            dataSet.setDrawCircleHole(true);
                            dataSet.setCircleRadius(1.6f);
                            dataSet.setCircleHoleRadius(1.2f);
                            dataSet.setCircleColor(currentLineColor);
                            dataSet.setDrawValues(false);
                            break;

                        case 3:
                            dataSet.setDrawCircles(true);
                            dataSet.setDrawCircleHole(true);
                            dataSet.setCircleRadius(1.6f);
                            dataSet.setCircleHoleRadius(1.2f);
                            dataSet.setCircleColor(currentLineColor);
                            dataSet.setDrawValues(true);
                            break;

                    }



                /* As a last step, one needs to add the LineDataSet object (or objects) that were created
                 * to a LineData object. This object holds all data that is represented by a Chart
                 * instance and allows further styling.
                 */
                    LineData lineData = new LineData(dataSet);

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



                    // Sets the LineData object to the LineChart object lineChart that is part of the view
                    lineChart.setData(lineData);
                    lineChart.notifyDataSetChanged();

                }
                lineChart.invalidate(); //refresh

                // Move to the graph tab automatically
                mViewPager.setCurrentItem(1, true);

                // Animate Y axis
                boolean disableAnimation = appSettings.getBoolean("pref_general_disable_animations", false);
                if (!disableAnimation) {
                    lineChart.animateY(1000);
                }

            }
        }).execute(graphLink);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_graph_tabbed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Switch statement to handle the menu clicks
        switch (id){
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_settings:
                Intent openSettings = new Intent(this, SettingsActivity.class);
                // Open to graph Settings without showing the other categories
                openSettings.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GraphPreferenceFragment.class.getName());
                openSettings.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                startActivity(openSettings);

                return true;

            case R.id.action_reset_zoom:
                lineChart.fitScreen();
                return true;

          /*  case R.id.action_disable_swipe:
                if (item.isChecked()) {
                    item.setChecked(false);
                } else{
                    item.setChecked(true);
                }
            */
        }


        return super.onOptionsItemSelected(item);
    }

    // Fragment for the First Page of the Graph with date and time settings
    public static class GraphParametersFragment extends Fragment{

        // Empty constructor
        public GraphParametersFragment(){}

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            //return super.onCreateView(inflater, container, savedInstanceState);
            final View parametersView = inflater.inflate(R.layout.graph_parameters,container, false);

            // Sets the graph title (TextView at the top of the activity) to be have the title and tag from the intent
            TextView graphHeading = (TextView) parametersView.findViewById(R.id.graph_title);
            SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean switchNameTag = appSettings.getBoolean("pref_general_switch_name_tag", false);
            if (switchNameTag) {
                graphHeading.setText(((GraphTabbed)getActivity()).stationName + " - " + ((GraphTabbed)getActivity()).stationTag);
            } else {
                graphHeading.setText(((GraphTabbed)getActivity()).stationTag + " - " + ((GraphTabbed)getActivity()).stationName);
            }

            // OnClickListener for "Set Start Date" button to show DatePicker Dialog
            FancyButton calStart = (FancyButton) parametersView.findViewById(R.id.btn_set_start_date);
            calStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogFragment newFragment = new DatePickerStartFragment();
                    newFragment.show(getFragmentManager(), "datePickerStart");
                }
            });

            // OnClickListener for "Set End Date" button to show DatePicker Dialog
            FancyButton calEnd = (FancyButton) parametersView.findViewById(R.id.btn_set_end_date);
            calEnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogFragment newFragment = new DatePickerEndFragment();
                    newFragment.show(getFragmentManager(), "datePickerEnd");
                }
            });

            // OnClickListener for "Set Start Time" button to show TimePicker Dialog
            FancyButton timeStart = (FancyButton) parametersView.findViewById(R.id.btn_set_start_time);
            timeStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogFragment newFragment = new TimePickerStartFragment();
                    newFragment.show(getFragmentManager(), "timePickerStart");
                }
            });

            // OnClickListener for "Set End Time" button to show TimePicker Dialog
            FancyButton timeEnd = (FancyButton) parametersView.findViewById(R.id.btn_set_end_time);
            timeEnd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DialogFragment newFragment = new TimePickerEndFragment();
                    newFragment.show(getFragmentManager(), "timePickerEnd");
                }
            });



            // OnClickListener for the "Draw Graph" button
            FancyButton drawGraphBtn = (FancyButton) parametersView.findViewById(R.id.btn_draw_graph_tabbed);
            drawGraphBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Find each TextView
                    TextView startTimeText = (TextView) parametersView.findViewById(R.id.textview_set_start_time);
                    TextView endTimeText = (TextView) parametersView.findViewById(R.id.textview_set_end_time);
                    TextView startDateText = (TextView) parametersView.findViewById(R.id.textview_set_start_date);
                    TextView endDateText = (TextView) parametersView.findViewById(R.id.textview_set_end_date);
                    // Set each TextView
                    // Note that the months have a '+1' as they start from month 0 not month 1
                    startDateText.setText(((GraphTabbed)getActivity()).day_start + "/" +(((GraphTabbed)getActivity()).month_start+1) + "/" + ((GraphTabbed)getActivity()).year_start);
                    endDateText.setText(((GraphTabbed)getActivity()).day_end + "/" + (((GraphTabbed)getActivity()).month_end+1) + "/" +((GraphTabbed)getActivity()).year_end);
                    startTimeText.setText(((GraphTabbed)getActivity()).hour_start+":"+((GraphTabbed)getActivity()).minute_start+"hrs");
                    endTimeText.setText(((GraphTabbed)getActivity()).hour_end+":"+((GraphTabbed)getActivity()).minute_end+"hrs");

                    // This is to check that the chosen start date/time does not come after the end date/time
                    if ( (Long.parseLong( ((GraphTabbed)getActivity()).startTime)) < (Long.parseLong( ((GraphTabbed)getActivity()).endTime))) {
                        // Clear previously drawn chart
                        ((GraphTabbed)getActivity()).lineChart.clear();
                        ((GraphTabbed)getActivity()).lineChart.invalidate();
                        // Draw Graph
                        ((GraphTabbed)getActivity()).setLink();
                        ((GraphTabbed)getActivity()).drawGraph(((GraphTabbed)getActivity()).link);

                    }
                    else {
                        // Show error message
                        Toast.makeText(getContext(), "Start date/time cannot come after End date/time", Toast.LENGTH_LONG).show();
                    }



                }
            });

            // Set Help Text to show the default duration when "Draw Graph" is pressed
            String defDays = appSettings.getString("graph_default_duration_listpref", "7");
            TextView helpText = (TextView) parametersView.findViewById(R.id.help_text_draw_graph);

            helpText.setText("Pressing \'Draw Graph\' without setting start or end date/time will " +
                    "show values for the last " + defDays + " days.");


            // Returning the View needed for onCreateView()
            return parametersView;
        }


    }

    // Fragment for the second tab containing the actual graph
    public static class GraphFragment extends Fragment{

        // Empty constructor
        public GraphFragment(){}

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            // Get the xml layout file for the fragment and store it as a view
            View graphOnlyView = inflater.inflate(R.layout.graph_only,container, false);
            // Set the global varible to the graph as the fragment is created
            ((GraphTabbed)getActivity()).lineChart = (LineChart) graphOnlyView.findViewById(R.id.graph_full_page);
            // Return the View
            return graphOnlyView;
        }
    }


    // Handles the behaviour for the DatePicker Dialog window for Start Date
    public static class DatePickerStartFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            /* Use the last date chosen as the date that is selected when the dialog pops up. This is
             * stored in the global variables year_start, month_start and dat_start. If it is the first
             * time the dialog is being launched, it will default the day a week from now
             */
            int year = ((GraphTabbed)getActivity()).year_start;
            int month = ((GraphTabbed)getActivity()).month_start;
            int day = ((GraphTabbed)getActivity()).day_start;

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        // Excuted if user clicks 'Ok' on the dialog
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

            // Show chosen date in TextView
            String dateText = String.valueOf(dayOfMonth) + "/" + String.valueOf(month+1) + "/"
                    + String.valueOf(year);
            TextView startDateText = (TextView) getActivity().findViewById(R.id.textview_set_start_date);
            startDateText.setText(dateText);



            // Get start time from activity
            int hourStart = ((GraphTabbed)getActivity()).hour_start;
            int minuteStart = ((GraphTabbed)getActivity()).minute_start;
            // Setting the UNIX timestamp that will be sent in the link for startTime
            Calendar chosenStart = Calendar.getInstance();
            chosenStart.set(year, month, dayOfMonth, hourStart, minuteStart);
            ((GraphTabbed)getActivity()).startTime = String.valueOf(chosenStart.getTimeInMillis());
            //setLink();

            //Set the global variables to current date chosen
            ((GraphTabbed)getActivity()).day_start = dayOfMonth;
            ((GraphTabbed)getActivity()).month_start = month;
            ((GraphTabbed)getActivity()).year_start = year;

        }
    }

    // Handles the behaviour for the DatePicker Dialog window for End Date
    public static class DatePickerEndFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            /* Use the last date chosen as the date that is selected when the dialog pops up. This is
             * stored in the global variables year_end, month_end and day_end. If it is the first
             * time the dialog is being launched, it will default to the current date.
             */
            int year = ((GraphTabbed)getActivity()).year_end;
            int month = ((GraphTabbed)getActivity()).month_end;
            int day = ((GraphTabbed)getActivity()).day_end;

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        // Executed if user clicks 'Ok' on the dialog
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {

            // Show chosen date in TextView
            String dateText = String.valueOf(dayOfMonth) + "/" + String.valueOf(month+1) + "/"
                    + String.valueOf(year);
            TextView endDateText = (TextView) getActivity().findViewById(R.id.textview_set_end_date);
            endDateText.setText(dateText);


            // Get start time from activity
            int hourEnd = ((GraphTabbed)getActivity()).hour_end;
            int minuteEnd = ((GraphTabbed)getActivity()).minute_end;
            // Setting the UNIX timestamp that will be sent in the link for startTime
            Calendar chosenStart = Calendar.getInstance();
            chosenStart.set(year, month, dayOfMonth, hourEnd, minuteEnd);
            ((GraphTabbed)getActivity()).endTime = String.valueOf(chosenStart.getTimeInMillis());
            //setLink();

            //Set the global variables to current date chosen
            ((GraphTabbed)getActivity()).day_end = dayOfMonth;
            ((GraphTabbed)getActivity()).month_end = month;
            ((GraphTabbed)getActivity()).year_end = year;

        }
    }

    // Handles the behaviour for the TimePicker Dialog for Start Time
    public static class TimePickerStartFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            /* Use the last time chosen as the time that is selected when the dialog pops up. This is
             * stored in the global variables hour_start and minute_start. If it is the first time
             * the dialog is being launched, it will default to the current time.
             */
            int hour = ((GraphTabbed)getActivity()).hour_start;
            int minute = ((GraphTabbed)getActivity()).minute_start;


            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Show Chosen Time in TextView
            String timeText = String.valueOf(hourOfDay) + ":" + String.valueOf(minute) + " hrs";
            TextView startTimeText = (TextView) getActivity().findViewById(R.id.textview_set_start_time);
            startTimeText.setText(timeText);

            // Get Date from Activity
            int day = ((GraphTabbed)getActivity()).day_start;
            int month = ((GraphTabbed)getActivity()).month_start;
            int year = ((GraphTabbed)getActivity()).year_start;
            // Setting the UNIX timestamp that will be sent in the link for startTime
            Calendar chosenStart = Calendar.getInstance();
            chosenStart.set(year, month, day, hourOfDay, minute);
            ((GraphTabbed)getActivity()).startTime = String.valueOf(chosenStart.getTimeInMillis());
            //setLink();

            // Set the global variable to time chosen
            ((GraphTabbed)getActivity()).hour_start = hourOfDay;
            ((GraphTabbed)getActivity()).minute_start = minute;

        }
    }

    // Handles the behaviour for the TimePicker Dialog for End Time
    public static class TimePickerEndFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            /* Use the last time chosen as the time that is selected when the dialog pops up. This is
             * stored in the global variables hour_end and minute_end. If it is the first time
             * the dialog is being launched, it will default to the current time.
             */
            int hour = ((GraphTabbed)getActivity()).hour_end;
            int minute = ((GraphTabbed)getActivity()).minute_end;


            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
        }

        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Show Chosen Time in TextView
            String timeText = String.valueOf(hourOfDay) + ":" + String.valueOf(minute) + " hrs";
            TextView endTimeText = (TextView) getActivity().findViewById(R.id.textview_set_end_time);
            endTimeText.setText(timeText);

            // Get Date from Activity
            int day = ((GraphTabbed)getActivity()).day_end;
            int month = ((GraphTabbed)getActivity()).month_end;
            int year = ((GraphTabbed)getActivity()).year_end;
            // Setting the UNIX timestamp that will be sent in the link for endTime
            Calendar chosenStart = Calendar.getInstance();
            chosenStart.set(year, month, day, hourOfDay, minute);
            ((GraphTabbed)getActivity()).endTime = String.valueOf(chosenStart.getTimeInMillis());
            //setLink();

            // Set the global variable to time chosen
            ((GraphTabbed)getActivity()).hour_end = hourOfDay;
            ((GraphTabbed)getActivity()).minute_end = minute;

        }
    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).

            switch (position){
                case 0:
                    return new GraphParametersFragment();
                case 1:
                    return new GraphFragment();

            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        // Change the tab titles here
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Parameters";
                case 1:
                    return "Graph";
            }
            return null;
        }
    }

    // Used to update the String URL
    private void setLink(){
        link = ROOT_LINK + String.valueOf(stationID) + "&start=" + startTime + "&end=" + endTime
                + "&interval=" + interval + "&skipmissing=1&limitinterval=1&apikey=" + API_KEY;

    }




}
