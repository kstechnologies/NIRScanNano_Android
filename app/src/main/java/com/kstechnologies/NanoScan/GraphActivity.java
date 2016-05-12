package com.kstechnologies.NanoScan;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.kstechnologies.nirscannanolibrary.KSTNanoSDK;
import com.kstechnologies.nirscannanolibrary.ScanListDictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

import com.kstechnologies.nirscannanolibrary.SettingsManager;

/**
 * Activity controlling the graphing of stored scan files.
 * This activity has to do special processing for the embedded raw files.
 * Currently, both the included raw files and the CSV files can be plotted
 *
 * @author collinmast
 */
public class GraphActivity extends Activity {

    private static Context mContext;

    private ListView graphListView;
    private ViewPager mViewPager;
    private String fileName;
    private ArrayList<String> mXValues;

    private ArrayList<Entry> mIntensityFloat;
    private ArrayList<Entry> mAbsorbanceFloat;
    private ArrayList<Entry> mReflectanceFloat;

    ArrayList<KSTNanoSDK.ScanListManager> graphDict = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        mContext = this;

        //Get the file name from the intent
        Intent intent = getIntent();
        fileName = intent.getStringExtra("file_name");

        //Set up action bar title, back button, and navigation tabs
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            if (fileName.contains(".csv")) {
                ab.setTitle(fileName);
            } else {
                ab.setTitle(fileName + ".csv");
            }
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

            mViewPager = (ViewPager) findViewById(R.id.viewpager);
            mViewPager.setOffscreenPageLimit(2);

            // Create a tab listener that is called when the user changes tabs.
            ActionBar.TabListener tl = new ActionBar.TabListener() {
                @Override
                public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
                    mViewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

                }

