package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;


/**
 * This activity controls the retrieval of scans stored on the Nano SD card.
 * Once the scans are retrieved, then the user can delete them from the device.
 * These operations require the Nano to be connected, so receiving a disconnect message from
 * the {@link NanoBLEService} should finish the activity
 *
 * @author collinmast
 */

public class StoredScanDataActivity extends Activity {

    private static Context mContext;
    private ArrayList<StoredScan> storedScanList = new ArrayList<>();
    private StoredScanAdapter storedScanAdapter;
    private BroadcastReceiver storedScanReceiver = new StoredScanReceiver();
    private BroadcastReceiver storedScanSizeReceiver;
    private final IntentFilter storedScanFilter = new IntentFilter(KSTNanoSDK.STORED_SCAN_DATA);
    private final BroadcastReceiver disconnReceiver = new DisconnReceiver();
    private final IntentFilter disconnFilter = new IntentFilter(KSTNanoSDK.ACTION_GATT_DISCONNECTED);
    private SwipeMenuCreator unknownCreator = createMenu();
    ProgressDialog barProgressDialog;
    private ProgressBar scanIndexProgress;
    private int storedScanSize;
    private int receivedScanSize;
    private TextView tv_no_scans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stored_scan_data);
        scanIndexProgress = (ProgressBar) findViewById(R.id.scanIndexProgress);
        tv_no_scans = (TextView) findViewById(R.id.tv_no_scans);

        mContext = this;

        //Set up the action bar name and enable the back arrow
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(getString(R.string.stored_scan_data));
        }

        //Send broadcast to BLE service to start requesting stored scans
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(KSTNanoSDK.GET_STORED_SCANS));

        /* Initialize the stored scan list size receiver. This receiver will set up the progress
         * bar and the number of scans to expect
         */
        storedScanSizeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scanIndexProgress.setVisibility(View.INVISIBLE);
                storedScanSize = intent.getIntExtra(KSTNanoSDK.EXTRA_INDEX_SIZE, 0);
                if (storedScanSize > 0) {
                    tv_no_scans.setVisibility(View.GONE);
                    barProgressDialog = new ProgressDialog(StoredScanDataActivity.this);

                    barProgressDialog.setTitle(getString(R.string.reading_sd_card));
                    barProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    barProgressDialog.setProgress(0);
                    barProgressDialog.setMax(intent.getIntExtra(KSTNanoSDK.EXTRA_INDEX_SIZE, 0));
                    barProgressDialog.setCancelable(true);
                    barProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            finish();
                        }
                    });
                    barProgressDialog.show();
                    receivedScanSize = 0;
                } else {
                    receivedScanSize = 0;
                    tv_no_scans.setVisibility(View.VISIBLE);

                }
            }
        };

        //Register the broadcast receivers for disconnects, scan data size, and scan data
        LocalBroadcastManager.getInstance(mContext).registerReceiver(storedScanReceiver, storedScanFilter);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(storedScanSizeReceiver, new IntentFilter(KSTNanoSDK.SD_SCAN_SIZE));
        LocalBroadcastManager.getInstance(mContext).registerReceiver(disconnReceiver, disconnFilter);
    }

    /*
     * On resume, make a call to the superclass.
     * Nothing else is needed here besides calling
     * the super method.
     */
    @Override
    public void onResume() {
        super.onResume();
    }

    /*
     * When the activity is destroyed, unregister the BroadcastReceiver
     * handling disconnection events, as well as the receivers for the scan data size and data.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(storedScanReceiver);
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(storedScanSizeReceiver);
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
     * Custom receiver for stored scan data broadcasts.
     * This receiver will cancel the dialog box once it receives all of the scans
     */
    private class StoredScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String scanDate = intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_DATE);
            final SwipeMenuListView lv_stored_scans = (SwipeMenuListView) findViewById(R.id.lv_stored_scans);

            /* Keep track of how many stored scans have been received, and either update the progress,
             * or dismiss it depending on if the retrieval is complete
             */
            receivedScanSize++;
            if (receivedScanSize == storedScanSize) {
                barProgressDialog.dismiss();
                lv_stored_scans.setVisibility(View.VISIBLE);
            } else {
                barProgressDialog.setProgress(receivedScanSize);
            }

            //Add a new item to the stored scan list and refresh the adapter
            storedScanList.add(new StoredScan(intent.getStringExtra(KSTNanoSDK.EXTRA_SCAN_NAME), createTimeString(scanDate), intent.getByteArrayExtra(KSTNanoSDK.EXTRA_SCAN_INDEX)));
            storedScanAdapter = new StoredScanAdapter(mContext, storedScanList);

            //Set the adapter and swipe menu for the stored scan listview
            lv_stored_scans.setAdapter(storedScanAdapter);
            lv_stored_scans.setMenuCreator(unknownCreator);

            //Set up the item click listener for the stored scan listview
            lv_stored_scans.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {

                    switch (index) {
                        case 0:
                            Intent deleteScanIntent = new Intent(KSTNanoSDK.DELETE_SCAN);
                            deleteScanIntent.putExtra(KSTNanoSDK.EXTRA_SCAN_INDEX, storedScanAdapter.getItem(position).getScanIndex());
                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(deleteScanIntent);
                            storedScanAdapter.remove(storedScanList.get(position));
                            lv_stored_scans.setAdapter(storedScanAdapter);
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Custom adapter for the stored scan list. This adapter holds
     * {@link com.kstechnologies.NanoScan.StoredScanDataActivity.StoredScan} objects and initializes
     * the view holder
     */
    public class StoredScanAdapter extends ArrayAdapter<StoredScan> {
        private final ArrayList<StoredScan> storedScans;


        public StoredScanAdapter(Context context, ArrayList<StoredScan> values) {
            super(context, -1, values);
            this.storedScans = values;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.row_stored_scan_item, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.scanName = (TextView) convertView.findViewById(R.id.tv_scan_name);
                viewHolder.scanDate = (TextView) convertView.findViewById(R.id.tv_scan_date);
                viewHolder.scanIndex = (TextView) convertView.findViewById(R.id.tv_scan_index);
                viewHolder.scanIndex.setVisibility(View.GONE);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final StoredScan scan = getItem(position);
            if (scan != null) {
                viewHolder.scanName.setText(scan.getScanName());
                viewHolder.scanDate.setText(scan.getScanDate());
                StringBuilder stringBuilder = new StringBuilder();
                for (byte b : scan.getScanIndex()) {
                    stringBuilder.append(b);
                }
                viewHolder.scanIndex.setText(stringBuilder.toString());
            }
            return convertView;
        }
    }

    /**
     * View holder for the labels consisting of a
     * {@link com.kstechnologies.NanoScan.StoredScanDataActivity.StoredScan} object
     */
    private class ViewHolder {
        private TextView scanName;
        private TextView scanDate;
        private TextView scanIndex;
    }

    /**
     * Class for holding the stored scan objects.
     * Each object has a name, date, and index
     */
    private class StoredScan {
        String scanName;
        String scanDate;
        byte[] scanIndex;

        public StoredScan(String scanName, String scanDate, byte[] scanIndex) {
            this.scanName = scanName;
            this.scanDate = scanDate;
            this.scanIndex = scanIndex;
        }

        public String getScanName() {
            return scanName;
        }

        public String getScanDate() {
            return scanDate;
        }

        public byte[] getScanIndex() {
            return scanIndex;
        }
    }

    /**
     * Function creating a formatted date from a string of bytes retrieved from the Nano.
     *
     * @param scanDate a string consisting of the 7 date bytes retrieved from the nano
     * @return The new formatted date string
     */
    private String createTimeString(String scanDate) {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddFFHHmmss", Locale.US);
        try {
            Date date = format.parse(scanDate);

            String outFormat = "EEEE, M/d/yy " + getString(R.string.time_format_at) + " HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(outFormat, Locale.US);
            return sdf.format(date);

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Create the swipe menu for deleting stored scans. This menu should have one option, delete
     * @return new instance of a SwipeMenu
     */
    private SwipeMenuCreator createMenu() {
        return new SwipeMenuCreator() {
            @Override
            public void create(SwipeMenu menu) {

                SwipeMenuItem settingsItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                settingsItem.setBackground(R.color.kst_red);
                // set item width
                settingsItem.setWidth(dp2px(90));
                // set a icon
                //settingsItem.setIcon(android.R.drawable.ic_menu_delete);
                settingsItem.setTitleColor(ContextCompat.getColor(mContext, R.color.white));
                settingsItem.setTitleSize(18);
                settingsItem.setTitle(getResources().getString(R.string.delete));

                // add to menu
                menu.addMenuItem(settingsItem);
            }
        };
    }

    /**
     * Function to convert dip to pixels
     *
     * @param dp the number of dip to convert
     * @return the dip units converted to pixels
     */
    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    /**
     * Broadcast Receiver handling the disconnect event. If the Nano disconnects,
     * this activity should finish so that the user is taken back to the {@link ScanListActivity}.
     * A toast message should appear so that the user knows why the activity is finishing.
     */
    public class DisconnReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(mContext, R.string.nano_disconnected, Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
