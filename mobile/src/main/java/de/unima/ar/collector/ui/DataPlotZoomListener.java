package de.unima.ar.collector.ui;

import android.graphics.Color;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unima.ar.collector.MainActivity;
import de.unima.ar.collector.R;
import de.unima.ar.collector.controller.ActivityController;
import de.unima.ar.collector.database.DatabaseHelper;
import de.unima.ar.collector.shared.database.SQLTableName;
import de.unima.ar.collector.util.PlotConfiguration;
import de.unima.ar.collector.util.SensorDataUtil;


/**
 * Regelt zoom verhalten für die datenbank anzeige plots
 *
 * @author Fabian Kramm
 */
public class DataPlotZoomListener implements View.OnTouchListener
{
    private XYPlot            plot;
    private PlotConfiguration pc;

    private String start;
    private String end;

    public XYPlot ActivityPlot;
    public XYPlot PositionPlot;
    public XYPlot PosturePlot;

    public PointF minXY;
    public PointF maxXY;

    private double maxY = 0;
    private double minY = 0;

    private Map<String, List<Double>> valueData;

    private SimpleXYSeries[] series;

    // Definition of the touch states
    static final int NONE             = 0;
    static final int ONE_FINGER_DRAG  = 1;
    static final int TWO_FINGERS_DRAG = 2;
    int mode = NONE;

    PointF firstFinger;
    float  lastScrolling;
    float  distBetweenFingers;
    float  lastZooming;


    public DataPlotZoomListener(XYPlot plot, PlotConfiguration pc, String start, String end)
    {
        this.plot = plot;
        this.pc = pc;
        this.start = start;
        this.end = end;
        this.valueData = new HashMap<>();
    }


    public void start()
    {
        series = new SimpleXYSeries[pc.domainValueNames.length];

        MainActivity main = (MainActivity) ActivityController.getInstance().get("MainActivity");
        main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading2, 5);

        String tableName = SQLTableName.PREFIX + pc.deviceID + pc.tableName;
        DatabaseHelper.streamData(tableName, pc.domainValueNames, start, end, valueData);

        main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading4, 20);

        for(int i = 0; i < pc.domainValueNames.length; i++) {
            series[i] = new SimpleXYSeries(valueData.get("attr_time"), valueData.get(pc.domainValueNames[i]), pc.domainValueNames[i].replace("attr_", ""));
            plot.addSeries(series[i], new LineAndPointFormatter(SensorDataUtil.getColor(i), Color.BLACK, null, null));
        }

        plot.calculateMinMaxVals();

        minXY = new PointF(plot.getCalculatedMinX().floatValue(), plot.getCalculatedMinY().floatValue());
        maxXY = new PointF(plot.getCalculatedMaxX().floatValue(), plot.getCalculatedMaxY().floatValue());

        maxY = plot.getCalculatedMaxY().doubleValue() + 1;
        minY = plot.getCalculatedMinY().doubleValue() - 1;

        plot.setRangeBoundaries(minY, maxY, BoundaryMode.FIXED);
        plot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);

        main.refreshGraphBuilderProgressBar(R.string.analyze_analyzelive_loading5, 80);

        plot.redraw();
    }


    private void refreshSeries()
    {
        for(int i = 0; i < pc.domainValueNames.length; i++) {
            plot.removeSeries(series[i]);
        }

        // New zoom
        double zoomStart = (((double) minXY.x) + 1388534400000d);
        double zoomEnd = (((double) maxXY.x) + 1388534400000d);

        //        DatabaseHelper.streamData(pc.tableName, "SELECT " + values + " FROM " + pc.tableName, start, "" + zoomStart, timeData, valueData, 150);
        //        DatabaseHelper.streamData(pc.tableName, "SELECT " + values + " FROM " + pc.tableName, "" + zoomStart, "" + zoomEnd, timeData, valueData, 400);
        //        DatabaseHelper.streamData(pc.tableName, "SELECT " + values + " FROM " + pc.tableName, "" + zoomEnd, end, timeData, valueData, 150);

        for(int i = 0; i < pc.domainValueNames.length; i++) {
            series[i] = new SimpleXYSeries(valueData.get("attr_time"), valueData.get(pc.domainValueNames[i]), pc.domainValueNames[i].replace("attr_", ""));

            plot.addSeries(series[i], new LineAndPointFormatter(SensorDataUtil.getColor(i), Color.BLACK, null, null));
        }

        plot.calculateMinMaxVals();

        if(plot.getCalculatedMaxY().doubleValue() > maxY || plot.getCalculatedMinY().doubleValue() < minY) {
            maxY = (plot.getCalculatedMaxY().doubleValue() > maxY) ? plot.getCalculatedMaxY().doubleValue() + 1 : maxY;
            minY = (plot.getCalculatedMinY().doubleValue() < minY) ? plot.getCalculatedMinY().doubleValue() - 1 : minY;

            plot.setRangeBoundaries(minY, maxY, BoundaryMode.FIXED);
            plot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
        }

        plot.redraw();
    }


    @Override
    public boolean onTouch(View arg0, MotionEvent event)
    {
        switch(event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // Start gesture
                firstFinger = new PointF(event.getX(), event.getY());
                mode = ONE_FINGER_DRAG;
                break;
            case MotionEvent.ACTION_UP:
                arg0.performClick();
            case MotionEvent.ACTION_POINTER_UP:
                if(mode != NONE) {
                    refreshSeries();
                }

                mode = NONE;
                break;

            case MotionEvent.ACTION_POINTER_DOWN: // second finger
                distBetweenFingers = spacing(event);
                // the distance check is done to avoid false alarms
                if(distBetweenFingers > 5f) {
                    mode = TWO_FINGERS_DRAG;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(mode == ONE_FINGER_DRAG) {
                    PointF oldFirstFinger = firstFinger;
                    firstFinger = new PointF(event.getX(), event.getY());
                    lastScrolling = oldFirstFinger.x - firstFinger.x;
                    scroll(lastScrolling);
                    lastZooming = (firstFinger.y - oldFirstFinger.y) / plot.getHeight();
                    if(lastZooming < 0) {
                        lastZooming = 1 / (1 - lastZooming);
                    } else {
                        lastZooming += 1;
                    }
                    zoom(lastZooming);
                } else if(mode == TWO_FINGERS_DRAG) {
                    float oldDist = distBetweenFingers;
                    distBetweenFingers = spacing(event);

                    if(distBetweenFingers == 0) {
                        break;
                    }

                    lastZooming = oldDist / distBetweenFingers;

                    zoom(lastZooming);
                    plot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
                    ActivityPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
                    PositionPlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
                    PosturePlot.setDomainBoundaries(minXY.x, maxXY.x, BoundaryMode.FIXED);
                    PositionPlot.redraw();
                    plot.redraw();
                    ActivityPlot.redraw();
                    PosturePlot.redraw();
                }
                break;
        }
        return true;
    }


    private void zoom(float scale)
    {
        float domainSpan = maxXY.x - minXY.x;
        float domainMidPoint = maxXY.x - domainSpan / 2.0f;
        float offset = domainSpan * scale / 2.0f;
        minXY.x = domainMidPoint - offset;
        maxXY.x = domainMidPoint + offset;
    }


    private void scroll(float pan)
    {
        float domainSpan = maxXY.x - minXY.x;
        float step = domainSpan / plot.getWidth();
        float offset = pan * step;
        minXY.x += offset;
        maxXY.x += offset;
    }


    private float spacing(MotionEvent event)
    {
        if(event.getPointerCount() < 2) {
            return 0;
        }

        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
}