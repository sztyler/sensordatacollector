package de.unima.ar.collector.database;

import android.app.Activity;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.sensors.SensorCollectorManager;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableMapper;
import de.unima.ar.collector.util.CSVWriter;
import de.unima.ar.collector.util.UIUtils;


/**
 * @author Timo Sztyler
 */
public class DatabaseExportCSV extends AsyncTask<String, Void, Boolean> implements MediaScannerConnection.OnScanCompletedListener
{
    private Context           context;
    private ArrayList<String> tables;
    private Set<String>       targetFiles;


    public DatabaseExportCSV(Context context, ArrayList<String> tables)
    {
        this.context = context;
        this.tables = tables;
        this.targetFiles = new HashSet<>();
    }


    @Override
    protected void onPreExecute()
    {
        showProgressBar();
    }


    @Override
    protected Boolean doInBackground(String... arg0)
    {
        // Make dirs on sd card
        File extStore = Environment.getExternalStorageDirectory();

        // sdcard does not exist
        if(extStore == null) {
            UIUtils.makeToast((Activity) context, R.string.option_export_nosdcardfound, Toast.LENGTH_LONG);
            return false;
        }

        // check if storage is available and writable
        String storageState = Environment.getExternalStorageState();
        if(!Environment.MEDIA_MOUNTED.equals(storageState)) {
            extStore = Environment.getDataDirectory();

            if(extStore == null || !extStore.canRead() || !extStore.canWrite()) {
                UIUtils.makeToast((Activity) context, R.string.option_export_nowritablemedia, Toast.LENGTH_LONG);
                return false;
            }
        }

        // check if a table was selected where the sensor is still running
        SensorCollectorManager scm = SensorDataCollectorService.getInstance().getSCM();
        if(scm != null) {
            Set<Integer> enabledSensors = scm.getEnabledCollectors();
            for(String table : tables) {
                if(!table.contains("Sensor")) {
                    continue;
                }

                if(enabledSensors.contains(SQLTableMapper.getType(table.substring(table.indexOf("Sensor"))))) {
                    UIUtils.makeToast((Activity) context, R.string.option_export_export_failed2, Toast.LENGTH_SHORT);
                    return false;
                }
            }
        }

        // unseren eigenen Ordner auf der SD Karte erstellen falls nicht vorhanden
        long timestamp = System.currentTimeMillis() / 1000;
        File root = new File(extStore.getAbsolutePath(), "SensorDataCollector");
        File targetFolder = new File(root, "export" + timestamp);
        if(!targetFolder.mkdirs() && !targetFolder.exists()) {
            return false;
        }

        for(String table : tables) {
            try {
                String query = "SELECT id FROM " + table + " ORDER BY id DESC LIMIT 1";
                List<String[]> numberOfRows = SQLDBController.getInstance().query(query, null, false);

                if(numberOfRows == null || numberOfRows.size() == 0 || numberOfRows.get(0).length == 0) {
                    continue;
                }

                int size = Integer.valueOf(numberOfRows.get(0)[0]);
                for(int i = 0; i < size; i += Settings.EXPORT_ATONCE) {     // This is necessary because the phone runs out of memory if a table is (really) big
                    String query2 = "SELECT * FROM " + table + " LIMIT " + Settings.EXPORT_ATONCE + " OFFSET " + i;
                    List<String[]> result = SQLDBController.getInstance().query(query2, null, true);

                    File target = new File(targetFolder, table + ".csv");
                    writeDataToDisk(target, result);
                    this.targetFiles.add(target.getAbsolutePath());
                }

                // advanced -combine tables - experimtental TODO
                //                if(table.startsWith(SQLTableName.PREFIX + "f3497abd5b090711SensorAccelerometer") && table.endsWith("Data")) {
                //                    List<String[]> result2 = SQLDBController.getInstance().query("SELECT ss.*, p2.name AS `label_environment`, p.name AS `label_posture`, dp.name AS `label_deviceposition`, a.name AS `label_activity`, ac.log AS `label_invalid` FROM " + table + " AS ss LEFT JOIN " + SQLTableName.POSTUREDATA + " pd on ((pd.starttime < ss.attr_time AND pd.endtime > ss.attr_time) OR (pd.starttime < ss.attr_time AND pd.endtime == 0)) LEFT JOIN " + SQLTableName.POSTURES + " AS p on (pd.pid == p.id) LEFT JOIN " + SQLTableName.POSITIONDATA + " AS pd2 on ((pd2.starttime < ss.attr_time AND pd2.endtime > ss.attr_time) OR (pd2.starttime < ss.attr_time AND pd2.endtime == 0)) LEFT JOIN " + SQLTableName.POSITIONS + " AS p2 on (pd2.pid == p2.id) LEFT JOIN " + SQLTableName.DEVICEPOSITIONDATA + " AS dpd ON ((dpd.starttime < ss.attr_time AND dpd.endtime > ss.attr_time) OR (dpd.starttime < ss.attr_time AND dpd.endtime == 0)) LEFT JOIN " + SQLTableName.DEVICEPOSITION + " AS dp ON (dpd.pid == dp.id) LEFT JOIN " + SQLTableName.ACTIVITYDATA + " AS ad on ((ad.starttime < ss.attr_time AND ad.endtime > ss.attr_time) OR (ad.starttime < ss.attr_time AND ad.endtime == 0)) LEFT JOIN " + SQLTableName.ACTIVITIES + " AS a on (ad.activityid == a.id) LEFT JOIN " + SQLTableName.ACTIVITYCORRECTION + " AS ac ON (ac.starttime < ss.attr_time AND ac.endtime > ss.attr_time);", null, true);
                //                    writeDataToDisk(dst.getAbsolutePath() + "/" + table + "Advanced.csv", result2);
                //                }
            } catch(IOException e1) {
                UIUtils.makeToast((Activity) context, R.string.option_export_couldnotcreatefile, Toast.LENGTH_LONG);
                return false;
            }
        }

        // make new files discoverable
        MediaScannerConnection.scanFile(context, this.targetFiles.toArray(new String[this.targetFiles.size()]), null, this);

        return true;
    }


