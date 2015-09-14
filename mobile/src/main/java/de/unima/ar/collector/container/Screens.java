package de.unima.ar.collector.container;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

import de.unima.ar.collector.extended.Plotter;
import de.unima.ar.collector.extended.SensorSelfTest;

public class Screens
{
    private static List<Pair<Type, Object[]>> history = new ArrayList<>();


    public enum Type
    {
        SENSOREN, ANALYZE, ANALYZE_LIVE, ANALYZE_DATABASE, ANALYZE_FEATURES_OVERVIEW, ANALYZE_FEATURES_DETAILED, OPTIONS, SENSOREN_DETAILS, ACTIVITIES, SENSOREN_SELFTEST, ADDITIONAL_DEVICES, ACTIVITY_CORRECTION, SETTINGS
    }


    public static Pair<Type, Object[]> current()
    {
        if(history.size() == 0) {
            return null;
        }

        return history.get(history.size() - 1);
    }


    public static boolean rm(int id)
    {
        if(history.size() <= id) {
            return false;
        }

        history.remove(id);
        return true;
    }


    public static Pair<Type, Object[]> get(int id)
    {
        if(history.size() < id) {
            return null;
        }

        return history.get(id);
    }


    public static int size()
    {
        return history.size();
    }


    public static void add(Type screen)
    {
        add(screen, new Object[]{});
    }


    public static void add(Type screen, Object[] data)
    {
        Pair<Type, Object[]> entry = new Pair<>(screen, data);

        // disable sensors if selftest was active
        if(history.size() > 0 && history.get(history.size() - 1).first == Type.SENSOREN_SELFTEST) {
            ((SensorSelfTest) history.get(history.size() - 1).second[2]).stopTest();
        }

        // disable live plotter if it was active
        if(history.size() > 0 && history.get(history.size() - 1).first == Type.ANALYZE_LIVE) {
            ((Plotter) history.get(history.size() - 1).second[2]).setPlotting(false);
        }

        // Maximal die letzten 10 Screens erinnern
        if(history.size() >= 9) {
            history.remove(0); // Lösche ersten Eintrag
        }

        history.add(entry);
    }
}