                @Override
                public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

                }
            };

            // Add 3 tabs, specifying the tab's text and TabListener
            for (int i = 0; i < 3; i++) {
                ab.addTab(
                        ab.newTab()
                                .setText(getResources().getStringArray(R.array.graph_tab_index)[i])
                                .setTabListener(tl));
            }
        }

        graphListView = (ListView) findViewById(R.id.lv_scan_data);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Initialize pager adapter
        CustomPagerAdapter pagerAdapter = new CustomPagerAdapter(this);
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.invalidate();

        //Set page change listener for pager to show proper tab when selected
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        // When swiping between pages, select the
                        // corresponding tab.
                        ActionBar ab = getActionBar();
                        if (ab != null) {
                            ab.setSelectedNavigationItem(position);
                        }
                    }
                });


        mXValues = new ArrayList<>();
        ArrayList<String> mIntensityString = new ArrayList<>();
        ArrayList<String> mAbsorbanceString = new ArrayList<>();
        ArrayList<String> mReflectanceString = new ArrayList<>();

        mIntensityFloat = new ArrayList<>();
        mAbsorbanceFloat = new ArrayList<>();
        mReflectanceFloat = new ArrayList<>();
        ArrayList<Float> mWavelengthFloat = new ArrayList<>();

        BufferedReader reader = null;
        BufferedReader dictReader = null;
        InputStream is = null;

        /*Try to open the file. First from the raw resources, then from the external directory
         * if that fails
         */
        try {
            is = getResources().openRawResource(getResources().getIdentifier(fileName, "raw", getPackageName()));
            reader = new BufferedReader(new InputStreamReader(is));
        } catch (Resources.NotFoundException e) {
            try {
                reader = new BufferedReader(new FileReader(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName));
                dictReader = new BufferedReader(new FileReader(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileName.replace(".csv", ".dict")));
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
                Toast.makeText(mContext, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        //Read lines in from the file
        try {
            String line;
            if (reader != null) {
                while ((line = reader.readLine()) != null) {
                    String[] RowData = line.split(",");
                    if (RowData[0].equals("(null)")) {
                        mXValues.add("0");
                    } else {
                        if (RowData[0].equals("Wavelength")) {
                            mXValues.add(RowData[0]);
                        } else {
                            mXValues.add(getSpatialFreq(RowData[0]));
                        }
                    }
                    if (RowData[1].equals("(null)")) {
                        mIntensityString.add("0");
                    } else {
                        mIntensityString.add(RowData[1]);
                    }
                    if (RowData[2].equals("(null)")) {
                        mAbsorbanceString.add("0");
                    } else {
                        mAbsorbanceString.add(RowData[2]);
                    }
                    if (RowData[3].equals("(null)")) {
                        mReflectanceString.add("0");
                    } else {
                        mReflectanceString.add(RowData[3]);
                    }
                }
            }
        } catch (IOException ex) {
            // handle exception
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // handle exception
            }
        }

        if (dictReader != null) {
            try {
                String line;
                while ((line = dictReader.readLine()) != null) {
                    String[] RowData = line.split(",");
                    graphDict.add(new KSTNanoSDK.ScanListManager(RowData[0], RowData[1]));
                }
            } catch (IOException ex) {
                // handle exception
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    // handle exception
                }
            }
        }

        //Remove the first items since these are column labels
        mXValues.remove(0);
        mIntensityString.remove(0);
        mAbsorbanceString.remove(0);
        mReflectanceString.remove(0);

        //Generate data points and calculate mins and maxes
        for (int i = 0; i < mXValues.size(); i++) {
            try {
            Float fIntensity = Float.parseFloat(mIntensityString.get(i));
            Float fAbsorbance = Float.parseFloat(mAbsorbanceString.get(i));
            Float fReflectance = Float.parseFloat(mReflectanceString.get(i));
            Float fWavelength = Float.parseFloat(mXValues.get(i));

            mIntensityFloat.add(new Entry(fIntensity, i));
            mAbsorbanceFloat.add(new Entry(fAbsorbance, i));
            mReflectanceFloat.add(new Entry(fReflectance, i));
            mWavelengthFloat.add(fWavelength);

            }catch (NumberFormatException e){
                Toast.makeText(GraphActivity.this, "Error parsing float value", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        float minWavelength = mWavelengthFloat.get(0);
        float maxWavelength = mWavelengthFloat.get(0);

        for (Float f : mWavelengthFloat) {
            if (f < minWavelength) minWavelength = f;
            if (f > maxWavelength) maxWavelength = f;
        }

        float minAbsorbance = mAbsorbanceFloat.get(0).getVal();
        float maxAbsorbance = mAbsorbanceFloat.get(0).getVal();

        for (Entry e : mAbsorbanceFloat) {
            if (e.getVal() < minAbsorbance) minAbsorbance = e.getVal();
            if (e.getVal() > maxAbsorbance) maxAbsorbance = e.getVal();
        }

        float minReflectance = mReflectanceFloat.get(0).getVal();
        float maxReflectance = mReflectanceFloat.get(0).getVal();

        for (Entry e : mReflectanceFloat) {
            if (e.getVal() < minReflectance) minReflectance = e.getVal();
            if (e.getVal() > maxReflectance) maxReflectance = e.getVal();
        }

        float minIntensity = mIntensityFloat.get(0).getVal();
        float maxIntensity = mIntensityFloat.get(0).getVal();

        for (Entry e : mIntensityFloat) {
            if (e.getVal() < minIntensity) minIntensity = e.getVal();
            if (e.getVal() > maxIntensity) maxIntensity = e.getVal();
        }

        ArrayList<KSTNanoSDK.ScanListManager> graphList = new ScanListDictionary(this).getScanList(fileName);
        ScanListAdapter mAdapter;
        if (graphList != null) {
            mAdapter = new ScanListAdapter(this, R.layout.row_graph_list_item, graphList);

            graphListView.setAdapter(mAdapter);
        } else if (graphDict != null) {
            mAdapter = new ScanListAdapter(this, R.layout.row_graph_list_item, graphDict);
            graphListView.setAdapter(mAdapter);
        }

    }

    /*
     * When the activity is destroyed, nothing is needed except a call to the super class
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /*
     * Inflate the options menu so that user actions are present
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_graph, menu);
        return true;
    }

    /*
     * Handle the selection of a menu item.
     * In this case, the user has the ability to email a file as well as navigate backwards.
     * When the email icon is clicked, the file is attached and an email template is created
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == android.R.id.home) {
            this.finish();
        }

        if (id == R.id.action_email) {
            if (findFile(fileName) != null) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emailing) + fileName);
                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(findFile(fileName)));
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.send_mail)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(GraphActivity.this, getString(R.string.no_email_clients), Toast.LENGTH_SHORT).show();
                }
            } else {

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                //i.putExtra(Intent.EXTRA_EMAIL, new String[]{"recipient@example.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.emailing) + fileName);
                //i.putExtra(Intent.EXTRA_TEXT, "body of email");
                InputStream inputStream = getResources().openRawResource(getResources().getIdentifier(fileName, "raw", getPackageName()));
                File file = new File(getExternalCacheDir(), "sample.csv");
                try {

                    OutputStream output = new FileOutputStream(file);
                    try {
                        try {
                            byte[] buffer = new byte[4 * 1024]; // or other buffer size
                            int read;

                            while ((read = inputStream.read(buffer)) != -1) {
                                output.write(buffer, 0, read);
                            }
                            output.flush();
                        } finally {
                            output.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // handle exception, define IOException and others
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                try {
                    startActivity(Intent.createChooser(i, getString(R.string.send_mail)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(GraphActivity.this, getString(R.string.no_email_clients), Toast.LENGTH_SHORT).show();
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }



    /**
     * Custom adapter to control the dictionary items in the listview
     */
    public class ScanListAdapter extends ArrayAdapter<KSTNanoSDK.ScanListManager> {
        private ViewHolder viewHolder;

        public ScanListAdapter(Context context, int textViewResourceId, ArrayList<KSTNanoSDK.ScanListManager> items) {
            super(context, textViewResourceId, items);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(this.getContext())
                        .inflate(R.layout.row_graph_list_item, parent, false);

                viewHolder = new ViewHolder();
                viewHolder.dataTitle = (TextView) convertView.findViewById(R.id.tv_list_head);
                viewHolder.dataBody = (TextView) convertView.findViewById(R.id.tv_list_data);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final KSTNanoSDK.ScanListManager item = getItem(position);
            if (item != null) {

                viewHolder.dataTitle.setText(item.getInfoTitle());
                viewHolder.dataBody.setText(item.getInfoBody());
            }
            return convertView;
        }

        private class ViewHolder {
            private TextView dataTitle;
            private TextView dataBody;
        }
    }

    /**
     * Pager enum to control tab tile and layout resource
     */
    public enum CustomPagerEnum {

        REFLECTANCE(R.string.reflectance, R.layout.page_graph_reflectance),
        ABSORBANCE(R.string.absorbance, R.layout.page_graph_absorbance),
        INTENSITY(R.string.intensity, R.layout.page_graph_intensity);

        private int mTitleResId;
        private int mLayoutResId;

        CustomPagerEnum(int titleResId, int layoutResId) {
            mTitleResId = titleResId;
            mLayoutResId = layoutResId;
        }

        public int getLayoutResId() {
            return mLayoutResId;
        }

    }

    /**
     * Custom pager adapter to handle changing chart data when pager tabs are changed
     */
    public class CustomPagerAdapter extends PagerAdapter {

        private Context mContext;

        public CustomPagerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public Object instantiateItem(ViewGroup collection, int position) {
            CustomPagerEnum customPagerEnum = CustomPagerEnum.values()[position];
            LayoutInflater inflater = LayoutInflater.from(mContext);
            ViewGroup layout = (ViewGroup) inflater.inflate(customPagerEnum.getLayoutResId(), collection, false);
            collection.addView(layout);

            if (customPagerEnum.getLayoutResId() == R.layout.page_graph_intensity) {
                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartInt);
                mChart.setDrawGridBackground(false);

                // no description text
                mChart.setDescription("");
                //mChart.setNoDataTextDescription("You need to provide data for the chart.");

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines


                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(true);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                setData(mChart, mXValues, mIntensityFloat, ChartType.INTENSITY);

                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_absorbance) {

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartAbs);
                mChart.setDrawGridBackground(false);

                // no description text
                mChart.setDescription("");
                //mChart.setNoDataTextDescription("You need to provide data for the chart.");

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                setData(mChart, mXValues, mAbsorbanceFloat, ChartType.ABSORBANCE);

                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                return layout;
            } else if (customPagerEnum.getLayoutResId() == R.layout.page_graph_reflectance) {

                LineChart mChart = (LineChart) layout.findViewById(R.id.lineChartRef);
                mChart.setDrawGridBackground(false);

                // no description text
                mChart.setDescription("");
                //mChart.setNoDataTextDescription("You need to provide data for the chart.");

                // enable touch gestures
                mChart.setTouchEnabled(true);

                // enable scaling and dragging
                mChart.setDragEnabled(true);
                mChart.setScaleEnabled(true);

                // if disabled, scaling can be done on x- and y-axis separately
                mChart.setPinchZoom(true);

                // x-axis limit line
                LimitLine llXAxis = new LimitLine(10f, "Index 10");
                llXAxis.setLineWidth(4f);
                llXAxis.enableDashedLine(10f, 10f, 0f);
                llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
                llXAxis.setTextSize(10f);

                XAxis xAxis = mChart.getXAxis();
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                YAxis leftAxis = mChart.getAxisLeft();
                leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines

                mChart.setAutoScaleMinMaxEnabled(true);

                leftAxis.setStartAtZero(false);
                leftAxis.enableGridDashedLine(10f, 10f, 0f);

                // limit lines are drawn behind data (and not on top)
                leftAxis.setDrawLimitLinesBehindData(true);

                mChart.getAxisRight().setEnabled(false);


                // add data
                setData(mChart, mXValues, mReflectanceFloat, ChartType.REFLECTANCE);

                mChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);

                // get the legend (only possible after setting data)
                Legend l = mChart.getLegend();

                // modify the legend ...
                l.setForm(Legend.LegendForm.LINE);
                return layout;
            } else {
                return layout;
            }
        }

        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }

        @Override
        public int getCount() {
            return CustomPagerEnum.values().length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.reflectance);
                case 1:
                    return getString(R.string.absorbance);
                case 2:
                    return getString(R.string.intensity);
            }
            return null;
        }
    }

    /**
     * Set the X-axis and Y-axis data for a specified chart
     * @param mChart the chart to update the data for
     * @param xValues the X -axis values to be plotted
     * @param yValues the Y-axis values to be plotted
     * @param type the type of chart to be displayed {@link com.kstechnologies.NanoScan.GraphActivity.ChartType}
     */
    private void setData(LineChart mChart, ArrayList<String> xValues, ArrayList<Entry> yValues, ChartType type) {

        if (type == ChartType.REFLECTANCE) {
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, fileName);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.RED);
            set1.setLineWidth(1f);
            set1.setCircleSize(3f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.RED);
            set1.setDrawFilled(true);

            ArrayList<LineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(xValues, dataSets);

            // set data
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.ABSORBANCE) {
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, fileName);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.GREEN);
            set1.setLineWidth(1f);
            set1.setCircleSize(3f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.GREEN);
            set1.setDrawFilled(true);

            ArrayList<LineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(xValues, dataSets);

            // set data
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else if (type == ChartType.INTENSITY) {
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, fileName);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLUE);
            set1.setLineWidth(1f);
            set1.setCircleSize(3f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLUE);
            set1.setDrawFilled(true);

            ArrayList<LineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(xValues, dataSets);

            // set data
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(20);
        } else {
            // create a dataset and give it a type
            LineDataSet set1 = new LineDataSet(yValues, fileName);

            // set the line to be drawn like this "- - - - - -"
            set1.enableDashedLine(10f, 5f, 0f);
            set1.enableDashedHighlightLine(10f, 5f, 0f);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleSize(3f);
            set1.setDrawCircleHole(true);
            set1.setValueTextSize(9f);
            set1.setFillAlpha(65);
            set1.setFillColor(Color.BLACK);
            set1.setDrawFilled(true);

            ArrayList<LineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData data = new LineData(xValues, dataSets);

            // set data
            mChart.setData(data);

            mChart.setMaxVisibleValueCount(10);
        }
    }

    /**
     * Enumeration of chart types
     */
    public enum ChartType {
        REFLECTANCE,
        ABSORBANCE,
        INTENSITY
    }

    /** Function to find a file in the external storage directory with the specified name
     *
     * @param name the name of the file to search for
     * @return File with the specified name
     */
    public File findFile(String name) {
        String nanoExtPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        File yourDir = new File(nanoExtPath, "/");
        for (File f : yourDir.listFiles()) {
            if (f.isFile()) {
                String fileName = f.getName();
                if (fileName.equals(name)) {
                    return f;
                }
            }
            // Do your stuff
        }
        return null;
    }

    /** Function return the specified frequency in units of frequency or wavenumber
     *
     * @param freq The frequency to convert
     * @return string representing either frequency or wavenumber
     */
    private String getSpatialFreq(String freq) {
        Float floatFreq = Float.parseFloat(freq);
        if (SettingsManager.getBooleanPref(mContext, SettingsManager.SharedPreferencesKeys.spatialFreq, SettingsManager.WAVELENGTH)) {
            return String.format("%.02f", floatFreq);
        } else {
            return String.format("%.02f", (10000000 / floatFreq));
        }
    }
}
