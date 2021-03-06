package edu.strathmore.serc.sercopenenergymonitorv3;


import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import mehdi.sakout.fancybuttons.FancyButton;

public class AccountSettings extends AppCompatActivity {

    private String account_ID;
    private EditText account_name;
    private EditText account_api;
    private EditText account_link ;
    private boolean newAccount;
    private boolean noAccountFound;
    private boolean firstTimeLaunch;

    private Account account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Bundle extras = getIntent().getExtras();
        if(extras!=null){
            newAccount = extras.getBoolean("new_account", false);
            account_ID = extras.getString("account_ID", null);
            noAccountFound = extras.getBoolean("no_account_found", false);
            firstTimeLaunch = extras.getBoolean("first_time_launch", false);
        }

        account_name = (EditText) findViewById(R.id.account_name);
        account_api = (EditText) findViewById(R.id.account_api);
        account_link = (EditText) findViewById(R.id.account_emon_server_link);

        final AccountConfig accountConfig = new AccountConfig(this);

        if (newAccount || noAccountFound || firstTimeLaunch){
            account_ID = accountConfig.addAccount();
            account = accountConfig.getAccountFromID(account_ID);
            setEditTextBoxes();
        }
        else if (account_ID != null){
            account = accountConfig.getAccountFromID(account_ID);
            setEditTextBoxes();
        }


        FancyButton saveButton = (FancyButton) findViewById(R.id.btn_save_account);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Get data from the Edit Text Fields
                account.setAccountName(account_name.getText().toString());
                account.setApiKey(account_api.getText().toString());
                account.setRootLink(account_link.getText().toString());


                // Save details in Settings
                accountConfig.saveAccountDetailsInSettings(account);




                    /*SharedPreferences sharePrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = sharePrefs.edit();
                    Set<String> accountIDs = new HashSet<String>();
                    accountIDs.add(account.getAccountID());
                    editor.putStringSet("selected_account_list",accountIDs);*/


                if (firstTimeLaunch) {
                    /*SharedPreferences sharePrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = sharePrefs.edit();
                    Set<String> accountIDs = new HashSet<String>();
                    accountIDs.add(account.getAccountID());
                    editor.putStringSet("selected_account_list",accountIDs);*/
                    Toast.makeText(getBaseContext(), "Account set up. Go to Account Settings to choose which locations to display",
                            Toast.LENGTH_LONG).show();
                    Intent startMainActivity = new Intent(getBaseContext(), MainActivityRecyclerView.class);
                    startActivity(startMainActivity);
                }
                else {


                    //Go Back to
                    Intent upIntent = NavUtils.getParentActivityIntent(AccountSettings.this);
                    if (NavUtils.shouldUpRecreateTask(AccountSettings.this, upIntent)) {
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
                        NavUtils.navigateUpTo(AccountSettings.this, upIntent);
                    }

                    if (noAccountFound){
                        Toast.makeText(getBaseContext(), "Remember to choose which stations to display from General settings",
                                Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(getBaseContext(), "Account details saved", Toast.LENGTH_SHORT).show();

                    }

                }
            }


        });
    }

    private void setEditTextBoxes(){
        account_name.setText(account.getAccountName());
        account_api.setText(account.getApiKey());
        account_link.setText(account.getRootLink());
    }

    // Creates the menu from the xml layout
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id){
            case (R.id.action_delete):
                DeleteConfirmationDialog deleteDialog = new DeleteConfirmationDialog();
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                deleteDialog.show(ft, "");

                return true;

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    // This activity is NOT part of this app's task, so create a new task
                    // when navigating up, with a synthesized back stack.
                    TaskStackBuilder.create(this)
                            // Add all of this activity's parents to the back stack
                            .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                            .startActivities();
                } else {
                    // This activity is part of this app's task, so simply
                    // navigate up to the logical parent activity.
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;




            default:
                return super.onOptionsItemSelected(item);
        }


    }


    public static class DeleteConfirmationDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Delete Account Details")
                    .setMessage("Are you sure you want to delete this entry?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Delete the account
                            AccountConfig accountConfig = new AccountConfig(getContext());
                            accountConfig.removeAccount( ((AccountSettings)getActivity()).account.getAccountID() );
                            Toast.makeText(getContext(), "Account Deleted", Toast.LENGTH_SHORT).show();
                            getActivity().finish();

                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                            Toast.makeText(getContext(), "Account Deletion cancelled", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert);
            // Create the AlertDialog object and return it
            return builder.create();
        }



    }


}
