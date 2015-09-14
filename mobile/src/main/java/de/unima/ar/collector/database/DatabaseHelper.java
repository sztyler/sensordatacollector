package de.unima.ar.collector.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.sensors.AccelerometerSensorCollector;
import de.unima.ar.collector.sensors.AmbientTemperatureSensorCollector;
import de.unima.ar.collector.sensors.GPSCollector;
import de.unima.ar.collector.sensors.GravitySensorCollector;
import de.unima.ar.collector.sensors.GyroscopeSensorCollector;
import de.unima.ar.collector.sensors.LightSensorCollector;
import de.unima.ar.collector.sensors.LinearAccelerationSensorCollector;
import de.unima.ar.collector.sensors.MagneticFieldSensorCollector;
import de.unima.ar.collector.sensors.MicrophoneCollector;
import de.unima.ar.collector.sensors.OrientationSensorCollector;
import de.unima.ar.collector.sensors.PressureSensorCollector;
import de.unima.ar.collector.sensors.ProximitySensorCollector;
import de.unima.ar.collector.sensors.RelativeHumiditySensorCollector;
import de.unima.ar.collector.sensors.RotationVectorSensorCollector;
import de.unima.ar.collector.sensors.StepDetectorSensorCollector;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.util.UIUtils;


/**
 * Regelt die Datenbankerstellung und hält Methoden zur Datenbankabfrage bereit
 *
 * @author Fabian Kramm, Timo Sztyler
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
    private Context context;

    private static final String sqlTableOptions            = "CREATE TABLE IF NOT EXISTS " + SQLTableName.OPTIONS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, option INT UNIQUE, value INT)";
    private static final String sqlTableSensorOptions      = "CREATE TABLE IF NOT EXISTS " + SQLTableName.SENSOROPTIONS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, sensor INT UNIQUE, freq DOUBLE, enabled BOOLEAN)";
    private static final String sqlTableActivities         = "CREATE TABLE IF NOT EXISTS " + SQLTableName.ACTIVITIES + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)";
    private static final String sqlTableSubActivities      = "CREATE TABLE IF NOT EXISTS " + SQLTableName.SUBACTIVITIES + " (id INTEGER PRIMARY KEY AUTOINCREMENT, activityid INT NOT NULL, name TEXT, FOREIGN KEY(activityid) REFERENCES " + SQLTableName.ACTIVITIES + " (id))";
    private static final String sqlTablePostures           = "CREATE TABLE IF NOT EXISTS " + SQLTableName.POSTURES + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)";
    private static final String sqlTablePositions          = "CREATE TABLE IF NOT EXISTS " + SQLTableName.POSITIONS + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)";
    private static final String sqlTableDevicePosition     = "CREATE TABLE IF NOT EXISTS " + SQLTableName.DEVICEPOSITION + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE)";
    private static final String sqlTableActivityData       = "CREATE TABLE IF NOT EXISTS " + SQLTableName.ACTIVITYDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, activityid INT NOT NULL, subactivityid INT, starttime INT, endtime INT)";
    private static final String sqlTablePostureData        = "CREATE TABLE IF NOT EXISTS " + SQLTableName.POSTUREDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, pid INT NOT NULL, starttime INT, endtime INT)";
    private static final String sqlTablePositionData       = "CREATE TABLE IF NOT EXISTS " + SQLTableName.POSITIONDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, pid INT NOT NULL, starttime INT, endtime INT)";
    private static final String sqlTableDevicePositionData = "CREATE TABLE IF NOT EXISTS " + SQLTableName.DEVICEPOSITIONDATA + " (id INTEGER PRIMARY KEY AUTOINCREMENT, pid INT NOT NULL, starttime INT, endtime INT)";
    private static final String sqlTableDevices            = "CREATE TABLE IF NOT EXISTS " + SQLTableName.DEVICES + " (id INTEGER PRIMARY KEY AUTOINCREMENT, device INT UNIQUE NOT NULL, lastseen INT NOT NULL)";
    private static final String sqlTableActivityCorrection = "CREATE TABLE IF NOT EXISTS " + SQLTableName.ACTIVITYCORRECTION + "(id INTEGER PRIMARY KEY AUTOINCREMENT, starttime INT, endtime INT, log VARCHAR(255))";

    private static final String DATABASE_NAME    = "sensordata.db";
    private static final int    DATABASE_VERSION = 3;


    public DatabaseHelper(Context cxt)
    {
        super(cxt, DATABASE_NAME, null, DATABASE_VERSION);

        this.context = cxt;
    }


    @Override
    public void onCreate(SQLiteDatabase db)
    {
        // nothing
    }


    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
        // nothing
    }


    public void deleteDatabase()
    {
        this.context.deleteDatabase(DATABASE_NAME);
    }


    public static void createTables()
    {
        // create option tables - device independent
        SQLDBController.getInstance().execSQL(sqlTableOptions);
        SQLDBController.getInstance().execSQL(sqlTableSensorOptions);
        SQLDBController.getInstance().execSQL(sqlTableDevices);

        // create activity tables - device independent
        SQLDBController.getInstance().execSQL(sqlTableActivities);
        SQLDBController.getInstance().execSQL(sqlTableSubActivities);
        SQLDBController.getInstance().execSQL(sqlTablePostures);
        SQLDBController.getInstance().execSQL(sqlTablePositions);
        SQLDBController.getInstance().execSQL(sqlTableDevicePosition);
        SQLDBController.getInstance().execSQL(sqlTableActivityData);
        SQLDBController.getInstance().execSQL(sqlTablePostureData);
        SQLDBController.getInstance().execSQL(sqlTablePositionData);
        SQLDBController.getInstance().execSQL(sqlTableDevicePositionData);
        SQLDBController.getInstance().execSQL(sqlTableActivityCorrection);

        // insert default values
        insertDefaultData();

        // modify tables which already exists and where modified in a new version
        DatabaseHelper.modifyExistingTables();
    }


    public static void createDeviceDependentTables(String deviceID)
    {
        AccelerometerSensorCollector.createDBStorage(deviceID);
        AmbientTemperatureSensorCollector.createDBStorage(deviceID);
        GPSCollector.createDBStorage(deviceID);
        GravitySensorCollector.createDBStorage(deviceID);
        GyroscopeSensorCollector.createDBStorage(deviceID);
        LightSensorCollector.createDBStorage(deviceID);
        LinearAccelerationSensorCollector.createDBStorage(deviceID);
        MagneticFieldSensorCollector.createDBStorage(deviceID);
        MicrophoneCollector.createDBStorage(deviceID);
        OrientationSensorCollector.createDBStorage(deviceID);
        PressureSensorCollector.createDBStorage(deviceID);
        ProximitySensorCollector.createDBStorage(deviceID);
        RelativeHumiditySensorCollector.createDBStorage(deviceID);
        RotationVectorSensorCollector.createDBStorage(deviceID);
        StepDetectorSensorCollector.createDBStorage(deviceID);
    }


    public static ArrayList<String> getStringResultSet(String statement, String[] statementValues)
    {
        ArrayList<String> returnSet = new ArrayList<>();

        List<String[]> result = SQLDBController.getInstance().query(statement, statementValues, false);

        for(String[] row : result) {
            returnSet.add(row[0]);
        }

        return returnSet;
    }


    public static void streamData(String tableName, String[] columns, String start, String end, Map<String, List<Double>> values)
    {
        Map<String, Double[]> tmp = new HashMap<>();

        String names = Arrays.toString(columns).replace("[", "").replace("]", "").trim();
        List<String[]> result = SQLDBController.getInstance().query("SELECT attr_time, " + names + " FROM " + tableName + " WHERE attr_time > ? AND attr_time < ?", new String[]{ String.valueOf(start), String.valueOf(end) }, false);

        MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
        main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading3, 15);

        tmp.put("attr_time", new Double[result.size()]);
        for(String column : columns) {
            tmp.put(column, new Double[result.size()]);
        }


        for(int i = 0; i < result.size(); i++) {
            String[] row = result.get(i);
            tmp.get("attr_time")[i] = (Double.valueOf(row[0]) - 1388534400000d);

            for(int j = 0; j < columns.length; j++) {
                tmp.get(columns[j])[i] = Double.valueOf(row[j + 1]);
            }
        }

        for(String key : tmp.keySet()) {
            Double[] entry = tmp.get(key);
            List<Double> list = Arrays.asList(entry);
            values.put(key, list);
        }
    }


    //    public static void streamData(String tableName, String beginOfSelect, String start, String end, ArrayList<Double> timeData, ArrayList<Double>[] valueData, long amount)
    //    {
    //        long count = Long.parseLong(DatabaseHelper.getStringResultSet("SELECT COUNT(*) FROM " + tableName + " WHERE attr_time > ? AND attr_time < ?", new String[]{ start, end }).get(0));
    //
    //        // We only need one select statement
    //        if(count <= amount) {
    //            List<String[]> result = SQLDBController.getInstance().query(beginOfSelect + " WHERE attr_time > ? AND attr_time < ?", new String[]{ String.valueOf(start), String.valueOf(end) }, false);
    //
    //            for(String[] row : result) {
    //                timeData.add(Double.valueOf(row[0]) - 1388534400000d);
    //
    //                for(int i = 0; i < valueData.length; i++) {
    //                    valueData[i].add(Double.valueOf(row[i + 1]));
    //                }
    //            }
    //
    //            return;
    //        }
    //        // calc steps
    //        long r = count % amount;
    //        long step = (count - r) / amount;
    //        double step2 = r / amount;
    //
    //        long limit = 0;
    //        long counter = 1;
    //        double counter2 = 0;
    //
    //        while(limit < count) {
    //            List<String[]> result = SQLDBController.getInstance().query(beginOfSelect + " WHERE attr_time > ? AND attr_time < ? LIMIT '" + limit + "', 150", new String[]{ String.valueOf(start), String.valueOf(end) }, false);
    //
    //            for(String[] row : result) {
    //                if(counter == 1) {
    //                    timeData.add(Double.valueOf(row[0]) - 1388534400000d);
    //
    //                    for(int i = 0; i < valueData.length; i++) {
    //                        valueData[i].add(Double.valueOf(row[i + 1]));
    //                    }
    //                } else if(counter == count) {
    //                    timeData.add(Double.valueOf(row[0]) - 1388534400000d);
    //
    //                    for(int i = 0; i < valueData.length; i++) {
    //                        valueData[i].add(Double.valueOf(row[i + 1]));
    //                    }
    //                } else if(counter % step == 0) {
    //                    timeData.add(Double.valueOf(row[0]) - 1388534400000d);
    //
    //                    for(int i = 0; i < valueData.length; i++) {
    //                        valueData[i].add(Double.valueOf(row[i + 1]));
    //                    }
    //                } else if(counter2 > 1) {
    //                    timeData.add(Double.valueOf(row[0]) - 1388534400000d);
    //
    //                    for(int i = 0; i < valueData.length; i++) {
    //                        valueData[i].add(Double.valueOf(row[i + 1]));
    //                    }
    //
    //                    counter2 = counter2 - 1;
    //                }
    //
    //                counter2 = counter2 + step2;
    //                counter++;
    //            }
    //
    //            limit = limit + 150;
    //        }
    //    }


    private static void insertDefaultData()
    {


        // Postures
        //        String[] postures = UIUtils.getString(R.string.activity_posture_idling, R.string.activity_posture_walking, R.string.activity_posture_cycling, R.string.activity_posture_driving, R.string.activity_posture_running, R.string.activity_posture_sitting, R.string.activity_posture_standing, R.string.activity_posture_stairsup, R.string.activity_posture_stairsdown);
        String[] postures = UIUtils.getString(R.string.activity_posture_none, R.string.activity_posture_walking, R.string.activity_posture_running, R.string.activity_posture_sitting, R.string.activity_posture_standing, R.string.activity_posture_recumbency, R.string.activity_posture_climbingup, R.string.activity_posture_climbingdown, R.string.activity_posture_jumping);
        List<String> existPostures = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.POSTURES, null);
        for(String posture : postures) {
            if(existPostures.contains(posture)) {
                continue;
            }

            ContentValues newValues = new ContentValues();
            newValues.put("name", posture);
            SQLDBController.getInstance().insert(SQLTableName.POSTURES, null, newValues);
        }

        // Enviroment
        //        String[] positions = UIUtils.getString(R.string.activity_position_none, R.string.activity_position_bedroom, R.string.activity_position_livingroom, R.string.activity_position_bathroom, R.string.activity_position_stairs, R.string.activity_position_outside);
        String[] positions = UIUtils.getString(R.string.activity_environment_none, R.string.activity_environment_home, R.string.activity_environment_office, R.string.activity_environment_transporation, R.string.activity_environment_building, R.string.activity_environment_street);
        List<String> existPositions = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.POSITIONS, null);
        for(String position : positions) {
            if(existPositions.contains(position)) {
                continue;
            }

            ContentValues newValues = new ContentValues();
            newValues.put("name", position);
            SQLDBController.getInstance().insert(SQLTableName.POSITIONS, null, newValues);
        }


        // Device Position
        String[] devicePositions = UIUtils.getString(R.string.activity_devicepositon_none, R.string.activity_devicepositon_head, R.string.activity_devicepositon_upperarm, R.string.activity_devicepositon_forearm, R.string.activity_devicepositon_thigh, R.string.activity_devicepositon_shin, R.string.activity_devicepositon_waist, R.string.activity_devicepositon_hand, R.string.activity_devicepositon_chest);
        List<String> existDevicePositions = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.DEVICEPOSITION, null);
        for(String devicePosition : devicePositions) {
            if(existDevicePositions.contains(devicePosition)) {
                continue;
            }

            ContentValues newValues = new ContentValues();
            newValues.put("name", devicePosition);
            SQLDBController.getInstance().insert(SQLTableName.DEVICEPOSITION, null, newValues);
        }

        // Activities
        String[] activities = UIUtils.getString(R.string.activity_activities_sleeping, R.string.activity_activities_eating, R.string.activity_activities_personal, R.string.activity_activities_transportation, R.string.activity_activities_relaxing, R.string.activity_activities_functional, R.string.activity_activities_medication, R.string.activity_activities_shopping, R.string.activity_activities_housework, R.string.activity_activities_preparation, R.string.activity_activities_socializing, R.string.activity_activities_movement, R.string.activity_activities_deskwork, R.string.activity_activities_sport);
        List<String> existActivities = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.ACTIVITIES, null);
        for(String activity : activities) {
            if(existActivities.contains(activity)) {
                continue;
            }

            ContentValues newValues = new ContentValues();
            newValues.put("name", activity);
            SQLDBController.getInstance().insert(SQLTableName.ACTIVITIES, null, newValues);
        }

        // SubActivities
        //        String[] subactivities = UIUtils.getString(R.string.activity_subactivities_salad, R.string.activity_subactivities_kebab, R.string.activity_subactivities_pizza, R.string.activity_subactivities_cleaning, R.string.activity_subactivities_vacuum, R.string.activity_subactivities_cutting, R.string.activity_subactivities_walk, R.string.activity_subactivities_sport);
        //        int[] activityid = new int[]{ 2, 2, 2, 9, 9, 10, 12, 12 };
        String[] subactivities = UIUtils.getString(R.string.activity_subactivities_brunch, R.string.activity_subactivities_breakfast, R.string.activity_subactivities_lunch, R.string.activity_subactivities_dinner, R.string.activity_subactivities_snack, R.string.activity_subactivities_coffeebreak, R.string.activity_subactivities_general_other, R.string.activity_subactivities_tram, R.string.activity_subactivities_bus, R.string.activity_subactivities_car, R.string.activity_subactivities_train, R.string.activity_subactivities_motorcycle, R.string.activity_subactivities_skateboard, R.string.activity_subactivities_bicycle, R.string.activity_subactivities_scooter, R.string.activity_subactivities_general_other, R.string.activity_subactivities_gaming, R.string.activity_subactivities_tv, R.string.activity_subactivities_music, R.string.activity_subactivities_general_other, R.string.activity_subactivities_wheelchair, R.string.activity_subactivities_rollator, R.string.activity_subactivities_crutch, R.string.activity_subactivities_general_other, R.string.activity_subactivities_cleaning, R.string.activity_subactivities_vacuum, R.string.activity_subactivities_general_other, R.string.activity_subactivities_athome, R.string.activity_subactivities_bar, R.string.activity_subactivities_cinema, R.string.activity_subactivities_park, R.string.activity_subactivities_general_other, R.string.activity_subactivities_walk, R.string.activity_subactivities_gotowork, R.string.activity_subactivities_gohome, R.string.activity_subactivities_general_other, R.string.activity_subactivities_soccer, R.string.activity_subactivities_basketball, R.string.activity_subactivities_gym, R.string.activity_subactivities_gymnastics, R.string.activity_subactivities_dance, R.string.activity_subactivities_icehockey, R.string.activity_subactivities_jogging, R.string.activity_subactivities_bicycling, R.string.activity_subactivities_general_other);
        int[] activityid = new int[]{ 2, 2, 2, 2, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 6, 6, 9, 9, 9, 11, 11, 11, 11, 11, 12, 12, 12, 12, 14, 14, 14, 14, 14, 14, 14, 14, 14 };
        List<String> existSubActivities = DatabaseHelper.getStringResultSet("SELECT name FROM " + SQLTableName.SUBACTIVITIES, null);
        for(int i = 0; i < subactivities.length; i++) {
            if(existSubActivities.contains(subactivities[i])) {
                continue;
            }

            ContentValues newValues = new ContentValues();
            newValues.put("name", subactivities[i]);
            newValues.put("activityid", activityid[i]);
            SQLDBController.getInstance().insert(SQLTableName.SUBACTIVITIES, null, newValues);
        }
    }


    private static void modifyExistingTables()
    {
        // Add Column to Table SensorOptions
        List<String[]> tableInfo = SQLDBController.getInstance().query("PRAGMA table_info(" + SQLTableName.SENSOROPTIONS + ")", null, true);
        if(tableInfo.size() > 1 && tableInfo.get(0).length > 2 && "name".equals(tableInfo.get(0)[1]) && !"enabled".equals(tableInfo.get(tableInfo.size() - 1)[1])) {
            SQLDBController.getInstance().execSQL("ALTER TABLE " + SQLTableName.SENSOROPTIONS + " ADD COLUMN enabled BOOLEAN");
        }

        SQLDBController.getInstance().execSQL("UPDATE " + SQLTableName.SENSOROPTIONS + " SET freq=50 WHERE freq is NULL");

        // Remove old Enviornment Value
        List<String[]> environment = SQLDBController.getInstance().query("SELECT * FROM " + SQLTableName.POSITIONS + " WHERE name=?", UIUtils.getString(R.string.activity_environment_office_old), false);
        if(environment.size() > 0 && environment.get(0).length == 2) {
            String id = environment.get(0)[0];
            int affectedLines = SQLDBController.getInstance().delete(SQLTableName.POSITIONS, "id=?", new String[]{ id });
            if(affectedLines == 1) {
                SQLDBController.getInstance().execSQL("UPDATE " + SQLTableName.POSITIONS + " SET id=" + id + " WHERE name='" + UIUtils.getString(R.string.activity_environment_office)[0] + "';");
            }
        }
    }
}