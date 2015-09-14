package de.unima.ar.collector.util;

import android.annotation.SuppressLint;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * @author Fabian Kramm
 *
 */
@SuppressLint ("SimpleDateFormat")
public class DateFormat extends Format
{
    private static final long serialVersionUID = 1L;


    /*
     * (non-Javadoc)
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos)
    {
        Number num = (Number) obj;

        // Wir haben die Zahl vorher bereits
        // subtrahiert also wieder draufaddieren
        // sonst k�nnen komische Daten entstehen
        long time = num.longValue() + 1388534400000l;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM HH:mm:ss");

        return toAppendTo.append(sdf.format(new Date(time)));
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