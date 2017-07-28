package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Bob on 21/07/2017.
 */

public class AccountConfig {

    private Context mContext;

    // For multiple account settings
    private static final String PREF_ALL_ACCOUNTS_LIST  = "emon_acc_ID_list";
    private static final String PREF_ACCOUNT_PREFIX  = "emon_acc_";



    // Constructor
    public AccountConfig(Context context){
        mContext = context;
    }

    public String addAccount() {

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

        String accountID = UUID.randomUUID().toString();
        String [] allAccList = getAccountIDArray();
        String accountName;
        if (allAccList.length<=1) {
            accountName = "Emoncms Acc " + (allAccList.length);
        } else {
            accountName = "Emoncms Acc " + (allAccList.length + 1);
        }

        // Add it to the full list of IDs
        String allAccountIDs = appSettings.getString(PREF_ALL_ACCOUNTS_LIST, "");
        allAccountIDs = allAccountIDs + accountID + ",";
        SharedPreferences.Editor editor = appSettings.edit();
        editor.putString(PREF_ALL_ACCOUNTS_LIST, allAccountIDs);
        editor.apply();


        String accountNamingConvention = PREF_ACCOUNT_PREFIX + accountID + "_";

        //Add Name, API Key and Root link for this account to settings
        editor.putString(accountNamingConvention + "name", accountName);
        editor.putString(accountNamingConvention + "apiKey", "");
        editor.putString(accountNamingConvention + "rootLink", "");
        editor.apply();

        return accountID;
    }

    public void removeAccount(String accountID){
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = appSettings.edit();

        // Remove ID from list of IDs
        String[] accListArray = getAccountIDArray();
        String newString = "";
        for (String id:accListArray){
            if(!id.isEmpty()){
                if(!id.contains(accountID)){
                    newString = newString + id + ",";
                }
            }
        }
        editor.putString(PREF_ALL_ACCOUNTS_LIST, newString);
        editor.apply();


        // Remove name, apiKey and rootLink from SharedPreferences
        Map<String, ?> allSettings = appSettings.getAll();
        for (Map.Entry<String, ?> entry: allSettings.entrySet()){
            if(entry.getKey().contains(accountID)){
                editor.remove(entry.getKey());
                editor.apply();
            }
        }


    }

    public void saveAccountDetailsInSettings(Account account){
        // Save the details in Settings
        String id = account.getAccountID();
        setAccountNameInSettings(id, account.getAccountName());
        setApiKeyInSettings(id, account.getApiKey());
        setRootLinkInSettings(id, account.getRootLink());

    }

    public void setAccountNameInSettings(String accountID, String accountName){

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = appSettings.edit();

        String name_key = PREF_ACCOUNT_PREFIX + accountID + "_name";

        editor.putString(name_key, accountName);
        editor.apply();

    }

    public void setApiKeyInSettings(String accountID, String apiKey){

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = appSettings.edit();

        String apiKey_key = PREF_ACCOUNT_PREFIX + accountID + "_apiKey";

        editor.putString(apiKey_key, apiKey);
        editor.apply();

    }

    public void setRootLinkInSettings(String accountID, String rootLink){

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = appSettings.edit();

        String rootLink_key = PREF_ACCOUNT_PREFIX + accountID + "_rootLink";

        editor.putString(rootLink_key, rootLink);
        editor.apply();

    }

    public void setStationListInSettings(String accountID, Set<String> stationList){

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = appSettings.edit();

        String stationList_key = PREF_ACCOUNT_PREFIX + accountID + "_stationList";

        editor.putStringSet(stationList_key, stationList);
        editor.apply();

    }

    // Gets all the accounts available in settings
    public ArrayList<Account> getAllAccounts(){

        ArrayList<Account> allAccounts = new ArrayList<>();

        String[] accountIdList = getAccountIDArray();


        // For each UUID
        for (String accountID : accountIdList) {
            // Make sure it's not blank (like at the end of the list)
            if (!accountID.trim().isEmpty()) {

                // Get the Account object from the ID and add to the array
                allAccounts.add(getAccountFromID(accountID));

            }
        }

        return allAccounts;
    }

    // Get String array that has all the current saved accounts
    public String[] getAccountIDArray(){
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        String allAccountIDs = appSettings.getString(PREF_ALL_ACCOUNTS_LIST, "");
        return allAccountIDs.split(",");
    }

    public Account getAccountFromID(String accountID){

        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        // Get all Settings
        Map<String,?> appSettingsAll =  appSettings.getAll();

        String accountName = "";
        String apiKey = "";
        String rootLink = "";
        Set<String> stationsSet = Collections.emptySet();
        // For each setting
        for(Map.Entry<String,?> entry:appSettingsAll.entrySet()) {
            // If UUID matches
            if(entry.getKey().contains(accountID)){

                // Get the Account name if available
                if(entry.getKey().contains("name")){
                    accountName = entry.getValue().toString();
                }
                // Get Account's api key if available
                if(entry.getKey().contains("api")){
                    apiKey = entry.getValue().toString();
                }
                // Get Account's root link if available
                if(entry.getKey().contains("rootLink")){
                    rootLink = entry.getValue().toString();
                }
                // Get Account's list of available stations
                if(entry.getKey().contains("stationList")){
                    stationsSet = (Set<String>) entry.getValue();
                }

            }

        }

        Account account = new Account(accountID, accountName, apiKey, rootLink);
        account.setStationList(stationsSet);
        return account;

    }

    public void removeAllAccountData() {
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        // Removes all account data
        SharedPreferences.Editor editor = appSettings.edit();
        Map<String, ?> allSettings = appSettings.getAll();
        for (Map.Entry<String, ?> entry : allSettings.entrySet()) {
            if (entry.getKey().contains(PREF_ACCOUNT_PREFIX)) {
                editor.remove(entry.getKey());
                editor.apply();
            }

        }
    }

}
