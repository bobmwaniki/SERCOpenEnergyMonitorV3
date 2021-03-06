package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import com.codemybrainsout.onboarder.AhoyOnboarderActivity;
import com.codemybrainsout.onboarder.AhoyOnboarderCard;

import java.util.ArrayList;
import java.util.List;

public class HelpActivity extends AhoyOnboarderActivity {

    private boolean helpPageShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(this);


        AhoyOnboarderCard ahoyOnboarderCard1 = new AhoyOnboarderCard("Welcome",
                "This app is has been created to monitor energy use remotely for multiple locations using the Emoncms platform",
                R.drawable.lightning_icon);
        AhoyOnboarderCard ahoyOnboarderCard2 = new AhoyOnboarderCard("Current Data",
                "To get the most current reading: \nSwipe down to refresh the data\nOR\nActivate live data mode",
                R.drawable.swipe_down_illust_2);
        AhoyOnboarderCard ahoyOnboarderCard3 = new AhoyOnboarderCard("Graph",
                "Click on any location to access its historical or live graph",
                R.drawable.easel_icon);
        AhoyOnboarderCard ahoyOnboarderCard4 = new AhoyOnboarderCard("Account",
                "Please note that in order to communicate with the platform, you will be requested to provide some account information",
                R.drawable.color_profile_icon);
        AhoyOnboarderCard ahoyOnboarderCard5 = new AhoyOnboarderCard("API Key",
                "The first is the READ API key, which can be found by logging into your Emoncms account under \'My Account\'",
                R.drawable.key_icon);
        AhoyOnboarderCard ahoyOnboarderCard6 = new AhoyOnboarderCard("Root Link",
                "The second is the HTTP/HTTPS link for your server where the platform is hosted",
                R.drawable.web_http_icon);
        AhoyOnboarderCard ahoyOnboarderCard7 = new AhoyOnboarderCard("More Info",
                "To access the settings, click on the 3 dot menu on the top right of the main screen",
                R.drawable.three_dot_blue);


        List<AhoyOnboarderCard> pages = new ArrayList<>();

        pages.add(ahoyOnboarderCard1);
        pages.add(ahoyOnboarderCard2);
        pages.add(ahoyOnboarderCard3);
        pages.add(ahoyOnboarderCard4);
        pages.add(ahoyOnboarderCard5);
        pages.add(ahoyOnboarderCard6);
        pages.add(ahoyOnboarderCard7);

        for (AhoyOnboarderCard page : pages) {
            page.setBackgroundColor(R.color.black_transparent);
            page.setTitleColor(R.color.white);
            page.setDescriptionColor(R.color.grey_200);
            page.setTitleTextSize(dpToPixels(10, this));
            page.setDescriptionTextSize(dpToPixels(6, this));
            //page.setIconLayoutParams(width, height, marginTop, marginLeft, marginRight, marginBottom);
        }

        helpPageShown = appSettings.getBoolean("help_page_shown",false);
        // Provides a different button depending on whether or not the help page has been shown before
        if (helpPageShown) {
            setFinishButtonTitle("Finish");
        } else{
            setFinishButtonTitle("Get Started");
        }


        showNavigationControls(true);
        setGradientBackground();

        //set the button style you created
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setFinishButtonDrawableStyle(ContextCompat.getDrawable(this, R.drawable.rounded_button));
        }



        setOnboardPages(pages);

        // Save in the preference that the help page has been shown
        SharedPreferences.Editor editor = appSettings.edit();
        editor.putBoolean("help_page_shown", true);
        editor.apply();
    }

    @Override
    public void onFinishButtonPressed() {

        if (helpPageShown) {
            finish();
        } else {
            Intent startMainPage = new Intent(getApplicationContext(), MainActivityRecyclerView.class);
            startMainPage.putExtra("first_launch", true);
            startActivity(startMainPage);
        }
        /*Intent upIntent = NavUtils.getParentActivityIntent(HelpActivity.this);
        if (NavUtils.shouldUpRecreateTask(HelpActivity.this, upIntent)) {
            // This activity is NOT part of this app's task, so create a new task
            // when navigating up, with a synthesized back stack.

            TaskStackBuilder.create(getBaseContext())
                    // Add all of this activity's parents to the back stack
                    .addNextIntentWithParentStack(upIntent)
                    // Navigate up to the closest parent
                    .startActivities();
        } else {
            // This activity is part of this app's task, so simply
            // navigate up to the logical parent activity.
            NavUtils.navigateUpTo(HelpActivity.this, upIntent);
        }*/

        /*Intent goToMain = new Intent(this, MainActivityRecyclerView.class);
        startActivity(goToMain);*/
    }
}
