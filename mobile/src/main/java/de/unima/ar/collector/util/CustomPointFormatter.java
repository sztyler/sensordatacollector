package de.unima.ar.collector.util;

import android.graphics.Color;

import com.androidplot.ui.SeriesRenderer;
import com.androidplot.xy.FillDirection;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.XYPlot;


/**
 * CustomFormatter to prevent icon creation in androidplot
 * 
 * @author Fabian Kramm
 *
 */
public class CustomPointFormatter extends LineAndPointFormatter
{
    public CustomPointFormatter()
    {
        super(Color.RED, Color.GREEN, Color.BLUE, null);
    }


    public CustomPointFormatter(Integer lineColor, Integer vertexColor, Integer fillColor, PointLabelFormatter plf)
    {
        super(lineColor, vertexColor, fillColor, plf, FillDirection.BOTTOM);
    }


    public CustomPointFormatter(Integer lineColor, Integer vertexColor, Integer fillColor, PointLabelFormatter plf, FillDirection fillDir)
    {
        super(lineColor, vertexColor, fillColor, plf, fillDir);
    }


    @SuppressWarnings ("rawtypes")
    @Override
    public SeriesRenderer getRendererInstance(XYPlot plot)
    {
        return new CustomPointRenderer(plot);
    }


    @SuppressWarnings ("rawtypes")
    @Override
    public Class<? extends SeriesRenderer> getRendererClass()
    {
        return CustomPointRenderer.class;
    }
}