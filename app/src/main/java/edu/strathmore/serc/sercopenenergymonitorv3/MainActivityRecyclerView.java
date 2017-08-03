package edu.strathmore.serc.sercopenenergymonitorv3;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
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

public class MainActivityRecyclerView extends AppCompatActivity {

    //apiKey = "36ec19e2a135f22b50883d555eea2114";

    // For the SwipeRefreshLayout used both in the onCreate and refresh method
    private SwipeRefreshLayout swipeRefreshLayout;

    // For the adapter used in the RecyclerView  of the Main Activity/Screen.
    // Needs to be global as it is used both in the onCreate and refresh method
    private RecyclerViewAdapter adapter;


    //private Account currentAccount;


    // Checks if the activity is launching
    //private boolean firstTimeLaunch = true;

    // Needed for live data
    private Handler timerHandler;
    private boolean isShowingLive = false;
    private int fetchInterval = 5000;
    //private int intervalBalance = 2000;
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                setUpLocationsForMainScreen(false,false);
            }
            finally {
                timerHandler.postDelayed(timerRunnable, fetchInterval);
            }

        }
    };


    // For snackbar
    CoordinatorLayout coordinatorLayout;

    private TextView noAccountFoundTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_recycler_view);


        // For the Snackbar called in the menu
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout_main);

        // Set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boolean firstLaunch = false;
        Bundle extras = getIntent().getExtras();
        if(extras!=null){
            firstLaunch = extras.getBoolean("first_launch",false);
        }



        // Gets the API key from settings (Shared Preferences)
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);

        // Get the fetch interval from settings
        fetchInterval = Integer.valueOf(appSettings.getString("pref_update_frequency", "5")) * 1000 ;

        AccountConfig accountConfig = new AccountConfig(getBaseContext());
        ArrayList<Account> allAccounts = accountConfig.getAllAccounts();
        String oldApiKey = appSettings.getString("api_key_edit", null);
        String oldRootLinkAddress = appSettings.getString("root_link_editpref", null);



        boolean shownOldAccountNotification = appSettings.getBoolean("old_account_dialog_shown",false);

        if (firstLaunch){
            WelcomeDialog welcomeDialog = new WelcomeDialog();
            welcomeDialog.setCancelable(false);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            welcomeDialog.show(ft, "welcome_dialog");
        }
        else if (!shownOldAccountNotification && oldApiKey!=null && oldRootLinkAddress != null){
            GetOldAPIAndLink notification = new GetOldAPIAndLink();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            notification.show(ft, "old_acc_notif");

        }
        else if (allAccounts.isEmpty()){

            boolean noAccountNotificationShown = appSettings.getBoolean("no_account_notification_shown", false);

            if (!noAccountNotificationShown) {
                NoAccountFound notification = new NoAccountFound();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                notification.show(ft, "no_acc");
            }
        }



        /*// Make sure the link is not null before trying to fix it
        if (rootLinkAddress != null) {
            rootLinkAddress = fixLink(rootLinkAddress);
        }*/


        /*
         * Checks the API key is not empty/null(such as the first time a user logs in) or blank (for
         * instance if a user clicked cancel when the user was first prompted)
         */
        /*if (apiKey==null || apiKey.contentEquals("")){

            APIKeyDialog apiKeyDialog = new APIKeyDialog();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            apiKeyDialog.show(ft, "fragment_api_key");

            *//*
             * Since the APIKeyDialog will save the API key in the shared preferences once the user
             * clicks the 'ok' button. If the user clicks cancel or enters a null string, the api key
             * will be null from resulting in an error when the link is sent (cannot concatenate strings
             * will a null object). As such an empty String "" is put as the default below
             *//*
            apiKey = appSettings.getString("api_key_edit", "");

        }

        if (rootLinkAddress==null || rootLinkAddress.contentEquals("")){

            RootLinkDialog rootLinkDialog = new RootLinkDialog();
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            rootLinkDialog.show(ft, "fragment_root_link");

            *//*
             * Since the RootLinkDialog will save the root link in the shared preferences once the user
             * clicks the 'ok' button. If the user clicks cancel or enters a null string, the root link
             * will be null, resulting in an error when the link is sent (cannot concatenate strings
             * will a null object). As such an empty String "" is put as the default below
             *//*
            rootLinkAddress = appSettings.getString("root_link_editpref", "");
            rootLinkAddress = fixLink(rootLinkAddress);

        }*/

        timerHandler = new Handler();
        noAccountFoundTextView = (TextView) findViewById(R.id.no_account_found);

        if (!firstLaunch) {


            Set<String> recordingStationsInSettings = appSettings.getStringSet("selected_station_list", Collections.<String>emptySet());

            if (recordingStationsInSettings.isEmpty()) {
                noAccountFoundTextView.setVisibility(View.VISIBLE);
                noAccountFoundTextView.setText("No locations chosen. Please choose a location to display in settings");
            } else {
                noAccountFoundTextView.setVisibility(View.GONE);
            }

            // Set up the locations in the main screen
            setUpLocationsForMainScreen(false, true);   //--> Moved to on resume
        }




        // onClick Listener for Swiping up to refresh feed. Calls the refreshContent method
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh(){

                swipeRefreshLayout.setRefreshing(true);

                // Calls the refresh content method defined within this class
                setUpLocationsForMainScreen(true, false);

                //stopRefreshLayoutRefreshing();

            }
        });



    }


    private void setUpOldAccount(){
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String api = appSettings.getString("api_key_edit", null);
        String link = appSettings.getString("root_link_editpref", null);

        AccountConfig accountConfig = new AccountConfig(getBaseContext());
        String id = accountConfig.addAccount();
        // Get Account object
        Account account = accountConfig.getAccountFromID(id);
        // Set Account Details
        account.setAccountName("Old Account");
        account.setApiKey(api);
        account.setRootLink(link);
        // Save to settings
        accountConfig.saveAccountDetailsInSettings(account);
    }

    // Refresh the data if the user navigates to this Activity from elsewhere (or if the app has
    // been inactive for a while and the app is paused)
    @Override
    protected void onResume() {
        super.onResume();
        //setUpLocationsForMainScreen(false, true);
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {

            case  (R.id.action_settings):
                Intent openSettingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(openSettingsIntent);
                return true;
            case  (R.id.action_about):
                Intent openAboutIntent = new Intent(this, AboutActivity.class);
                startActivity(openAboutIntent);
                return true;
            case (R.id.action_help):
                Intent openHelpIntent = new Intent(this, HelpActivity.class);
                startActivity(openHelpIntent);
                return true;
            case (R.id.action_live_data):

                // Don't show Live Data
                if (item.isChecked()) {

                    isShowingLive = false;
                    item.setChecked(false);
                    stopRepeatingAction();

                    // Snackbar
                    Snackbar snackbar = Snackbar.make(coordinatorLayout, "No longer showing live data", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    item.setChecked(true);
                                    isShowingLive = true;

                                    //timerHandler.postDelayed(timerRunnable, fetchInterval);
                                    startRepeatingAction();
                                }
                            });
                    // Changing message text color
                    snackbar.setActionTextColor(Color.RED);

                    // Changing action button text color
                    View sbView = snackbar.getView();
                    TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.YELLOW);

                    snackbar.show();

                    return true;


                }
                // Show live data
                else {
                    item.setChecked(true);
                    isShowingLive = true;
                    //timerHandler.postDelayed(timerRunnable, fetchInterval);
                    startRepeatingAction();


                    // Snackbar
                    Snackbar snackbar = Snackbar.make(coordinatorLayout, "Now showing live data", Snackbar.LENGTH_LONG)
                            .setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    isShowingLive = false;
                                    item.setChecked(false);
                                    stopRepeatingAction();


                                }
                            });
                    // Changing message text color
                    snackbar.setActionTextColor(Color.RED);

                    // Changing action button text color
                    View sbView = snackbar.getView();
                    TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.YELLOW);

                    snackbar.show();

                    return true;

                }


            /*case (R.id.save_old_account):
                setUpAccount();
                return true;*/

           /* case (R.id.show_ac_list):
                AccountConfig config = new AccountConfig(getBaseContext());
                String [] accountList = config.getAccountIDArray();
                for (String accountID:accountList){
                    Log.i("SERC Log", "ID: " + accountID);
                }
                return true;*/

            /*case (R.id.add_textview):
                LayoutInflater inflater = getLayoutInflater();
                View view = inflater.inflate(R.layout.activity_main_recycler_view_content, null);
                TextView textView = new TextView(getBaseContext());
                textView.setText("Trail");
                LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.main_screen_content_container);
                linearLayout.addView(textView);
                return true;*/


            default:
                return super.onOptionsItemSelected(item);

        }


        //return super.onOptionsItemSelected(item);
    }


    void startRepeatingAction(){
        Log.i("SERC Log", "Live data started");
        timerRunnable.run();
    }

    void stopRepeatingAction(){
        Log.i("SERC Log", "Live data stopped");
        timerHandler.removeCallbacks(timerRunnable);
    }

    // Method used to add a "/" at the end and "https://" at the beginning of a link and to remove spaces
    private String fixLink (String linkToFix){
        String mFixedString = "";
        // Removes all spaces
        String trimedLinkToFix = linkToFix.replace(" ","");

        // Adds a "/" if it is not the last character in the link
        if (!trimedLinkToFix.matches(".*[/]")){
            mFixedString = trimedLinkToFix + "/";
        }

        // Checks if the link starts with https:// or http:// and adds it if not
        if (trimedLinkToFix.startsWith("https://") || trimedLinkToFix.startsWith("http://")){
            mFixedString = trimedLinkToFix;
        }
        else {
            mFixedString = "https://" + trimedLinkToFix;

        }

        return mFixedString;
    }

    // Pop-up on LongPress on a location
    public static class LocationDetails extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //return super.onCreateDialog(savedInstanceState);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.popup_layout, null);
            builder.setView(view);

            // Get the TextViews
            TextView locationID = (TextView) view.findViewById(R.id.popup_location_id);
            TextView locationTag = (TextView) view.findViewById(R.id.popup_location_tags);
            TextView locationName = (TextView) view.findViewById(R.id.popup_location_name);
            TextView locationReading = (TextView) view.findViewById(R.id.popup_location_reading);
            TextView locationTime = (TextView) view.findViewById(R.id.popup_location_time);

            // Get information sent as Bundle
            Bundle bundle = getArguments();

            int stationID = bundle.getInt("location_ID", 0);
            String stationName = bundle.getString("location_name","");
            String stationTag = bundle.getString("location_tag","");
            int stationReading = bundle.getInt("location_reading",0);
            long stationTime = bundle.getLong("location_time",0l);
            Date currentTime = new Date(stationTime*1000l);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy h:mm a");
            String time = simpleDateFormat.format(currentTime);

            // Set text
            locationID.setText(String.valueOf(stationID));
            locationTag.setText(stationTag);
            locationName.setText(stationName);
            locationReading.setText(String.valueOf(stationReading));
            locationTime.setText(time);

            return builder.create();

        }
    }

    // Deals with pop-up when item is clicked
    public static class LocationContextMenu extends DialogFragment{


        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //return super.onCreateDialog(savedInstanceState);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.context_menu, null);
            builder.setView(view);

            TextView heading = (TextView) view.findViewById(R.id.context_menu_heading);
            TextView historicalGraph = (TextView) view.findViewById(R.id.context_menu_historical_graph);
            TextView liveGraph = (TextView) view.findViewById(R.id.context_menu_live_graph);

            final Bundle mArgs = getArguments();
            String title = mArgs.getString("location_title","");
            heading.setText(title);




            historicalGraph.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), GraphTabbed.class );
                    // Getting the station ID, name and tag of the Clicked item to be sent with the intent
                    //account_ID
                    intent.putExtra("Station_ID", mArgs.getInt("location_ID"));
                    intent.putExtra("Station_name", mArgs.getString("location_name"));
                    intent.putExtra("Station_tag", mArgs.getString("location_tag"));
                    intent.putExtra("Account_ID", mArgs.getString("account_ID"));
                    LocationContextMenu.this.getDialog().cancel();
                    startActivity(intent);
                }
            });

            liveGraph.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), LiveGraphActivity.class);
                    intent.putExtra("Station_ID", mArgs.getInt("location_ID"));
                    intent.putExtra("Station_name", mArgs.getString("location_name"));
                    intent.putExtra("Station_tag", mArgs.getString("location_tag"));
                    intent.putExtra("Account_ID", mArgs.getString("account_ID"));
                    LocationContextMenu.this.getDialog().cancel();
                    startActivity(intent);
                }
            });


            return builder.create();
        }
    }

    public static class WelcomeDialog extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //return super.onCreateDialog(savedInstanceState);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Welcome")
                    .setMessage("You will now be taken to the account set up page to enter your Emoncms " +
                            "account details. This is required use the app.")
                    .setCancelable(false)
                    .setPositiveButton("Set up Account", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent openAccountSettings = new Intent(getContext(), AccountSettings.class);
                            openAccountSettings.putExtra("first_time_launch", true);
                            startActivity(openAccountSettings);
                        }
                    })
            ;


            return builder.create();
        }
    }

    // Dialog Fragment for showing new account support
    public static class NoAccountFound extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(final Bundle savedInstanceState) {
            //return super.onCreateDialog(savedInstanceState);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            final SharedPreferences.Editor editor = prefs.edit();
            builder.setTitle("No Account Info Found")
                    .setMessage("SERC Energy Monitor v.4.0 now supports multiple accounts from different emoncms servers.\n\n" +
                            "No account information was found on this device. Would you like to set up a new Account?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Save that dialog has been shown
                            editor.putBoolean("no_account_notification_shown", true);
                            editor.apply();
                            // Go to account set up page
                            Intent startAccountSettings = new Intent(getContext(), AccountSettings.class);
                            startAccountSettings.putExtra("no_account_found", true);
                            startActivity(startAccountSettings);

                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Save that dialog has been shown
                            editor.putBoolean("no_account_notification_shown", true);
                            editor.apply();
                            // Dismiss the dialog window
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton("Remind me later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                            editor.putBoolean("no_account_notification_shown", false);
                            editor.apply();
                            // Dismiss
                            dialog.dismiss();
                        }
                    })


            ;



            return builder.create();
        }
    }

    // Dialog that gets old account information
    public static class GetOldAPIAndLink extends DialogFragment{
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //return super.onCreateDialog(savedInstanceState);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            final SharedPreferences.Editor editor = prefs.edit();
            builder.setTitle("Account Info Detected!")
                    .setMessage("We have noted that there is already API key and Emoncms server information stored in this device. " +
                    "These will no longer work with the new update and may cause no information to appear on the main screen\n\n" +
                    "Would you like to create a new Account using this information?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Create new account
                            ((MainActivityRecyclerView)getActivity()).setUpOldAccount();
                            editor.putBoolean("old_account_dialog_shown", true);
                            editor.apply();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Save don't show dialog in settings

                            editor.putBoolean("old_account_dialog_shown", true);
                            editor.apply();
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton("Remind me later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Don't change
                            editor.putBoolean("old_account_dialog_shown", false);
                            editor.apply();
                            dialog.dismiss();
                        }
                    })
            ;


            return builder.create();
        }
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

                    ((MainActivityRecyclerView)getActivity()).setUpLocationsForMainScreen(false,true);

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

                    ((MainActivityRecyclerView)getActivity()).setUpLocationsForMainScreen(false, true);

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
    public void setUpLocationsForMainScreen(boolean forSwipeToRefresh, boolean firstTimeLaunch){
        // Get Root/API key from settings for each account
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        AccountConfig accountConfig = new AccountConfig(getBaseContext());
        // Get the IDs of the selected accounts from settings
        Set<String> selectedAccounts = appSettings.getStringSet("selected_account_list", Collections.<String>emptySet());

        //Log.i("SERC Log","");
        Log.i("SERC Log","Selected Accounts List is empty: " + String.valueOf(selectedAccounts.isEmpty()));

        if (selectedAccounts.isEmpty()) {

            if (noAccountFoundTextView!=null) {
                noAccountFoundTextView.setVisibility(View.VISIBLE);
                noAccountFoundTextView.setText("No Account Selected. Showing all accounts");
            }


            // If selected accounts is empty (like first launch), show all accounts
            ArrayList<Account> accounts = accountConfig.getAllAccounts();

            for (Account acc:accounts){
                Log.i("SERC Log", "ID: " + acc.getAccountID() + " Name: " + acc.getAccountName());
            }

            for (int i = 0; i<accounts.size();i++) {
                boolean lastAccount = false;
                boolean clearScreen = false;
                if (i == accounts.size()-1){
                    lastAccount = true;
                }
                if (i==0){
                    clearScreen = true;
                }
                Log.i("SERC Log", "forSwipeToRefresh: " + forSwipeToRefresh + " ,firstTimeLaunch: " + firstTimeLaunch
                        + " ,lastAccount: " + lastAccount + " ,clearScreen: " + clearScreen
                );
                apiCall(accounts.get(i), forSwipeToRefresh, firstTimeLaunch, lastAccount, clearScreen);
            }
        }

        else {
            if (noAccountFoundTextView!=null) {
                noAccountFoundTextView.setVisibility(View.GONE);
            }
            ArrayList<Account> accounts = new ArrayList<>();
            // Gets valid accounts from selected list
            for (String accountID : selectedAccounts) {
                if (!accountID.isEmpty()) {
                    Account account = accountConfig.getAccountFromID(accountID);
                    Log.i("SERC Log", "Account Name: " + account.getAccountName() + "  ID: " + account.getAccountID());
                    if (!account.getAccountName().isEmpty()) {
                        accounts.add(account);
                    }
                }
            }

            //Log.i("SERC Log","Number of chosen accounts: " + accounts.size());
            if (accounts.size()>0) {
                for (int i = 0; i<accounts.size();i++) {
                    boolean lastAccount = false;
                    boolean clearScreen = false;
                    if (i == accounts.size()-1){
                        lastAccount = true;
                    }
                    if (i==0){
                        clearScreen = true;
                    }
                    apiCall(accounts.get(i), forSwipeToRefresh, firstTimeLaunch, lastAccount, clearScreen);
                }
            } else if (accounts.size() == 0 && forSwipeToRefresh){
                // Stops the refresh layout from spinning if no account is chosen
                stopRefreshLayoutRefreshing();
                if (noAccountFoundTextView!=null) {
                    noAccountFoundTextView.setVisibility(View.VISIBLE);
                    noAccountFoundTextView.setText("No Account Selected!");
                }

                if(adapter!=null){
                    adapter.clear();
                }
            }



        }


    }

    private void apiCall (final Account account, boolean forSwipeToRefresh, final boolean firstTimeLaunch, final boolean lastAccount,
                          boolean clearScreen){

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if (firstTimeLaunch) {
            Log.i("SERC Log","Running first time setup");
            // Binding the adapter to the RecyclerView
            adapter = new RecyclerViewAdapter(getBaseContext(), new ArrayList<RecordingStation>(), account);
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_main_activity);
            recyclerView.setAdapter(adapter);

            // Check if the device has a large screen or is in Landscape mode
            // Use a grid layout when the app is in landscape or devices has a 6.5inch screen or bigger
            double screenSize = getScreenSize();
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE || screenSize>=6.5) {
                recyclerView.setLayoutManager(new GridLayoutManager(MainActivityRecyclerView.this, 2));
            }
            else{
                // Use a linear layout when the app is in portrait
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivityRecyclerView.this));
            }

            // Set the animation from wasabeef's recycler-animator library if animations are not disabled
            boolean disableAnimation = appSettings.getBoolean("pref_general_disable_animations", false);
            if (!disableAnimation) {
                recyclerView.setItemAnimator(new SlideInUpAnimator());
            }

            setUpClickListeners(account);



        }

        String rootLinkAddress = account.getRootLink();
        rootLinkAddress = fixLink(rootLinkAddress);
        if(!rootLinkAddress.endsWith("/")){
            rootLinkAddress = rootLinkAddress  + "/";
        }
        String apiKey = account.getApiKey();

        if (rootLinkAddress.isEmpty()) {
            Toast.makeText(getBaseContext(), "No Link found for " + account.getAccountName(),
                    Toast.LENGTH_SHORT).show();
        }
        else if (apiKey.isEmpty()) {
            Toast.makeText(getBaseContext(), "No API found for " + account.getAccountName(),
                    Toast.LENGTH_SHORT).show();
        }
        else {
            final boolean clear = clearScreen;

            //Log.i("SERC Log","Clear Screen, for loop: " + clearScreen);

            // This checks if the the method has been called by the SwipeRefreshLayout and creates the
            // appropriate constructor for it
            if (forSwipeToRefresh) {
                //Log.i("SERC Log","Clear Screen, for loop/swipeRefresh: " + clear);

                // Activity Done in the onPostExecute of the Asynctask to make sure the data is not out of sync
                new CmsApiCall(MainActivityRecyclerView.this, swipeRefreshLayout, account, new CmsApiCall.AsyncResponse() {
                    @Override
                    public void processFinish(String output) throws JSONException {
                        setUpSteps(output, account, firstTimeLaunch, clear);
                        if (lastAccount) {
                            // Stops the indicator in the swipe to refresh layout from spinning
                            stopRefreshLayoutRefreshing();
                        }
                    }
                }).execute(rootLinkAddress + "feed/list.json&apikey=" + apiKey);


            } else {

                // Activity Done in the onPostExecute of the Asynctask to make sure the data is not out of sync
                new CmsApiCall(MainActivityRecyclerView.this, account, new CmsApiCall.AsyncResponse() {
                    @Override
                    public void processFinish(String output) throws JSONException {
                        //Log.i("SERC Log","Clear Screen, for loop/noSwipeRefresh: " + clear);
                        setUpSteps(output, account, firstTimeLaunch, clear);
                        if (lastAccount) {
                            // Stops the indicator in the swipe to refresh layout from spinning
                            stopRefreshLayoutRefreshing();
                        }
                    }
                }).execute(rootLinkAddress + "feed/list.json&apikey=" + apiKey);

            }

        }
    }

    private boolean lastAccountInList(Account account, ArrayList<Account> accounts){
        return account.getAccountID() == accounts.get(accounts.size() - 1).getAccountID();
    }

    // Actual steps needed to set up the RecyclerView
    private void setUpSteps(String output, Account account, boolean firstTime, boolean clearScreen) {

        //Log.i("SERC Log", "Clear Screen: " + clearScreen);


        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Get list of RecordingStation objects from response
        ArrayList<RecordingStation> recordingStations = jsonToRecordingStationList(output);

       /* for (RecordingStation station:recordingStations){
            Log.i("SERC Log", "All Locations: " + station.getStationTag() + " - " + station.getStationName());
        }*/

        // Get subset of those locations selected in settings
        ArrayList<RecordingStation> recordingStationsForAdapter = getRecordingStationsInSettings(recordingStations, account, firstTime);

        /*for (RecordingStation station:recordingStationsForAdapter){
            Log.i("SERC Log", "Locations for adapter: " + station.getStationTag() + " - " + station.getStationName());
        }*/

        if (recordingStationsForAdapter.size()>0) {
            // Arranges the Stations alphabetically by tag name
            final boolean switchNameTag = appSettings.getBoolean("pref_general_switch_name_tag", false);
            Log.i("SERC Log","Arranging locations alphabetically");
            if (recordingStationsForAdapter.size()>1){
                Collections.sort(recordingStationsForAdapter, new Comparator<RecordingStation>() {
                    @Override
                    public int compare(RecordingStation o1, RecordingStation o2) {
                        int comp;
                        // If Preference is switched on, arrange by name first
                        if (switchNameTag) {
                            comp = o1.getStationName().compareTo(o2.getStationName());
                            if (comp==0) {
                                comp = o1.getStationTag().compareTo(o2.getStationTag());
                            }
                        } else {
                            comp = o1.getStationTag().compareTo(o2.getStationTag());
                            if (comp==0) {
                                comp = o1.getStationName().compareTo(o2.getStationName());
                            }
                        }
                        return comp;
                    }
                });
            }


            boolean disableAnimations = appSettings.getBoolean("pref_general_disable_animations", false);
            //Log.i("SERC Log","");
            //Log.i("SERC Log","Adapter null: " + String.valueOf(adapter==null));
            if (adapter!=null) {

                if (isShowingLive || disableAnimations) {
                    adapter.notifyMassDataChange(recordingStationsForAdapter, clearScreen, account);


                } else{
                    // Clear the adapter and load up new content to adapter


                    //Log.i("SERC Log","Clear Screen, !firstLaunch with animations : " + clearScreen);
                    if (clearScreen) {
                        Log.i("SERC Log","Smooth Clear run");
                        adapter.smoothClear();
                    }

                    adapter.addAll(recordingStationsForAdapter,account);


                }
            }
        }

        else {
            if(clearScreen && adapter!=null){
                adapter.clear();
            }
        }


    }

    // Method that gets the JSON String from the emoncms server and transforms it into an ArrayList
    // of RecordingStation objects
    public ArrayList<RecordingStation> jsonToRecordingStationList(String jsonText){

        try {
            // Create an ArrayList of RecordingStation Objects with the variable name recordingStations
            ArrayList<RecordingStation> recordingStations = new ArrayList<>();

            // This changes the JSON String into a JSON object. The response for this call consists of
            // one JSON array with individual objects for each node added to the Emon CMS platform
            JSONArray parentJSON = new JSONArray(jsonText);
            JSONObject childJSON;

            if (parentJSON.length()>0) {
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
            }

            return recordingStations;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Gets the subset of Recording Stations in selected in settings from the full list supplied to it
    public ArrayList<RecordingStation> getRecordingStationsInSettings(ArrayList<RecordingStation> recordingStations, Account account, boolean firstTime){
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);
        if (recordingStations.size()>0) {




        /* This function takes in an array list of the recording stations and returns an array list
         * of recording stations which is a subset of the of the input. This subset contains all the
         * recording stations contained within the setting preferences
         */
            Set<String> recordingStationsInSettings = appSettings.getStringSet("selected_station_list", Collections.<String>emptySet());


            if (firstTime) {
                Set<String> chosenRecordingStations = new HashSet<>();
                // Creates an array list of names of the stations in the form "TAG - NAME"
                ArrayList<String> recordingStationNames = new ArrayList<>();
                for (int i=0; i<recordingStations.size(); i++){
                    recordingStationNames.add(recordingStations.get(i).getStationTag() + " - " + recordingStations.get(i).getStationName());

                }

                // Sort List Alphabetically
                Collections.sort(recordingStationNames, String.CASE_INSENSITIVE_ORDER);

                // Adds these names to a new String Array List to chosenRecordingStations
                chosenRecordingStations.addAll(recordingStationNames);

                AccountConfig accountConfig = new AccountConfig(getBaseContext());
                accountConfig.setStationListInSettings(account.getAccountID(), chosenRecordingStations);
            }
            ArrayList<RecordingStation> recordingStationsForAdapter;

            if (recordingStationsInSettings.isEmpty()) {
                recordingStationsForAdapter = recordingStations;
                if (noAccountFoundTextView!=null) {

                    if (noAccountFoundTextView.getVisibility() == View.GONE) {
                        noAccountFoundTextView.setVisibility(View.VISIBLE);
                        noAccountFoundTextView.setText("No Location Selected. Showing all locations");
                    }

                }

            } else {
                // Compare strings in settings (in String set recordingStationsInSettings) with the ArrayList of
                // RecordingStations (in recordingStations)
                recordingStationsForAdapter = new ArrayList<>();
                for (String nameTag:recordingStationsInSettings){
                    for (int j = 0; j < recordingStations.size(); j++){
                        String currentStn;
                        currentStn = recordingStations.get(j).getStationTag() + " - " + recordingStations.get(j).getStationName();


                        if (nameTag.contains(currentStn)){
                            recordingStationsForAdapter.add(recordingStations.get(j));

                        }
                    }
                }
            }

            /*for (RecordingStation recordingStation:recordingStationsForAdapter){
                Log.i("SERC Log", "Recording Stations for adapter: " + recordingStation.getStationTag() + " - " + recordingStation.getStationName());
            }*/

            return recordingStationsForAdapter;


        } else {

            return recordingStations;
        }

    }

    // Sets up the click listeners for the RecyclerView adapter
    public void setUpClickListeners(final Account account){


        /* OnItemClickLister for each item in the RecylerView. When an item in the RecylerView is clicked,
         * this sends an intent to open GraphActivity (while passing some information about the
         * object to GraphActivity within the intent)
         */
        adapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View itemView, int position) {

                int station_ID = adapter.getRecordingStation(position).getStationID();
                String station_name = adapter.getRecordingStation(position).getStationName();
                String station_tag = adapter.getRecordingStation(position).getStationTag();



                //Log.i("SERC Log", "");



                // Start Location Context Menu dialog fragment
                // Used to pass the title to the fragment
                Bundle args = new Bundle();
                args.putString("account_ID", account.getAccountID());
                args.putString("location_title", station_tag + " - " + station_name);
                args.putInt("location_ID", station_ID);
                args.putString("location_name", station_name);
                args.putString("location_tag", station_tag);
                // New instance of LocationContextMenu
                LocationContextMenu locationContextMenu = new LocationContextMenu();
                locationContextMenu.setArguments(args);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                locationContextMenu.show(ft, "location_context_menu_historical");
            }
        });



        /**
         * This is the listener for when a user long clicks/presses on an item in the listview.
         * This opens a Dialog showing all the information stored for the that RecordingStation object
         */

        adapter.setOnItemLongClickListener(new RecyclerViewAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View itemView, int position) {

                int station_ID = adapter.getRecordingStation(position).getStationID();
                String station_name = adapter.getRecordingStation(position).getStationName();
                String station_tag = adapter.getRecordingStation(position).getStationTag();
                int station_reading = adapter.getRecordingStation(position).getStationValueReading();
                long station_time = adapter.getRecordingStation(position).getStationTime();

                Bundle args = new Bundle();
                args.putInt("location_ID", station_ID);
                args.putString("location_name", station_name);
                args.putString("location_tag", station_tag);
                args.putInt("location_reading", station_reading);
                args.putLong("location_time", station_time);


                // New instance of LocationDetails
                LocationDetails locationDetails = new LocationDetails();
                locationDetails.setArguments(args);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                locationDetails.show(ft, "location_details_popup");

            }
        });

    }

    // Returns the diagonal screen size of the device in inches
    public double getScreenSize(){
        // Checks if screen size is more than 6.5 inches diagonally (which I consider to be a tablet)
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // Calculate screen size
        float yInches= metrics.heightPixels/metrics.ydpi;
        float xInches= metrics.widthPixels/metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches*xInches + yInches*yInches);

        return diagonalInches;
    }

    // Method to stop the SwipeRefreshLayout from spinning if it already spinning
    public void stopRefreshLayoutRefreshing(){
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    // Stop runnable once activity stops
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRepeatingAction();
        stopRefreshLayoutRefreshing();
    }

    // Stop runnable once activity stops
    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingAction();
        stopRefreshLayoutRefreshing();
    }


}
