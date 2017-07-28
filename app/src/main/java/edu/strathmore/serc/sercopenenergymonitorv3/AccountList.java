package edu.strathmore.serc.sercopenenergymonitorv3;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class AccountList extends AppCompatActivity {

    private AccountListAdapter adapter;
    //private ArrayList<Account> accounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_list);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        AccountConfig accountConfig = new AccountConfig(this);
        ArrayList<Account> accounts = accountConfig.getAllAccounts();


        ListView listView = (ListView) findViewById(R.id.account_list_list_view);
        adapter = new AccountListAdapter(this, accounts);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String accountID = adapter.getItem(position).getAccountID();

                Intent openAccountDetails = new Intent(getBaseContext(), AccountSettings.class);
                openAccountDetails.putExtra("account_ID", accountID);
                startActivity(openAccountDetails);
            }
        });

        LinearLayout addAccountBtn = (LinearLayout) findViewById(R.id.add_account_btn);
        addAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent openAccountSettings = new Intent(getBaseContext(), AccountSettings.class);
                openAccountSettings.putExtra("new_account", true);
                startActivity(openAccountSettings);
            }
        });





    }

    private void refreshData(){
        AccountConfig accountConfig = new AccountConfig(this);
        ArrayList<Account> accounts = accountConfig.getAllAccounts();
        adapter.clear();
        adapter.addAll(accounts);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id){

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;




            default:
                return super.onOptionsItemSelected(item);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData();

    }



    public class AccountListAdapter extends ArrayAdapter<Account> {



        public AccountListAdapter(Context context, ArrayList<Account> accounts){
            // The constructor ArrayAdapter(Context context, int resource, T[] objects) has been used
            // Since no resource id is used, we pass a 0 (generic) to avoid errors
            super(context, 0, accounts);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            // Check if the existing View is being reused, otherwise inflate a new view
            View listItemView = convertView;
            if(listItemView == null){
                listItemView = LayoutInflater.from(getContext()).inflate(
                        R.layout.account_list_item, parent, false);
            }

            // Get the recording station object at this position on the list
            Account currentAccount = getItem(position);

            //Set TextView to Account Name
            TextView accountName = (TextView) listItemView.findViewById(R.id.account_list_account_name);
            accountName.setText(currentAccount.getAccountName());

            // Return the ListView containing the 3 TextViews
            return listItemView;
        }
    }
}
