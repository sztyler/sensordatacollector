package de.unima.ar.collector.util;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;


/**
 * @author Fabian Kramm
 *
 */
public class BarPlotFormat extends Format
{
    private static final long serialVersionUID = 1L;
    private String[]          names;


    public BarPlotFormat(String[] names)
    {
        this.names = names;
    }


    /*
     * (non-Javadoc)
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
    {
        Number num = (Number) obj;

        // using num.intValue() will floor the value, so we add 0.5 to round instead:
        int roundNum = (int) (num.floatValue() + 0.5f);

        if(roundNum < 0 || roundNum >= names.length) { return toAppendTo.append("unknown"); }

        return toAppendTo.append(names[roundNum]);
    }


    /*
     * (non-Javadoc)
     * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
     */
    @Override
    public Object parseObject(String arg0, ParsePosition arg1)
    {
        return null;
    }
}