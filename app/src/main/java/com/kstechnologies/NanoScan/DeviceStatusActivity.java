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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.SettingsManager;

/**
 * This activity controls the view for the Nano device status
 * This includes information such as Nano temp/humidity, and battery percentage
 *
 * @author collinmast
 */
public class DeviceStatusActivity extends Activity {

    private static Context mContext;

    private TextView tv_batt;
    private TextView tv_temp;
    private TextView tv_humid;
    private EditText et_tempThresh;
    private EditText et_humidThresh;

    private BroadcastReceiver mStatusReceiver;
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_status);

        mContext = this;

        //Set up the action bar title and enable the back arrow
        ActionBar ab = getActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.device_status));
        }

        //Get UI element references
        tv_batt = (TextView)findViewById(R.id.tv_batt);
        tv_temp = (TextView)findViewById(R.id.tv_temp);
        tv_humid = (TextView)findViewById(R.id.tv_humid);
        et_tempThresh = (EditText)findViewById(R.id.et_tempThresh);
        et_humidThresh = (EditText)findViewById(R.id.et_humidThresh);
        Button btn_update_thresholds = (Button) findViewById(R.id.btn_update_thresholds);

        //Set up threshold update button
        btn_update_thresholds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent thresholdUpdateIntent = new Intent(KSTNanoSDK.UPDATE_THRESHOLD);
                String tempString = et_tempThresh.getText().toString();
                String humidString = et_humidThresh.getText().toString();
                byte[] tempThreshBytes = {0, 0};
                byte[] humidThreshBytes = {0, 0};
                if (!tempString.equals("")) {
                    int tempThreshFloat = (int) (Float.parseFloat(tempString) * 100);

                    tempThreshBytes[0] = (byte) ((tempThreshFloat) & 0xFF);
                    tempThreshBytes[1] = (byte) (((tempThreshFloat) >> 8) & 0xFF);
                }
                if (!humidString.equals("")) {
                    int humidThreshFloat = (int) (Float.parseFloat(humidString) * 100);
                    humidThreshBytes[0] = (byte) ((humidThreshFloat) & 0xFF);
                    humidThreshBytes[1] = (byte) (((humidThreshFloat) >> 8) & 0xFF);
                }
                thresholdUpdateIntent.putExtra(KSTNanoSDK.EXTRA_TEMP_THRESH, tempThreshBytes);
                thresholdUpdateIntent.putExtra(KSTNanoSDK.EXTRA_HUMID_THRESH, humidThreshBytes);

                LocalBroadcastManager.getInstance(mContext).sendBroadcast(thresholdUpdateIntent);
            }
        });

        //The the hint of the temp threshold based on preferred temperature units
        if(!SettingsManager.getBooleanPref(this, SettingsManager.SharedPreferencesKeys.tempUnits,false)){
            et_tempThresh.setHint(R.string.deg_c);
        }else{
            et_tempThresh.setHint(R.string.deg_f);
        }

        //Send broadcast to get device status information from the BLE service
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.GET_STATUS));

        /*Set up receiver for device status information
         * The receiver recalculates the temperature in preferred units since it is always returned
         * in degrees Celsius
         */
        mStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int batt = intent.getIntExtra(KSTNanoSDK.EXTRA_BATT, 0);
                float temp = intent.getFloatExtra(KSTNanoSDK.EXTRA_TEMP, 0);
                tv_batt.setText(getString(R.string.batt_level_value, batt));
                if(!SettingsManager.getBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits, false)){
                    tv_temp.setText(getString(R.string.temp_value_c, Float.toString(temp)));
                }else{
                    temp = (float)(temp * 1.8)+32;
                    tv_temp.setText(getString(R.string.temp_value_f, Float.toString(temp)));
                }

                tv_humid.setText(getString(R.string.humid_value, intent.getFloatExtra(KSTNanoSDK.EXTRA_HUMID, 0)));

                ProgressBar pb = (ProgressBar)findViewById(R.id.pb_status);
                pb.setVisibility(View.INVISIBLE);
            }
        };

        //Register receivers for disconnection events and device status information
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mStatusReceiver, new IntentFilter(KSTNanoSDK.ACTION_STATUS));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(disconnReceiver, disconnFilter);
    }

    /*
     * On resume, make a call to the super class.
     * Nothing else is needed here besides calling
     * the super method.
     */
    @Override
    public void onResume(){
        super.onResume();
    }

    /*
     * When the activity is destroyed, unregister the BroadcastReceivers
     * handling disconnection and status events
     */
    @Override
    public void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mStatusReceiver);
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

        if(id == android.R.id.home){
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
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
