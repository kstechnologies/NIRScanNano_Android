package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * This activity controls the main launcher view
 * This activity is responsible for generating the splash screen and the main
 * file list view
 *
 * From this activity, the user can begin the scan process {@link NewScanActivity},
 * Go to the info view {@link InfoActivity}, or view old scan data {@link GraphActivity}
 *
 * @author collinmast
 */
public class ScanListActivity extends Activity {

    private ArrayList<String> csvFiles = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private static Context mContext;
    private SwipeMenuListView lv_csv_files;
    private SwipeMenuCreator unknownCreator = createMenu();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        // Setup ActionBar for Splash Animation by hiding it
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.hide();
        }

        setContentView(R.layout.activity_scan_list);

        //Get UI elements references
        final RelativeLayout mSplashLayout = (RelativeLayout) findViewById(R.id.rl_splash);
        final RelativeLayout mMainLayout = (RelativeLayout) findViewById(R.id.rl_mainLayout);

        //Set up the splash screen
        mSplashLayout.setVisibility(View.VISIBLE);
        mMainLayout.setVisibility(View.GONE);

        // Setup the Splash Animation
        Animation animSplash = AnimationUtils.loadAnimation(this, R.anim.alpha_splash);
        animSplash.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //Hide splash layout and display the main layout
                mSplashLayout.setVisibility(View.GONE);
                mMainLayout.setVisibility(View.VISIBLE);

                //Now that the splash animation is over, show the action bar
                getActionBar().show();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        // Animate the Splash
        mSplashLayout.setAnimation(animSplash);
        animSplash.start();
    }

    /*
     * When the activity is destroyed, make a call to the super class
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /* On resume, check for crashes and updates with HockeyApp,
     * and set up the file list,swipe menu, and event listeners
     */
    @Override
    public void onResume() {
        super.onResume();
        //checkForCrashes();
        //checkForUpdates();

        csvFiles.clear();

        lv_csv_files = (SwipeMenuListView) findViewById(R.id.lv_csv_files);
        populateListView();

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, csvFiles);
        lv_csv_files.setAdapter(mAdapter);
        lv_csv_files.setMenuCreator(unknownCreator);

        /* set the on menu item click for the SwipeMenuListView.
         * In this case, delete the selected file
         */
        lv_csv_files.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final int position, SwipeMenu menu, int index) {

                switch (index) {
                    case 0:
                        removeFile(mAdapter.getItem(position));
                        mAdapter.remove(csvFiles.get(position));
                        lv_csv_files.setAdapter(mAdapter);
                        break;
                }
                return false;
            }
        });

        /* Add item click listener to file listview. This will close an item if it's
         * swipe menu is open
         */
        lv_csv_files.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                lv_csv_files.smoothOpenMenu(position);
            }
        });

        mAdapter.notifyDataSetChanged();

        /* Add item click listener to file listview. Clicking an item will start the graph
         * activity for that file
         */
        lv_csv_files.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent graphIntent = new Intent(mContext, GraphActivity.class);
                graphIntent.putExtra("file_name", mAdapter.getItem(i));
                startActivity(graphIntent);
            }
        });

        //Get UI reference to Edit text bar for searching through scan names
        EditText searchText = (EditText) findViewById(R.id.et_search);

        //Add listener to editText so that the listview is updated as the user starts typing
        searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /*
     * Inflate the options menu so that the info, settings, and connect icons are visible
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scan_list, menu);
        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, there are three options. The user can go to the info activity,
     * the settings activity, or connect to a Nano
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        else if (id == R.id.action_info) {
            Intent infoIntent = new Intent(this, InfoActivity.class);
            startActivity(infoIntent);
            return true;
        }

        else if (id == R.id.action_scan) {
            Intent graphIntent = new Intent(mContext, NewScanActivity.class);
            graphIntent.putExtra("file_name", getString(R.string.newScan));
            startActivity(graphIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Populate the stored scan listview with included files in the raw directory as well as
     * stored CSV files
     */
    public void populateListView() {
        Field[] files = R.raw.class.getFields();

        // loop for every file in raw folder
        for (Field file : files) {
            String filename = file.getName();

            csvFiles.add(filename);
        }

        String nanoExtPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        File yourDir = new File(nanoExtPath, "/");
        for (File f : yourDir.listFiles()) {
            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.contains(".csv")) {
                    //Log.d(TAG, "found:" + fileName);
                    csvFiles.add(fileName);
                }
            }
        }
    }

    /**
     * Removes a specified file from the external storage directory
     * @param name name of the file to delete
     */
    public void removeFile(String name) {
        String nanoExtPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        File yourDir = new File(nanoExtPath, "/");
        for (File f : yourDir.listFiles()) {
            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.equals(name)) {
                    f.delete();
                }
            }
        }
    }

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
}
