package de.unima.ar.collector.util;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;


/**
 * @author Fabian Kramm, Timo Sztyler
 */
public class PlotConfiguration implements Comparable<PlotConfiguration>
{
    public XYPlot plot;

    public String plotName   = "DefaultName";
    public String tableName  = "";
    public String sensorName = "";

    public String rangeName = "RangeName";
    public int    rangeMin  = 0;
    public int    rangeMax  = 100;

    public String   domainName       = "DomainName";
    public int      domainMin        = 0;
    public int      domainMax        = 100;
    public String[] domainValueNames = { };

    public String   SeriesName       = "SeriesName";
    public String[] seriesValueNames = { };

    public String deviceID;

    public BoundaryMode rangeBoundary  = BoundaryMode.FIXED;
    public BoundaryMode domainBoundary = BoundaryMode.FIXED;


    @Override
    public int compareTo(PlotConfiguration another)
    {
        String self = this.sensorName.concat(this.deviceID);
        String other = another.sensorName.concat(another.deviceID);

        return self.compareTo(other);
    }
}