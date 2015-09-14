package de.unima.ar.collector.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class StringUtils
{
    public static String formatSensorName(String s)
    {
        StringBuilder result = new StringBuilder();

        s = s.substring(5).toLowerCase(Locale.ENGLISH).trim();
        s = s.replace("_", " ");

        s = s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1);

        if(s.toUpperCase(Locale.ENGLISH).equals("GPS")) {
            return s.toUpperCase(Locale.ENGLISH);
        }

        for(int i = 0; i < s.length(); i++) {
            if((i - 1) >= 0 && s.charAt(i - 1) == ' ') {
                result.append(String.valueOf(s.charAt(i)).toUpperCase(Locale.ENGLISH));
                continue;
            }
            result.append(s.charAt(i));
        }

        return result.toString();
    }


    public static String convertByteArrayToString(byte[] data)
    {
        try {
            return new String(data, "UTF-8");
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String[] split(String s)
    {
        List<String> result = new ArrayList<>();

        int counter = 0;

        while(counter <= s.length()) {
            int pos = s.indexOf(";", counter);

            if(pos == -1) {
                pos = s.length();
            }

            String fragment = s.substring(counter, pos);
            result.add(fragment);

            counter += fragment.length() + 1;
        }

        return result.toArray(new String[result.size()]);
    }


    public static String formatFeatureValues(double d)
    {
        String s = String.valueOf(d);
        if(s.length() > 10) {
            return s.substring(0, 10);
        }

        for(int i = 0; i < (10 - s.length()); i++) {
            s += "0";
        }

        return s;
    }


    public static String relatedLabel(String method, String attr)
    {
        switch(method) {
            case "CC":
                if("x".equals(attr)) {
                    return "xy";
                } else if("y".equals(attr)) {
                    return "yz";
                } else if("z".equals(attr)) {
                    return "yx";
                }
                break;
            case "Orient":
                if("x".equals(attr)) {
                    return "Azimut";
                } else if("y".equals(attr)) {
                    return "Roll";
                } else if("z".equals(attr)) {
                    return "Pitch";
                }
                break;
            default:
                return attr;
        }

        return attr;
    }
}