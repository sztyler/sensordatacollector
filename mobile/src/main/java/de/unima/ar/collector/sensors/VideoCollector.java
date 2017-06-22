package de.unima.ar.collector.sensors;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.controller.SQLDBController;
import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.shared.util.Utils;

/**
 * @author Timo Sztyler
 */
public class VideoCollector extends CustomCollector implements SurfaceHolder.Callback
{
    private static final int      type       = -4;
    private static final String[] valueNames = new String[]{ "attr_video", "attr_time" };

    private MediaRecorder recorder = null;
    private WindowManager windowManager;
    private SurfaceView   surfaceView;
    private long          startTime;
    private boolean       isRunning;


    @Override
    public void onRegistered()
    {
        this.isRunning = false;
        this.startTime = System.currentTimeMillis();
        Activity main = ActivityController.getInstance().get("MainActivity");

        WindowManager.LayoutParams layoutParams;
        if(Build.VERSION.SDK_INT >= 26) {
            layoutParams = new WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);
        } else {
            layoutParams = new WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW, WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);
        }
        layoutParams.gravity = Gravity.START | Gravity.TOP;

        this.windowManager = (WindowManager) main.getSystemService(Context.WINDOW_SERVICE);
        this.surfaceView = new SurfaceView(main);

        this.windowManager.addView(this.surfaceView, layoutParams);
        this.surfaceView.getHolder().addCallback(this);
    }


    @Override
    public void onDeRegistered()
    {
        if(this.isRunning) {
            this.recorder.stop();
        }
        this.recorder.reset();
        this.recorder.release();

        this.windowManager.removeView(this.surfaceView);
    }


    @Override
    public int getType()
    {
        return type;
    }


    @Override
    public Plotter getPlotter(String deviceID)
    {
        return null;
    }


    public static void createDBStorage(String deviceID)
    {
        String sqlTable = "CREATE TABLE IF NOT EXISTS " + SQLTableName.PREFIX + deviceID + SQLTableName.VIDEO + " (id INTEGER PRIMARY KEY, " + valueNames[1] + " INTEGER, " + valueNames[0] + " BLOB)";
        SQLDBController.getInstance().execSQL(sqlTable);
    }


    public static void writeDBStorage(String deviceID, ContentValues newValues)
    {
        //String deviceID = DeviceID.get(SensorDataCollectorService.getInstance());
        // TODO
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder)
    {
        try {
            // init recorder
            this.recorder = new MediaRecorder();
            this.recorder.setPreviewDisplay(surfaceHolder.getSurface());
            this.recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            this.recorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
            // this.recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            // this.recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            // this.recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            CamcorderProfile cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            recorder.setProfile(cpHigh);

            this.recorder.setMaxDuration(0); // 0 seconds = unlimit
            this.recorder.setMaxFileSize(0); // 0 = unlimit

            // set output path
            File extStore = Environment.getExternalStorageDirectory();
            File root = new File(extStore.getAbsolutePath(), "SensorDataCollector");
            boolean result = root.mkdir();
            if(!result && !root.exists()) {
                return; // TODO
            }
            File output = new File(root.getAbsolutePath(), "video_" + startTime + ".mp4");
            this.recorder.setOutputFile(output.getAbsolutePath());

            // start
            try {
                this.recorder.prepare();
            } catch(Exception e) {
                this.recorder.reset();
                e.printStackTrace();
            }

            this.recorder.start();
            this.isRunning = true;

            Activity main = ActivityController.getInstance().get("MainActivity");
            if(main != null) {
                Utils.makeToast2(main, R.string.sensor_video_storage, Toast.LENGTH_SHORT);
            }
        } catch(Exception e) {
            Activity main = ActivityController.getInstance().get("MainActivity");
            if(main != null) {
                Utils.makeToast2(main, R.string.sensor_video_error, Toast.LENGTH_SHORT);
            }

            e.printStackTrace();
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // nothing to do
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // nothing to do
    }
}