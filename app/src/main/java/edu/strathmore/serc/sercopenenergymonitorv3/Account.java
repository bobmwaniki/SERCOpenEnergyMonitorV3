package edu.strathmore.serc.sercopenenergymonitorv3;

import java.util.Collections;
import java.util.Set;

/**
 * Created by Bob on 19/07/2017.
 */

public class Account {
    private String mAccountID;
    private String mAccountName;
    private String mApikey;
    private String mRootLink;
    private Set<String> mStationList = Collections.emptySet();

    // Constructor
    public Account(String accountID, String accountName, String apiKey, String rootLink){
        mAccountID = accountID;
        mAccountName = accountName;
        mApikey = apiKey;
        mRootLink = rootLink;

    }

    // Empty constructor
    public Account(){

    }

    // Getter methods

    public String getAccountID(){return mAccountID;}

    public String getAccountName(){return mAccountName;}

    public String getApiKey(){return mApikey;}

    public String getRootLink(){return mRootLink;}

    public Set<String> getStationList(){return mStationList;}


    // Setter methods

    // public void setAccountID(String accountID){mAccountID = accountID;;}

    public void setAccountName(String accountName){mAccountName = accountName;}

    public void setApiKey(String apiKey){mApikey = apiKey;}

    public void setRootLink(String rootLink){mRootLink = rootLink;}

    public void setStationList(Set<String> stationList){mStationList = stationList;}



}

