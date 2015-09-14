package de.unima.ar.collector.util;

import android.graphics.*;

import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.LineAndPointRenderer;
import com.androidplot.xy.XYPlot;


/**
 * CustomRenderer for CustomFormatter
 * 
 * @author Fabian Kramm
 *
 */
@SuppressWarnings ("rawtypes")
public class CustomPointRenderer extends LineAndPointRenderer
{

    public CustomPointRenderer(XYPlot plot)
    {
        super(plot);
    }


    @Override
    public void doDrawLegendIcon(Canvas canvas, RectF rect, LineAndPointFormatter formatter)
    {
        // do not draw icon
        if(formatter.getFillPaint() != null) {
            canvas.drawRect(rect, formatter.getFillPaint());
        }
    }
}