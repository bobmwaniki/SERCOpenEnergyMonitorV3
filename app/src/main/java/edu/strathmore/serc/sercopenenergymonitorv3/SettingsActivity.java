package edu.strathmore.serc.sercopenenergymonitorv3;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {



    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

    }



    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);

        }
    }

    // To enable up button on SettingsActivity to back to MainActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || GraphPreferenceFragment.class.getName().equals(fragmentName)
                || AccountPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        // Declaring some global variables to be used in the onCreate method
       /* MultiSelectListPreference stationMultiSelect;
        SharedPreferences appSettings;
        Set<String> recordingStationsSet;
        Set<String> recordingStationsFullSet;*/


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            //bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("pref_update_frequency"));
            //bindPreferenceSummaryToValue(findPreference("graph_zero_listpref"));


            /** Gets the MultiSelectListPreference and adds the String set stored in selected_station_list
             * to its entries and entry values. The selected_station_list string set is set on the
             * onCreate method of the in the main activity
             *//*
            stationMultiSelect = (MultiSelectListPreference) findPreference("stations_multi_list");
            appSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            recordingStationsSet = appSettings.getStringSet("selected_station_list", Collections.<String>emptySet());
            recordingStationsFullSet = appSettings.getStringSet("full_station_list", Collections.<String>emptySet());

            AccountConfig accountConfig = new AccountConfig(getActivity().getBaseContext());
            ArrayList<Account> accounts = accountConfig.getAllAccounts();
            for (Account account:accounts){
                if (!account.getStationList().isEmpty()) {
                    recordingStationsFullSet.addAll(account.getStationList());
                }
            }



            // Sizes of the String sets
            int recordingStationsSetSize = recordingStationsSet.size();
            int recordingStationsFullSetSize = recordingStationsFullSet.size();


            Log.i("SERC Log", "MultiSelectPreference Entry Value length: "+ String.valueOf(stationMultiSelect.getEntryValues().length));
            Log.i("SERC Log", "SharedPreferrences Names Set length: "+ String.valueOf(recordingStationsSetSize));



            // Logging entries in the list
            for (int i=0; i<recordingStationsSetSize; i++){
                Log.i("SERC Log:", "selected_station_list_ID " + String.valueOf(i) + ": "+ String.valueOf(recordingStationsSet.toArray()[i]));
            }
            for (int i=0; i<recordingStationsFullSetSize; i++){
                Log.i("SERC Log:", "full_station_list_ID " + String.valueOf(i) + ": "+ String.valueOf(recordingStationsFullSet.toArray()[i]));
            }



            // Converting the String Sets to String Arrays
            String[] valuesArray = recordingStationsSet.toArray(new String[recordingStationsSetSize]);
            String[] entriesArray = recordingStationsFullSet.toArray(new String[recordingStationsFullSetSize]);

            Arrays.sort(valuesArray);
            Arrays.sort(entriesArray);


            // Setting selected_station_list to the entries and selected_station_list_ID to the values
            stationMultiSelect.setEntries(entriesArray);
            stationMultiSelect.setEntryValues(entriesArray);


            // OnClickListener for the Station List chooser
            stationMultiSelect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    // Edit the SharePreferences as well
                    SharedPreferences.Editor editor = appSettings.edit();
                    editor.putStringSet("selected_station_list", (Set<String>) newValue);

                    // Logging new entries in the settings
                    for (int i=0; i<((Set<String>) newValue).size(); i++){
                        Log.i("SERC Log:", "Update selected_station_list on click " + String.valueOf(i) + ": "+ String.valueOf(((Set<String>) newValue).toArray()[i]));
                    }

                    // Apply the settings
                    editor.apply();
                    return true;
                }
            });
*/

        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }


    /**
     * This fragment shows graph preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GraphPreferenceFragment extends PreferenceFragment{
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_graph);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("pref_graph_line_style"));
            bindPreferenceSummaryToValue(findPreference("graph_x_axis_position_listpref"));
            bindPreferenceSummaryToValue(findPreference("graph_x_axis_angle_listpref"));
            bindPreferenceSummaryToValue(findPreference("graph_default_duration_listpref"));
            bindPreferenceSummaryToValue(findPreference("graph_x_axis_time_date"));


        }


        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }



    /**
     * This fragment shows account preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class AccountPreferenceFragment extends PreferenceFragment{
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_account);
            setHasOptionsMenu(true);

            final Context context = getActivity().getBaseContext();




            MultiSelectListPreference chosenAccounts = (MultiSelectListPreference) findPreference("pref_selected_accounts");
            //setUpMultiSelectPreference();

            chosenAccounts.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                    SharedPreferences.Editor editor = appSettings.edit();
                    editor.putStringSet("selected_account_list", (Set<String>) newValue);
                    for (String id:(Set<String>) newValue) {
                        Log.i("SERC Log", "Saved account id: " + id);
                    }
                    // Apply the settings
                    editor.apply();
                    return true;
                }
            });





            // Show the add account button
            Preference addAccount = findPreference("add_account_button");
            addAccount.setLayoutResource(R.layout.add_account);
            addAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent showAccountDetails = new Intent(context, AccountSettings.class);
                    showAccountDetails.putExtra("new_account", true);
                    startActivity(showAccountDetails);

                    return true;
                }
            });

            // Add open Accounts list page on button press
            Preference showAllAccounts = findPreference("show_account_list");
            showAllAccounts.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    Intent openAccountList = new Intent (context, AccountList.class);
                    startActivity(openAccountList);

                    return true;
                }
            });

            setUpLocationsMultipreference();



        }

        private void setUpAccountMultiSelectPreference(){
            MultiSelectListPreference accountMultiPref = (MultiSelectListPreference) findPreference("pref_selected_accounts");
            AccountConfig accountConfig = new AccountConfig(getActivity().getBaseContext());
            String[] accountIDs = accountConfig.getAccountIDArray();
            int accountIDListSize = accountIDs.length;
            // String sets to be used in the MultiSelectListPreference
            if (accountIDListSize>0) {
                String[] accountNames = new String[accountIDListSize];
                // Fill the String array and Set with values
                for (int i=0; i<accountIDListSize;i++){
                    if(!accountIDs[i].isEmpty()){
                        // Get Account Name from ID
                        accountNames[i] = accountConfig.getAccountFromID(accountIDs[i]).getAccountName();

                    }
                }

                accountMultiPref.setEntries(accountNames);
                accountMultiPref.setEntryValues(accountIDs);
            }


        }

        private void setUpLocationsMultipreference(){

            /* Gets the MultiSelectListPreference and adds the String set stored in selected_station_list
             * to its entries and entry values. The selected_station_list string set is set on the
             * onCreate method of the in the main activity
             */
            MultiSelectListPreference stationMultiSelect = (MultiSelectListPreference) findPreference("stations_multi_list");
            final SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Set<String> recordingStationsSet = appSettings.getStringSet("selected_station_list", new HashSet<String>());
            Set<String> recordingStationsFullSet = appSettings.getStringSet("full_station_list", new HashSet<String>());

            //AccountConfig accountConfig = new AccountConfig(getActivity().getBaseContext());
            AccountConfig accountConfig = new AccountConfig(getActivity().getBaseContext());
            ArrayList<Account> accounts = accountConfig.getAllAccounts();
            if (!accounts.isEmpty()) {
                for (Account account:accounts){
                    if (!account.getStationList().isEmpty()) {
                        recordingStationsFullSet.addAll(account.getStationList());
                    }
                }
            }


            // Sizes of the String sets
            int recordingStationsSetSize = recordingStationsSet.size();
            int recordingStationsFullSetSize = recordingStationsFullSet.size();


            Log.i("SERC Log", "MultiSelectPreference Entry Value length: "+ String.valueOf(stationMultiSelect.getEntryValues().length));
            Log.i("SERC Log", "SharedPreferrences Names Set length: "+ String.valueOf(recordingStationsSetSize));



            // Logging entries in the list
            for (int i=0; i<recordingStationsSetSize; i++){
                Log.i("SERC Log:", "selected_station_list_ID " + String.valueOf(i) + ": "+ String.valueOf(recordingStationsSet.toArray()[i]));
            }
            for (int i=0; i<recordingStationsFullSetSize; i++){
                Log.i("SERC Log:", "full_station_list_ID " + String.valueOf(i) + ": "+ String.valueOf(recordingStationsFullSet.toArray()[i]));
            }



            // Converting the String Sets to String Arrays
            String[] valuesArray = recordingStationsSet.toArray(new String[recordingStationsSetSize]);
            String[] entriesArray = recordingStationsFullSet.toArray(new String[recordingStationsFullSetSize]);

            Arrays.sort(valuesArray);
            Arrays.sort(entriesArray);


            // Setting selected_station_list to the entries and selected_station_list_ID to the values
            stationMultiSelect.setEntries(entriesArray);
            stationMultiSelect.setEntryValues(entriesArray);


            // OnClickListener for the Station List chooser
            stationMultiSelect.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    // Edit the SharePreferences as well
                    SharedPreferences.Editor editor = appSettings.edit();
                    editor.putStringSet("selected_station_list", (Set<String>) newValue);

                    // Logging new entries in the settings
                    for (int i=0; i<((Set<String>) newValue).size(); i++){
                        Log.i("SERC Log:", "Update selected_station_list on click " + String.valueOf(i) + ": "+ String.valueOf(((Set<String>) newValue).toArray()[i]));
                    }

                    // Apply the settings
                    editor.apply();
                    return true;
                }
            });

        }


        @Override
        public void onResume() {
            super.onResume();
            setUpAccountMultiSelectPreference();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    }







    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
