package de.unima.ar.collector.shared;

public class Settings
{
    public static double  SENSOR_DEFAULT_FREQUENCY = 50.0d;    // Hz
    public static long    GPS_DEFAULT_FREQUENCY    = 600000l; // Milliseconds 600000l = 10min
    public static boolean ACCLOWPASS               = false;

    public static boolean DATABASE_DIRECT_INSERT = false;
    public static int     DATABASE_CACHE_SIZE    = 10000;   // number of entries
    public static String  DATABASE_DELIMITER     = "-#~o~#-";

    public static String ACTIVITY_DELIMITER = " - ";

    public static int EXPORT_ATONCE = 10000;

    public static boolean LIVE_PLOTTER_DISABLE = false;

    public static boolean WEARDIRECTTRANSFER   = false;
    public static int     WEARTRANSFERIDLETIME = 120000;   // milliseconds
    public static int     WEARTRANSFERTIMEOUT  = 1200;
    public static int     WEARTRANSFERSIZE     = 1000;
    public static boolean WEARSENSORDISABLED   = true;
}