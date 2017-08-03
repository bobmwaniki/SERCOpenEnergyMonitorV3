package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Bob on 08/06/2017.
 * Custom adapter needed for the RecyclerView in MainActivityRecyclerView
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    // Member variable for the list of RecordingStation objects
    private ArrayList<RecordingStation> mRecordingStations;

    // Storing context for easy access
    private Context mContext;

    // Account
    private Account mCurrentAccount;

    // Needed to keep track which location for the new account is the first for the account identifier TextView
    private int mFirstLocation = 0;
    private ArrayList<Integer> mFirstLocationPositions = new ArrayList<>();
    private boolean newScreen = true;



    /************ Creating OnItemClickListener ************/
    // Listener member variable
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener{
        void onItemClick (View itemView, int position);
    }

    public interface OnItemLongClickListener{
        void onItemLongClick (View itemView, int position);
    }

    // Method that allows the parent Activity/fragment to define the listener
    public void setOnItemClickListener (OnItemClickListener clickListener){
        listener = clickListener;
    }

    public void setOnItemLongClickListener (OnItemLongClickListener clickListener){
        longClickListener = clickListener;
    }

    private void resetFirstLocations(){
        mFirstLocation = 0;
        mFirstLocationPositions.clear();
        mFirstLocationPositions.add(0);
    }
    private void addFirstLocation (int location){
        mFirstLocationPositions.add(location);
    }


    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView nameTextView;
        private TextView tagTextView;
        private TextView powerReadingTextView;
        private TextView accountIdentifierTextView;

        public ViewHolder(final View itemView) {
            super(itemView);

            // Get the various TextViews from list_item.xml
            nameTextView = (TextView) itemView.findViewById(R.id.station_name);
            tagTextView = (TextView) itemView.findViewById(R.id.station_tag);
            powerReadingTextView = (TextView) itemView.findViewById(R.id.station_power_reading);
            accountIdentifierTextView = (TextView) itemView.findViewById(R.id.account_identifier_textview);

            // Setup the click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Triggers click upwards to the adapter on click
                    if(listener != null){
                        int position = getAdapterPosition();
                        // Make sure position exists in RecyclerView
                        if (position != RecyclerView.NO_POSITION){
                            listener.onItemClick(itemView, position);
                        }
                    }
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(longClickListener != null){
                        int position = getAdapterPosition();
                        // Make sure position exists in RecyclerView
                        if (position != RecyclerView.NO_POSITION){
                            longClickListener.onItemLongClick(itemView, position);
                        }
                    }
                     /* This returns a boolean to indicate whether you have consumed the event and it
                     * should not be carried further. That is, return true to indicate that you have
                     * handled the event and it should stop here; return false if you have not handled
                     * it and/or the event should continue to any other on-click listeners.
                     * If false is returned, OnItemClickListener will be triggered resulting in onItemClick
                     * being triggered*/
                    return true;
                }
            });

        }
    }


    // Constructor for the Adapter
    public RecyclerViewAdapter(Context context, ArrayList<RecordingStation> recordingStations, Account account){
        mContext = context;
        mRecordingStations = recordingStations;
        mCurrentAccount = account;
        resetFirstLocations();

    }

    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout for the main activity
        View listItemView = inflater.inflate(R.layout.list_item, parent, false);

        // Return ViewHolder instance
        ViewHolder viewHolder = new ViewHolder(listItemView);
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (mRecordingStations.size()>0) {
            Log.i(" SERC Adapter", "mFirstLocation onBindViewHolder: " + mFirstLocation);
            // Get the RecordingStation based on position
            RecordingStation recordingStation = mRecordingStations.get(position);

            // Set TextViews based on the attributes of the RecordingStation
            TextView nameTV = holder.nameTextView;
            TextView tagTV = holder.tagTextView;
            TextView powerReadingTV = holder.powerReadingTextView;
            TextView accountIdentifierTV = holder.accountIdentifierTextView;



            //Account identifier
            if(mFirstLocationPositions.contains(position)){
                accountIdentifierTV.setVisibility(View.VISIBLE);
                if (position == mFirstLocationPositions.get(mFirstLocationPositions.size() - 1)) {
                    Log.i("SERC Log","Current Account set: " + mCurrentAccount.getAccountName());
                    accountIdentifierTV.setText(mCurrentAccount.getAccountName());
                }
            }else {
                accountIdentifierTV.setVisibility(View.GONE);
            }


            SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean switchNameTag = appSettings.getBoolean("pref_general_switch_name_tag", false);

            if (switchNameTag) {
                tagTV.setText(recordingStation.getStationName());
                nameTV.setText(recordingStation.getStationTag());

            } else {
                nameTV.setText(recordingStation.getStationName());
                tagTV.setText(recordingStation.getStationTag());
            }
            powerReadingTV.setText(String.valueOf(recordingStation.getStationValueReading()) + " W");
        }

    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return mRecordingStations.size();
    }

    public RecordingStation getRecordingStation(int position){
        return mRecordingStations.get(position);
    }

    /*
    * The following 3 classes are used to help clear and load new data to the adapter
    */
    // Clear all elements of the recycler
    public void smoothClear(){
        int size = mRecordingStations.size();
        mRecordingStations.clear();
        resetFirstLocations();
        newScreen = true;

        for (int i=0; i<size; i++){
            notifyItemRemoved(i);
        }

    }

    public void clear(){
        mRecordingStations.clear();
        resetFirstLocations();
        newScreen = true;
        notifyDataSetChanged();

    }

    // Add list of items
    public void addAll(ArrayList<RecordingStation> recordingStations, Account account){
        mCurrentAccount = account;
//        Log.i("SERC Log","Account received from addAll: " + account.getAccountName());
        if (newScreen) {
            newScreen = false;
        }
        else{
            mFirstLocation += mRecordingStations.size();
            addFirstLocation(mFirstLocation);
        }
//        Log.i(" SERC Adapter", "mFirstLocation addAll: " + mFirstLocation);
        for (int i=0; i<recordingStations.size(); i++){
            mRecordingStations.add(recordingStations.get(i));
            notifyItemChanged(i);
        }

    }

    public void notifyMassDataChange(ArrayList<RecordingStation> recordingStations, boolean clearScreen, Account account){
        mCurrentAccount = account;
        if (clearScreen) {
            mRecordingStations.clear();
            mRecordingStations = recordingStations;
            resetFirstLocations();
            newScreen = true;
            notifyDataSetChanged();
        }
        else {
            if (newScreen) {
                newScreen = false;
            }
            else{
                mFirstLocation += mRecordingStations.size();
                addFirstLocation(mFirstLocation);
            }
//            Log.i(" SERC Adapter", "mFirstLocation notifyMassDataChange: " + mFirstLocation);
            for(RecordingStation station:recordingStations){
                mRecordingStations.add(station);
            }
            notifyDataSetChanged();
        }

    }


}

