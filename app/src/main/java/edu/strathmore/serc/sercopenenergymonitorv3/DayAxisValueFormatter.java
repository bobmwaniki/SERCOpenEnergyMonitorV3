package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Bob on 24/03/2017.
 */

public class DayAxisValueFormatter implements IValueFormatter, IAxisValueFormatter {

    BarLineChartBase<?> chart;
    private Context mContext;
    //private SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

    public DayAxisValueFormatter(BarLineChartBase<?> chart, Context context) {

        this.chart = chart;
        mContext = context;
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return null;
    }


    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        //Log.i("SERC Log", "Time Stamp float value: " + String.valueOf(value));
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);

        int userChoice = Integer.valueOf(appSettings.getString("graph_x_axis_time_date", "3")) ;

        return getDateTime(userChoice, value);


    }

    private String getDateTime(int choice, float value){

        //String stringValue = String.valueOf(value);
        //Log.i("SERC Log", "Time Stamp long value: " + String.valueOf((long) value));
        SimpleDateFormat sdf;
        Long longValue = (long) value;
        Log.i("SERC Log", "Time Stamp long value: " + String.valueOf(longValue));

        if (choice == 1){
            sdf = new SimpleDateFormat("dd/MM/yyyy");
        } else if(choice == 2){
            sdf = new SimpleDateFormat("h:mm a");
        } else {
            sdf = new SimpleDateFormat("h:mm a \n dd/MM/yyyy");
        }

       // Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis((long) value);
        //Log.i("SERC Log", "Time Stamp date: " + sdf.format(calendar.getTime()));
        //return sdf.format(calendar.getTime());


        //Date date = new Date(Long.parseLong(stringValue));
        //return sdf.format(date);
        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
        Long reference_timestamp = appSettings.getLong("reference_timestamp", 0L);

        Date date = new Date (longValue+ reference_timestamp);
        Log.i("SERC Log", "Time Stamp date: " + sdf.format(date));
        return sdf.format(date);
    }
}