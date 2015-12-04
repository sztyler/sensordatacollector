package de.unima.ar.collector.shared.database;

public class SQLTableMapper
{
    public static String getName(int type)
    {
        switch(type) {
            case 1:
                return SQLTableName.ACCELEROMETER;
            case 2:
                return SQLTableName.MAGNETIC;
            case 3:
                return SQLTableName.ORIENTATION;
            case 4:
                return SQLTableName.GYROSCOPE;
            case 5:
                return SQLTableName.LIGHT;
            case 6:
                return SQLTableName.PRESSURE;
            case 8:
                return SQLTableName.PROXIMITY;
            case 9:
                return SQLTableName.GRAVITY;
            case 10:
                return SQLTableName.LINEAR;
            case 11:
                return SQLTableName.ROTATION;
            case 12:
                return SQLTableName.RELATIVE;
            case 13:
                return SQLTableName.AMBIENT;
            case 18:
                return SQLTableName.STEP;
            case 19:
                return SQLTableName.STEPCOUNTER;
            case -2:
                return SQLTableName.MICROPHONE;
            case -3:
                return SQLTableName.GPS;
        }

        return null;
    }


    public static int getType(String sqlTableName)
    {
        switch(sqlTableName) {
            case SQLTableName.ACCELEROMETER:
                return 1;
            case SQLTableName.MAGNETIC:
                return 2;
            case SQLTableName.ORIENTATION:
                return 3;
            case SQLTableName.GYROSCOPE:
                return 4;
            case SQLTableName.LIGHT:
                return 5;
            case SQLTableName.PRESSURE:
                return 6;
            case SQLTableName.PROXIMITY:
                return 8;
            case SQLTableName.GRAVITY:
                return 9;
            case SQLTableName.LINEAR:
                return 10;
            case SQLTableName.ROTATION:
                return 11;
            case SQLTableName.RELATIVE:
                return 12;
            case SQLTableName.AMBIENT:
                return 13;
            case SQLTableName.STEP:
                return 18;
            case SQLTableName.STEPCOUNTER:
                return 19;
            case SQLTableName.MICROPHONE:
                return -2;
            case SQLTableName.GPS:
                return -3;
        }

        return 0;
    }
}
