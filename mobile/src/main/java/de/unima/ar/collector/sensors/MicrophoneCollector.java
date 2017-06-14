package de.unima.ar.collector.sensors;

import android.app.Activity;
import android.content.ContentValues;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.unima.ar.collector.R;
import de.unima.ar.collector.SensorDataCollectorService;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.shared.Settings;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.DeviceID;
import de.unima.ar.collector.shared.util.Utils;
import de.unima.ar.collector.util.PlotConfiguration;


/**
 * @author Timo Sztyler, Fabian Kramm
 */
public class MicrophoneCollector extends CustomCollector
{
    private static final int      type       = -2;
    private static final String[] valueNames = new String[]{ "attr_db", "attr_time" };

    private Timer timer;
    //    private MediaRecorder mRecorder = null;

    private static Map<String, Plotter> plotters     = new HashMap<>();
    private static int[]                mSampleRates = new int[]{ 8000, 11025, 22050, 44100 };


    MicrophoneCollector()
    {
        super();

        if(this.sensorRate < 0) {
            this.sensorRate = Settings.MICRO_DEFAULT_FREQUENCY;
        }

        // create new plotter
        List<String> devices = DatabaseHelper.getStringResultSet("SELECT device FROM " + SQLTableName.DEVICES, null);
        for(String device : devices) {
            MicrophoneCollector.createNewPlotter(device);
        }
    }


    @Override
    public void onRegistered()
    {
        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                doTask();
            }
        }, 0, getSensorRate());

        /*
         * if (mRecorder == null) { mRecorder = new MediaRecorder();
         * mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
         * mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
         * mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
         * mRecorder.setOutputFile("/dev/null"); try { mRecorder.prepare(); }
         * catch (IllegalStateException e) { e.printStackTrace(); } catch
         * (IOException e) { e.printStackTrace(); } mRecorder.start(); }
         * //
         */
    }


    @Override
    public void onDeRegistered()
    {
        this.timer.cancel();

        // ar.stop();
        //        if(mRecorder != null) {
        //            mRecorder.stop();
        //            mRecorder.release();
        //            mRecorder = null;
        //        }
    }


    private void doTask()
    {
        // http://stackoverflow.com/questions/10655703/what-does-androids-getmaxamplitude-function-for-the-mediarecorder-actually-gi

        int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
        // making the buffer bigger....
        bufferSize = bufferSize * 4;
        // AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        AudioRecord recorder = findAudioRecord();
        if(recorder == null) {
            Activity main = ActivityController.getInstance().get("MainActivity");
            if(main != null) {
                Utils.makeToast2(main, R.string.sensor_microphone_error, Toast.LENGTH_SHORT);
            }

            return;
        }

        short data[] = new short[bufferSize];
        double average = 0.0;
        recorder.startRecording();
        // recording data;
        recorder.read(data, 0, bufferSize);

        recorder.stop();
        for(short s : data) {
            if(s > 0) {
                average += Math.abs(s);
            } else {
                bufferSize--;
            }
        }
        // x=max;
        double x = average / bufferSize;
        recorder.release();
        double db;
        if(x == 0) {
            Log.w("TAG", "Warning no sound captured!");
            return;
        }
        // calculating the pascal pressure based on the idea that the max
        // amplitude (between 0 and 32767) is
        // relative to the pressure
        double pressure = x / 51805.5336; // the value 51805.5336 can be derived
        // from asuming that x=32767=0.6325
        // Pa and x=1 = 0.00002 Pa (the
        // reference value)
        db = (20 * Math.log10(pressure / 0.00002));
        if(db < 0) {
            return;
        }

        // if( mRecorder == null )
        // return;

        // float maxVolume = (float)(20 * Math.log10(mRecorder.getMaxAmplitude()
        // / 2700.0));

        long time = System.currentTimeMillis();
        ContentValues newValues = new ContentValues();
        newValues.put(valueNames[0], db);
        newValues.put(valueNames[1], time);

        if(db == Double.NEGATIVE_INFINITY || db == Double.POSITIVE_INFINITY || db == Double.NaN) {
            return;
        }

        String deviceID = DeviceID.get(SensorDataCollectorService.getInstance());
        MicrophoneCollector.updateLivePlotter(deviceID, new float[]{ (float) db });
        MicrophoneCollector.writeDBStorage(deviceID, newValues);
    }


    @Override
    public Plotter getPlotter(String deviceID)
    {
        if(!plotters.containsKey(deviceID)) {
            MicrophoneCollector.createNewPlotter(deviceID);
        }

        return plotters.get(deviceID);
    }


    @Override
    public int getType()
    {
        return type;
    }


    public static void createNewPlotter(String deviceID)
    {
        PlotConfiguration levelPlot = new PlotConfiguration();
        levelPlot.plotName = "LevelPlot";
        levelPlot.rangeMin = 0;
        levelPlot.rangeMax = 200;
        levelPlot.rangeName = "db";
        levelPlot.SeriesName = "volume";
        levelPlot.domainName = "Axis";
        levelPlot.domainValueNames = Arrays.copyOfRange(valueNames, 0, 1);
        levelPlot.tableName = SQLTableName.MICROPHONE;
        levelPlot.sensorName = "Microphone";


        PlotConfiguration historyPlot = new PlotConfiguration();
        historyPlot.plotName = "HistoryPlot";
        historyPlot.rangeMin = 0;
        historyPlot.rangeMax = 200;
        historyPlot.domainMin = 0;
        historyPlot.domainMax = 80;
        historyPlot.rangeName = "db";
        historyPlot.SeriesName = "volume";
        historyPlot.domainName = "Time";
        historyPlot.seriesValueNames = Arrays.copyOfRange(valueNames, 0, 1);

        Plotter plotter = new Plotter(deviceID, levelPlot, historyPlot);
        plotters.put(deviceID, plotter);
    }


    public static void updateLivePlotter(String deviceID, float[] values)
    {
        Plotter plotter = plotters.get(deviceID);
        if(plotter == null) {
            MicrophoneCollector.createNewPlotter(deviceID);
            plotter = plotters.get(deviceID);
        }

        plotter.setDynamicPlotData(values);
    }


    public static void createDBStorage(String deviceID)
    {
        String sqlTable = "CREATE TABLE IF NOT EXISTS " + SQLTableName.PREFIX + deviceID + SQLTableName.MICROPHONE + " (id INTEGER PRIMARY KEY, " + valueNames[1] + " INTEGER, " + valueNames[0] + " REAL)";
        SQLDBController.getInstance().execSQL(sqlTable);
    }


    public static void writeDBStorage(String deviceID, ContentValues newValues)
    {
        SQLDBController.getInstance().insert(SQLTableName.PREFIX + deviceID + SQLTableName.MICROPHONE, null, newValues);
    }


    private AudioRecord findAudioRecord()
    {
        for(int rate : mSampleRates) {
            for(short audioFormat : new short[]{ AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT }) {
                for(short channelConfig : new short[]{ AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.CHANNEL_IN_MONO }) {
                    try {
                        Log.d("MicrophoneCollector", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if(bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if(recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch(Exception e) {
                        Log.e("MicrophoneCollector", rate + " Exception, keep trying.", e);
                    }
                }
            }
        }
        return null;
    }
}