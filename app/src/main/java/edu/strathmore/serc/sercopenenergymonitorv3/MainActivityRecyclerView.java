package edu.strathmore.serc.sercopenenergymonitorv3;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;
import rm.com.longpresspopup.LongPressPopup;
import rm.com.longpresspopup.LongPressPopupBuilder;
import rm.com.longpresspopup.PopupInflaterListener;

public class MainActivityRecyclerView extends AppCompatActivity {

    //For the call to be made to the CMS API
    private String rootLinkAddress;
    private String apiKey;
    //apiKey = "36ec19e2a135f22b50883d555eea2114";

    // For the SwipeRefreshLayout used both in the onCreate and refresh method
    private SwipeRefreshLayout swipeRefreshLayout;

    // For the adapter used in the RecyclerView  of the Main Activity/Screen.
    // Needs to be global as it is used both in the onCreate and refresh method
    private RecyclerViewAdapter adapter;

    // Result from EmonCms
    private String resultFromEmonCms = "";

    // Checks if the activity is launching
    private boolean firstTimeLaunch = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_recycler_view);

        // Set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Gets the API key from settings (Shared Preferences)
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);

        // Used to put the default account settings for SERC
        /*SharedPreferences.Editor editor = appSettings.edit();
        editor.putString("api_key_edit", "36ec19e2a135f22b50883d555eea2114");
        editor.putString("root_link_editpref", "https://serc.strathmore.edu/emoncms");
        editor.apply();*/

        apiKey = appSettings.getString("api_key_edit", null);
        rootLinkAddress = appSettings.getString("root_link_editpref", null);
        // Make sure the link is not null before trying to fix it
        if (rootLinkAddress != null) {
            rootLinkAddress = fixLink(rootLinkAddress);
        }


        /*
         * Checks the API key is not empty/null(such as the first time a user logs in) or blank (for
         * instance if a user clicked cancel when the user was first prompted)
         */
        if (apiKey==null || apiKey.contentEquals("")){

            APIKeyDialog apiKeyDialog = new APIKeyDialog();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            apiKeyDialog.show(ft, "fragment_api_key");

            /*
             * Since the APIKeyDialog will save the API key in the shared preferences once the user
             * clicks the 'ok' button. If the user clicks cancel or enters a null string, the api key
             * will be null from resulting in an error when the link is sent (cannot concatenate strings
             * will a null object). As such an empty String "" is put as the default below
             */
            apiKey = appSettings.getString("api_key_edit", "");

        }

        if (rootLinkAddress==null || rootLinkAddress.contentEquals("")){

            RootLinkDialog rootLinkDialog = new RootLinkDialog();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            rootLinkDialog.show(ft, "fragment_root_link");

            /*
             * Since the RootLinkDialog will save the root link in the shared preferences once the user
             * clicks the 'ok' button. If the user clicks cancel or enters a null string, the root link
             * will be null, resulting in an error when the link is sent (cannot concatenate strings
             * will a null object). As such an empty String "" is put as the default below
             */
            rootLinkAddress = appSettings.getString("root_link_editpref", "");
            rootLinkAddress = fixLink(rootLinkAddress);

        }


        // Set up the locations in the main screen
        setUpLocationsForMainScreen();



        // onClick Listener for Swiping up to refresh feed. Calls the refreshContent method
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh(){
                // Calls the refresh content method defined within this class
                refreshContent();
            }
        });



    }

    // Creates the menu from the xml layout
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Defines the actions to take on button click for the menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(openSettingsIntent);
            return true;
        }
        if (id==R.id.action_about){
            Intent openAboutIntent = new Intent(this, AboutActivity.class);
            startActivity(openAboutIntent);
            return true;
        }
        if (id==R.id.action_help){
            Intent openHelpIntent = new Intent(this, HelpActivity.class);
            startActivity(openHelpIntent);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }


    //Method to refresh content. Called when user swipes up to refresh
    private void refreshContent(){
        swipeRefreshLayout.setRefreshing(true);
        // Call the main method. Since firstTimeLaunch is set to false, the adapter will only be
        // cleared and refreshed
        setUpLocationsForMainScreen();

        swipeRefreshLayout.setRefreshing(false); //stop the refresh dialog once finished
    }

    // Method used to add a "/" at the end and "https://" at the beginning of a link and to remove spaces
    private String fixLink (String linkToFix){
        String mFixedString = "";
        // Removes all spaces
        String trimedLinkToFix = linkToFix.replace(" ","");

        // Gets the API key from settings (Shared Preferences)
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = appSettings.edit();

        // Adds a "/" if it is not the last character in the link
        if (!trimedLinkToFix.matches(".*[/]")){
            mFixedString = trimedLinkToFix + "/";
            editor.putString("root_link_editpref", mFixedString);
            editor.apply();
        }
        // Checks if the link starts with https:// or http:// and adds it if not
        if (trimedLinkToFix.startsWith("https://") || trimedLinkToFix.startsWith("http://")){
            mFixedString = trimedLinkToFix;
        }
        else {
            mFixedString = "https://" + trimedLinkToFix;
            editor.putString("root_link_editpref", mFixedString);
            editor.apply();
        }

        return mFixedString;
    }

    // This Fragment class defines the pop-up that shows up if an API key is not found/or provided by the user
    public static class APIKeyDialog extends DialogFragment {


        private EditText apiKeyInput;


        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // Show soft keyboard automatically
            if (getDialog() != null) {
                getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }



        // Empty constructor required for DialogFragment
        public APIKeyDialog(){}


        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();


            // Inflate and set the layout for the dialog
            // Pass null as the parent view because it's going in the dialog layout
            View view = inflater.inflate(R.layout.dialog_login_api, null);
            dialogBuilder.setView(view);
            // Get the edit text view from the xml
            apiKeyInput = (EditText) view.findViewById(R.id.dialog_edit_text_api_key);
            // Makes the EditText box to be highlighted
            apiKeyInput.requestFocus();


            //Add Action button
            dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String userInput = apiKeyInput.getText().toString();

                    if (userInput.contentEquals("")) {
                        Toast.makeText(getContext(), "No API Key saved. Please add it in settings", Toast.LENGTH_LONG).show();
                    } else
                    {
                        Toast.makeText(getContext(), "API key " + userInput + " saved in Settings", Toast.LENGTH_LONG).show();
                    }

                    // Save settings
                    SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = appSettings.edit();
                    editor.putString("api_key_edit", userInput);
                    editor.apply();

                    ((MainActivityRecyclerView)getActivity()).refreshContent();

                }
            });

            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getContext(), "No API Key saved. Please add it in settings", Toast.LENGTH_LONG).show();
                    APIKeyDialog.this.getDialog().cancel();
                }
            });


            return dialogBuilder.create();
        }
    }

    // This Fragment class defines the pop-up that shows up if an API key is not found/or provided by the user
    public static class RootLinkDialog extends DialogFragment {



        private EditText rootLinkInput;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            // Show soft keyboard automatically
            if (getDialog() != null) {
                getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }



        // Empty constructor required for DialogFragment
        public RootLinkDialog(){}


        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();



            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            View view = inflater.inflate(R.layout.dialog_login_root_link, null);
            dialogBuilder.setView(view);
            // Get the edit text view from the xml
            rootLinkInput = (EditText) view.findViewById(R.id.dialog_edit_text_root_link);
            // Makes the EditText box to be highlighted
            rootLinkInput.requestFocus();


            //Add Action button
            dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String userInput = rootLinkInput.getText().toString();
                    if (!userInput.endsWith("/")){
                        userInput += "/";
                    }

                    if (userInput.contentEquals("")) {
                        Toast.makeText(getContext(), "Root Link not saved. Please add it in settings", Toast.LENGTH_LONG).show();
                    } else
                    {
                        Toast.makeText(getContext(), "Root Link " + userInput + " saved in Settings", Toast.LENGTH_LONG).show();
                    }

                    // Save settings
                    SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor editor = appSettings.edit();
                    editor.putString("root_link_editpref", userInput);
                    editor.apply();

                    ((MainActivityRecyclerView)getActivity()).refreshContent();

                }
            });

            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getContext(), "Root Link not saved. Please add it in settings", Toast.LENGTH_LONG).show();
                    RootLinkDialog.this.getDialog().cancel();
                }
            });


            return dialogBuilder.create();
        }
    }

    // Main method that sets up all the cards in the main screen
    public void setUpLocationsForMainScreen(){

        // Get Root/API key from settings
        final SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        rootLinkAddress = appSettings.getString("root_link_editpref","");
        // Make sure link is not malformed
        rootLinkAddress = fixLink(rootLinkAddress);

        apiKey = appSettings.getString("api_key_edit", "");
        // Call CmsApiCall using the MainActivity as the context. The result is the JSON file in
        // form of a continuous String.
        //result = new CmsApiCall(MainActivityRecyclerView.this).execute(rootLinkAddress+"feed/list.json&apikey="+apiKey).get();


        // Activity Done in the onPostExecute of the Asynctask to make sure the data is not out of sync
        new CmsApiCall(MainActivityRecyclerView.this, new CmsApiCall.AsyncResponse() {
            @Override
            public void processFinish(String output) throws JSONException {
                // Create an ArrayList of RecordingStation Objects with the variable name recordingStations
                ArrayList<RecordingStation> recordingStations = new ArrayList<>();

                resultFromEmonCms = output;


                // This changes the JSON String into a JSON object. The response for this call consists of
                // one JSON array with individual objects for each node added to the Emon CMS platform
                JSONArray parentJSON = new JSONArray(resultFromEmonCms);
                JSONObject childJSON;

                // Cycles through all objects within the JSON array
                for (int i=0; i<parentJSON.length(); i++){

                    // Initialising the variables needed for the RecordingStation constructor
                    int id =0;
                    String name="";
                    String tag="";
                    int time=0;
                    int powerReading=0;

                    // Setting childJSON as an object in the JSON array at position i
                    childJSON = parentJSON.getJSONObject(i);

                    //Checks if ID field exists and is not null
                    if (childJSON.has("id") && !childJSON.isNull("id")){
                        id = childJSON.getInt("id");
                    }
                    //Checks if name field exists and is not null
                    if (childJSON.has("name") && !childJSON.isNull("name")){
                        name = childJSON.getString("name");
                    }
                    //Checks if tag field exists and is not null
                    if (childJSON.has("tag") && !childJSON.isNull("tag")){
                        tag = childJSON.getString("tag");
                    }
                    //Checks if time field exists and is not null
                    if (childJSON.has("time") && !childJSON.isNull("time")){
                        time = childJSON.getInt("time");
                    }
                    //Checks if value field exists and is not null
                    if (childJSON.has("value") && !childJSON.isNull("value")){
                        powerReading = childJSON.getInt("value");
                    }

                    //Creating new RecordingStation object with the values and adding it to the ArrayList for each loop
                    RecordingStation recordingStation = new RecordingStation(id, name, tag, time, powerReading);
                    recordingStations.add(recordingStation);

                }

                /*
                     * This function takes in an array list of the recording stations and returns an array list
                     * of recording stations which is a subset of the of the input. This subset contains all the
                     * recording stations contained within the setting preferences
                     */

                Set<String> recordingStationsInSettings = appSettings.getStringSet("selected_station_list", Collections.<String>emptySet());
                Set<String> chosenRecordingStations = new HashSet<>();


                // Creates an array list of names of the stations in the form "TAG - NAME"
                Log.i("SERC Log:", "Building List of Recording Station Names");
                ArrayList<String> recordingStationNames = new ArrayList<>();
                for (int i=0; i<recordingStations.size(); i++){
                    recordingStationNames.add(recordingStations.get(i).getStationTag() + " - " + recordingStations.get(i).getStationName());
                }

                // Sort List Alphabetically
                Collections.sort(recordingStationNames, String.CASE_INSENSITIVE_ORDER);

                // Adds these names to a new String Array List to chosenRecordingStations
                Log.i("SERC Log:", "Adding the names to settings");
                chosenRecordingStations.addAll(recordingStationNames);

                Log.i("SERC Log:", "Saving new settings");
                SharedPreferences.Editor editor = appSettings.edit();
                editor.putStringSet("full_station_list", chosenRecordingStations);

                Log.i("SERC Log", "Checking if selected_station_list in SharedPrefs is empty: " + String.valueOf(recordingStations.isEmpty()));
                if (recordingStationsInSettings.isEmpty()) {
                    // Adds full list in case selected list is empty e.g. on first launch
                    editor.putStringSet("selected_station_list", chosenRecordingStations);
                    editor.apply();
                }

                // Logging entries in the list
                Log.i("SERC Log:", "selected_station_list size: "+ String.valueOf(recordingStationsInSettings.size()));
                for (int i=0; i<recordingStationsInSettings.size(); i++){
                    Log.i("SERC Log:", "selected_station_list " + String.valueOf(i) + ": "+ String.valueOf(recordingStationsInSettings.toArray()[i]));
                }

                ArrayList<RecordingStation> recordingStationsForAdapter = new ArrayList<>();
                for (String nameTag:recordingStationsInSettings){
                    for (int j = 0; j < recordingStations.size(); j++){
                        String currentStn = recordingStations.get(j).getStationTag() + " - " + recordingStations.get(j).getStationName();
                        Log.i("SERC Log", "currentStn: " + currentStn);
                        Log.i("SERC Log", "nameTag: " + nameTag);
                        Log.i("SERC Log", "nameTag.contains(currentStn): " + String.valueOf(nameTag.contains(currentStn)));

                        if (nameTag.contains(currentStn)){
                            recordingStationsForAdapter.add(recordingStations.get(j));

                        }
                    }
                }


                // Arranges the Stations alphabetically by tag name
                if (recordingStationsForAdapter.size()>1){
                    Collections.sort(recordingStationsForAdapter, new Comparator<RecordingStation>() {
                        @Override
                        public int compare(RecordingStation o1, RecordingStation o2) {
                            return o1.getStationTag().compareTo(o2.getStationTag());
                        }
                    });
                }

                // Checks if the activity is being lauched or if it is a refresh
                if (firstTimeLaunch) {
                    // Binding the adapter to the RecyclerView
                    adapter = new RecyclerViewAdapter(MainActivityRecyclerView.this, recordingStationsForAdapter);
                    RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_main_activity);
                    recyclerView.setAdapter(adapter);
                    // Attach layout manager to the RecyclerView
                    recyclerView.setLayoutManager(new LinearLayoutManager(MainActivityRecyclerView.this));
                    // Set the animation from wasabeef's recycler-animator library
                    recyclerView.setItemAnimator(new SlideInUpAnimator());




                    /*
                     * OnItemClickLister for each item in the RecylerView. When an item in the RecylerView is clicked,
                     * this sends an intent to open GraphActivity (while passing some information about the
                     * object to GraphActivity within the intent)
                     */
                    adapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(View itemView, int position) {
                            Intent graphIntent = new Intent(MainActivityRecyclerView.this, GraphTabbed.class);

                            // Getting the station ID, name and tag of the Clicked item to be sent with the intent
                            graphIntent.putExtra("Station_ID", adapter.getRecordingStation(position).getStationID());
                            graphIntent.putExtra("Station_name", adapter.getRecordingStation(position).getStationName());
                            graphIntent.putExtra("Station_tag", adapter.getRecordingStation(position).getStationTag());

                            // Start GraphActivity
                            startActivity(graphIntent);
                        }
                    });



                    /**
                     * This is the listener for when a user long clicks/presses on an item in the listview.
                     * This opens a Dialog showing all the information stored for the that RecordingStation object
                     */

                    /*adapter.setOnItemLongClickListener(new RecyclerViewAdapter.OnItemLongClickListener() {
                        @Override
                        public void onItemLongClick(View itemView, int position) {
                            AlertDialog.Builder  alertDialogBuilder = new AlertDialog.Builder(MainActivityRecyclerView.this);
                            alertDialogBuilder.setTitle("Station Details");
                            alertDialogBuilder.setIcon(R.mipmap.ic_launcher_serc);
                            alertDialogBuilder.setPositiveButton("Ok", null);

                            Date currentTime = new Date(adapter.getRecordingStation(position).getStationTime()*1000);
                            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy h:mm a");
                            String stationTime = sdf.format(currentTime);
                            CharSequence[] stationDetails = {
                                    "Station ID: " + String.valueOf(adapter.getRecordingStation(position).getStationID()),
                                    "Station Name: " + adapter.getRecordingStation(position).getStationName(),
                                    "Station Tag: " + adapter.getRecordingStation(position).getStationTag(),
                                    "Current Reading: " + String.valueOf(adapter.getRecordingStation(position).getStationValueReading()),
                                    "Time of current reading: " + stationTime};

                            alertDialogBuilder.setItems(stationDetails, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 1){
                                        Toast.makeText(getBaseContext(), "The ID sent from the EmonCMS platform for this particular station", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });

                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();




                        }
                    });*/

                    adapter.setOnItemLongClickListener(new RecyclerViewAdapter.OnItemLongClickListener() {
                        @Override
                        public void onItemLongClick(View itemView, final int position) {

                            final int currentPosition = position;

                            Date currentTime = new Date(adapter.getRecordingStation(position).getStationTime()*1000);
                            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy h:mm a");
                            final String stationTime = sdf.format(currentTime);

                            LongPressPopup popup = new LongPressPopupBuilder(MainActivityRecyclerView.this)// A Context object for the builder constructor
                                    .setTarget(itemView)
                                    .setPopupView(R.layout.popup_layout, new PopupInflaterListener() {
                                        @Override
                                        public void onViewInflated(@Nullable String popupTag, View root) {
                                            TextView popup_ID = (TextView) root.findViewById(R.id.popup_location_id);
                                            TextView popup_Tag = (TextView) root.findViewById(R.id.popup_location_tags);
                                            TextView popup_Name = (TextView) root.findViewById(R.id.popup_location_name);
                                            TextView popup_reading = (TextView) root.findViewById(R.id.popup_location_reading);
                                            TextView popup_time = (TextView) root.findViewById(R.id.popup_location_time);

                                            popup_ID.setText( String.valueOf(adapter.getRecordingStation(currentPosition).getStationID()));
                                            popup_Name.setText( adapter.getRecordingStation(currentPosition).getStationName());
                                            popup_Tag.setText(adapter.getRecordingStation(currentPosition).getStationTag());
                                            popup_reading.setText(String.valueOf(adapter.getRecordingStation(currentPosition).getStationValueReading())+ "W");
                                            popup_time.setText(stationTime);

                                        }
                                    })// The View to show when long pressed
                                    .setCancelTouchOnDragOutsideView(true)
                                    .build();// This will give you a LongPressPopup object

                            // You can also chain it to the .build() method call above without declaring
                            // the "popup" variable before
                            // From this moment, the touch events are registered and, if long pressed,
                            // will show the given view inside the popup, call unregister() to stop
                            popup.register();
                        }
                    });


                    // Makes sure this loop does not repeat again while activity has not closed
                    firstTimeLaunch = false;


                } else{

                    // Clear the adapter and load up new content to adapter
                    adapter.clear();
                    adapter.addAll(recordingStationsForAdapter);

                }


            }
        }).execute(rootLinkAddress+"feed/list.json&apikey="+apiKey);


    }



}