    @Override
    protected void onPostExecute(final Boolean success)
    {
        if(!success) {  // if success see method onScanCompleted
            UIUtils.makeToast((Activity) context, R.string.option_export_failed, Toast.LENGTH_SHORT);
            hideProgressBar();
        }
    }


    @Override
    public void onScanCompleted(String path, Uri uri)   // this method is only called if the export was successfull
    {
        this.targetFiles.remove(path);

        if(this.targetFiles.size() == 0) {
            hideProgressBar();
            UIUtils.makeToast((Activity) context, R.string.option_export_success, Toast.LENGTH_SHORT);
        }
    }


    private boolean writeDataToDisk(File targetFile, List<String[]> result) throws IOException
    {
        // noting
        if(result.size() == 0) {
            return false;
        }

        // create file
        boolean success = targetFile.createNewFile();
        CSVWriter csvWrite = new CSVWriter(new FileWriter(targetFile, true));

        // write headline
        String[] columnNames = result.get(0);
        if(success) {
            csvWrite.writeNext(columnNames);
        }

        // write data
        for(int i = 1; i < result.size(); i++) {    // move from pos=-1 to pos=0
            String arrStr[] = new String[columnNames.length];
            String[] row = result.get(i);

            for(int j = 0; j < columnNames.length; j++) {
                String value = row[j];
                if(value == null && targetFile.getAbsolutePath().toLowerCase(Locale.ENGLISH).endsWith("advanced.csv")) {
                    value = "?";
                }
                arrStr[j] = value;

                // set the correct endtime for already running activities
                if(columnNames[j].toLowerCase(Locale.ENGLISH).equals("endtime") && Double.valueOf(arrStr[j]) == 0) {
                    arrStr[j] = String.valueOf(System.currentTimeMillis());
                }
            }

            csvWrite.writeNext(arrStr);
        }

        // close streams
        csvWrite.close();

        return true;
    }


    private void showProgressBar()
    {
        ((Activity) context).runOnUiThread(new Runnable()
        {
            public void run()
            {
                ((Activity) context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                FrameLayout progressBarHolder = (FrameLayout) ((Activity) context).findViewById(R.id.progressBarHolder);
                progressBarHolder.setVisibility(View.VISIBLE);
            }
        });
    }


    private void hideProgressBar()
    {
        ((Activity) context).runOnUiThread(new Runnable()
        {
            public void run()
            {
                ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                FrameLayout progressBarHolder = (FrameLayout) ((Activity) context).findViewById(R.id.progressBarHolder);
                progressBarHolder.setVisibility(View.GONE);
            }
        });
    }
}