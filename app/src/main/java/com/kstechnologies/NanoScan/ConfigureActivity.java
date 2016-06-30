package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import android.widget.ListView;

import com.kstechnologies.nirscannanolibrary.*;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;

/**
 * This activity controls the view for settings once the Nano is connected
 * Four options are presented, each one launching a new activity.
 * Since each option requires the Nano to be connected to perform GATT operations,
 *
 * @author collinmast
 */
public class ConfigureActivity extends Activity {

    private static Context mContext;

    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        mContext = this;

        //Set the action bar title and enable the back button
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.configure));
        }

        //Get reference to listview and add the click listener
        ListView lv_configure = (ListView) findViewById(R.id.lv_configure);
        lv_configure.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                switch (i) {
                    case 0:
                        Intent infoIntent = new Intent(mContext, DeviceInfoActivity.class);
                        startActivity(infoIntent);
                        break;
                    case 1:
                        Intent statusIntent = new Intent(mContext, DeviceStatusActivity.class);
                        startActivity(statusIntent);
                        break;
                    case 2:
                        Intent confIntent = new Intent(mContext, ScanConfActivity.class);
                        startActivity(confIntent);
                        break;
                    case 3:
                        Intent scanDataIntent = new Intent(mContext, StoredScanDataActivity.class);
                        startActivity(scanDataIntent);
                        break;
                    default:
                        break;
                }
            }
        });

        //Register the disconnect broadcast receiver
        LocalBroadcastManager.getInstance(mContext).registerReceiver(disconnReceiver, disconnFilter);
    }

    /*
     * On resume, make a call to the super class.
     * Nothing else is needed here besides calling
     * the super method.
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /*
     * When the activity is destroyed, unregister the BroadcastReceiver
     * handling disconnection events.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(disconnReceiver);
    }

    /*
     * Inflate the options menu
     * In this case, there is no menu and only an up indicator,
     * so the function should always return true.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, there is only the up indicator. If selected, this activity should finish.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the {@link ScanListActivity}
     */
    public class DisconnReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }
}
