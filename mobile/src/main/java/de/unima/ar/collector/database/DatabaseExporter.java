package de.unima.ar.collector.database;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.sensors.SensorCollectorManager;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableMapper;
import de.unima.ar.collector.util.CSVWriter;
import de.unima.ar.collector.util.UIUtils;


/**
 * @author Fabian Kramm
 */
public class DatabaseExporter extends AsyncTask<String, Void, Boolean>
{
    private Context           context;
    private ArrayList<String> tables;


    public DatabaseExporter(Context context, ArrayList<String> tables)
    {
        this.context = context;
        this.tables = tables;
    }


    @Override
    protected void onPreExecute()
    {
        MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");

        final ListView activityMain = (ListView) main.findViewById(R.id.mainlist);
        activityMain.setEnabled(false);

        final FrameLayout progressBarHolder = (FrameLayout) main.findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.VISIBLE);
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

        // Unseren eigenen Ordner auf der SD Karte erstellen falls nicht vorhanden
        long timestamp = System.currentTimeMillis() / 1000;
        File dst = new File(extStore.getAbsolutePath() + "/SensorDataCollector/export" + timestamp);
        if(!dst.mkdirs() && !dst.exists()) {
            return false;
        }

        // basic
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
                    writeDataToDisk(dst.getAbsolutePath() + "/" + table + ".csv", result);
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

        return true;
    }


    protected void onPostExecute(final Boolean success)
    {
        if(success) {
            UIUtils.makeToast((Activity) context, R.string.option_export_success, Toast.LENGTH_SHORT);
        } else {
            UIUtils.makeToast((Activity) context, R.string.option_export_failed, Toast.LENGTH_SHORT);
        }

        MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");

        final ListView activityMain = (ListView) main.findViewById(R.id.mainlist);
        activityMain.setEnabled(true);

        final FrameLayout progressBarHolder = (FrameLayout) main.findViewById(R.id.progressBarHolder);
        progressBarHolder.setVisibility(View.GONE);
    }


    private boolean writeDataToDisk(String path, List<String[]> result) throws IOException
    {
        // noting
        if(result.size() == 0) {
            return false;
        }

        // create file
        File exportFile = new File(path);
        boolean success = exportFile.createNewFile();
        CSVWriter csvWrite = new CSVWriter(new FileWriter(exportFile, true));

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
                if(value == null && path.toLowerCase(Locale.ENGLISH).endsWith("advanced.csv")) {
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
}