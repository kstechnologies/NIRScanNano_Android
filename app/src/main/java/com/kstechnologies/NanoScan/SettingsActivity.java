package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.kstechnologies.nirscannanolibrary.SettingsManager;

/**
 * This activity controls the view for global settings. These settings do not require a Nano
 * to be connected.
 *
 * The user can change temperature and spatial frequency units, as well as set and clear a
 * preferred Nano device
 *
 * @author collinmast
 */
public class SettingsActivity extends Activity {

    private TextView tv_version;
    private ToggleButton tb_temp;
    private ToggleButton tb_spatial;
    private Button btn_set;
    private Button btn_forget;
    private AlertDialog alertDialog;
    private TextView tv_pref_nano;
    private String preferredNano;

    private static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mContext = this;

        //Set up action bar up indicator
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        //Get references to UI elements
        tv_version = (TextView) findViewById(R.id.tv_version);
        tb_temp = (ToggleButton) findViewById(R.id.tb_temp);
        tb_spatial = (ToggleButton) findViewById(R.id.tb_spatial);
        btn_set = (Button) findViewById(R.id.btn_set);
        btn_forget = (Button) findViewById(R.id.btn_forget);
        tv_pref_nano = (TextView) findViewById(R.id.tv_pref_nano);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Initialize preferred device
        preferredNano = SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null);

        //Retrieve package information for displaying version info
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            tv_version.setText(getString(R.string.version, version, versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            tv_version.setText("");
        }

        //Initialize UI toggle element states, and create event listeners
        tb_temp.setChecked(SettingsManager.getBooleanPref(this, SettingsManager.SharedPreferencesKeys.tempUnits, SettingsManager.CELSIUS));
        tb_spatial.setChecked(SettingsManager.getBooleanPref(this, SettingsManager.SharedPreferencesKeys.spatialFreq, SettingsManager.WAVELENGTH));

        tb_temp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsManager.storeBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.tempUnits, b);
            }
        });

        tb_spatial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SettingsManager.storeBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.spatialFreq, b);
            }
        });

        btn_set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //When trying to set a preferred Nano, start the bluetooth scanner so that a device can be chosen
                startActivity(new Intent(mContext, ScanActivity.class));
            }
        });

        if(preferredNano == null){
            btn_forget.setEnabled(false);
        }else{
            btn_forget.setEnabled(true);
        }
        btn_forget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preferredNano != null) {
                    confirmationDialog(preferredNano);
                }
            }
        });

        //Update set button and field based on whether a preferred nano has been set or not
        if (preferredNano != null) {
            btn_set.setVisibility(View.INVISIBLE);
            tv_pref_nano.setText(SettingsManager.getStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null));
            tv_pref_nano.setVisibility(View.VISIBLE);
        } else {
            btn_set.setVisibility(View.VISIBLE);
            tv_pref_nano.setVisibility(View.INVISIBLE);
        }

    }

    /*
     * When the activity is destroyed, make a call to super class
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
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
     * In this case, there is are two items, the up indicator, and the settings button
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }

        else if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Function for displaying the dialog to confirm clearing the stored Nano
     * @param mac the mac address of the stored Nano
     */
    public void confirmationDialog(String mac) {

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setTitle(mContext.getResources().getString(R.string.nano_confirmation_title));
        alertDialogBuilder.setMessage(mContext.getResources().getString(R.string.nano_forget_msg, mac));

        alertDialogBuilder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                alertDialog.dismiss();
                SettingsManager.storeStringPref(mContext, SettingsManager.SharedPreferencesKeys.preferredDevice, null);
                btn_set.setVisibility(View.VISIBLE);
                tv_pref_nano.setVisibility(View.INVISIBLE);
                btn_forget.setEnabled(false);
            }
        });

        alertDialogBuilder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
}
