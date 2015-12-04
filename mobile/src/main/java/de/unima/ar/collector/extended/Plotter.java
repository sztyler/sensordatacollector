package de.unima.ar.collector.extended;

import android.graphics.Color;

import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.Arrays;

import de.unima.ar.collector.util.BarPlotFormat;
import de.unima.ar.collector.util.PlotConfiguration;

/**
 * @author Timo Sztyler
 */
public class Plotter
{
    private PlotConfiguration levelPlot;
    private PlotConfiguration historyPlot;
    private SimpleXYSeries    levelsValues;
    private SimpleXYSeries[]  historyValues;
    private String            deviceID;
    private boolean           isPlotting;


    public Plotter(String deviceID, PlotConfiguration levelPlot, PlotConfiguration historyPlot)
    {
        this.deviceID = deviceID;
        this.levelPlot = levelPlot;
        this.historyPlot = historyPlot;

        this.levelPlot.deviceID = deviceID;
        this.historyPlot.deviceID = deviceID;

        this.isPlotting = false;
    }


    public void setDynamicPlotData(float[] values)
    {
        if(!isPlotting()) {
            return;
        }

        // update instantaneous data:
        Number[] series1Numbers = new Number[levelPlot.domainValueNames.length];

        for(int i = 0; i < levelPlot.domainValueNames.length; i++) {
            series1Numbers[i] = values[i];
        }

        levelsValues.setModel(Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

        if(historyPlot == null || historyValues == null || historyValues.length == 0) {
            return;
        }

        // get rid the oldest sample in history:
        if(historyValues[0].size() > historyPlot.domainMax - historyPlot.domainMin) {
            for(SimpleXYSeries historyValue : historyValues) {
                historyValue.removeFirst();
            }
        }

        for(int i = 0; i < historyValues.length; i++) {
            historyValues[i].addLast(null, values[i]);
        }

        // redraw the Plots:
        levelPlot.plot.redraw();
        historyPlot.plot.redraw();
    }


    public void startPlotting(XYPlot levelXYPlot, XYPlot historyXYPlot)
    {
        levelPlot.plot = levelXYPlot;
        historyPlot.plot = historyXYPlot;

        levelsValues = new SimpleXYSeries(levelPlot.SeriesName);
        levelsValues.useImplicitXVals();
        levelPlot.plot.addSeries(levelsValues, new BarFormatter(Color.argb(180, 0, 200, 0), Color.rgb(0, 100, 0)));
        levelPlot.plot.addSeries(new SimpleXYSeries(Arrays.asList(-10, 0, 10, 0), SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED, "0"), new LineAndPointFormatter(Color.argb(140, 200, 80, 80), Color.BLACK, null, null));
        levelPlot.plot.setTitle(levelPlot.plotName);

        levelPlot.plot.setDomainStepValue(levelPlot.domainValueNames.length);
        levelPlot.plot.setTicksPerRangeLabel(3);

        // per the android documentation, the minimum and maximum readings we can get from
        // any of the orientation sensors is -180 and 359 respectively so we will fix our plot's
        // boundaries to those values. If we did not do this, the plot would auto-range which
        // can be visually confusing in the case of dynamic plots.
        levelPlot.plot.setRangeBoundaries(levelPlot.rangeMin, levelPlot.rangeMax, levelPlot.rangeBoundary);

        levelPlot.plot.setDomainBoundaries(0, levelPlot.domainValueNames.length - 1, BoundaryMode.FIXED);

        // use our custom domain value formatter:
        levelPlot.plot.setDomainValueFormat(new BarPlotFormat(levelPlot.domainValueNames));

        // update our domain and range axis labels:
        levelPlot.plot.setDomainLabel(levelPlot.domainName);
        levelPlot.plot.getDomainLabelWidget().pack();
        levelPlot.plot.setRangeLabel(levelPlot.rangeName);
        levelPlot.plot.getRangeLabelWidget().pack();
        levelPlot.plot.setGridPadding(15, 0, 15, 0);

        // History Plot
        historyPlot.plot.setTitle(historyPlot.plotName);
        historyPlot.plot.setDomainStepValue(5);
        historyPlot.plot.setTicksPerRangeLabel(3);

        historyValues = new SimpleXYSeries[historyPlot.seriesValueNames.length];

        int Colors[] = new int[6];

        Colors[0] = Color.rgb(100, 100, 250);
        Colors[1] = Color.rgb(250, 100, 100);
        Colors[2] = Color.rgb(100, 250, 100);
        Colors[3] = Color.rgb(250, 100, 250);
        Colors[4] = Color.rgb(250, 250, 100);
        Colors[5] = Color.rgb(100, 250, 250);

        for(int i = 0; i < historyPlot.seriesValueNames.length; i++) {
            historyValues[i] = new SimpleXYSeries(historyPlot.seriesValueNames[i].replace("attr_", ""));
            historyValues[i].useImplicitXVals();

            if(i > 5) {
                historyPlot.plot.addSeries(historyValues[i], new LineAndPointFormatter(Color.rgb(100, 100, 200), Color.BLACK, null, null));
            } else {
                historyPlot.plot.addSeries(historyValues[i], new LineAndPointFormatter(Colors[i], Color.BLACK, null, null));
            }
        }

        historyPlot.plot.setRangeBoundaries(historyPlot.rangeMin, historyPlot.rangeMax, historyPlot.rangeBoundary);
        historyPlot.plot.setDomainBoundaries(historyPlot.domainMin, historyPlot.domainMax, historyPlot.domainBoundary);
        historyPlot.plot.setDomainStepValue(5);
        historyPlot.plot.setTicksPerRangeLabel(3);
        historyPlot.plot.setDomainLabel(historyPlot.domainName);
        historyPlot.plot.getDomainLabelWidget().pack();
        historyPlot.plot.setRangeLabel(historyPlot.rangeName);
        historyPlot.plot.getRangeLabelWidget().pack();

        // get a ref to the BarRenderer so we can make some changes to it:
        @SuppressWarnings( "rawtypes" ) BarRenderer barRenderer = (BarRenderer) levelPlot.plot.getRenderer(BarRenderer.class);
        if(barRenderer != null) {
            // make our bars a little thicker than the default so they can be seen better:
            // barRenderer.setBarWidth(300);
            barRenderer.setBarWidthStyle(BarRenderer.BarWidthStyle.VARIABLE_WIDTH);
            barRenderer.setBarGap(20);
        }
    }

    // TODO
    //    public void stopPlotting()
    //    {
    //        levelPlot = null;
    //        historyPlot = null;
    //        levelsValues = null;
    //        historyValues = null;
    //    }


    public boolean isPlotting()
    {
        return isPlotting;
    }


    public void setPlotting(boolean state)
    {
        this.isPlotting = state;
    }


    public PlotConfiguration getPlotConfiguration()
    {
        return this.levelPlot;
    }


    public String getDeviceID()
    {
        return this.deviceID;
    }


    public boolean plottingEnabled()
    {
        return !this.levelPlot.sensorName.equals("GPS");
    }
}