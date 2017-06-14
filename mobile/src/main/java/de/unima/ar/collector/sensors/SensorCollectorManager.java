package de.unima.ar.collector.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Fabian Kramm, Timo Sztyler
 * @version 11.12.2014
 */
public class SensorCollectorManager
{
    private Context                       context;
    private Map<Integer, SensorCollector> sensorCollectors;
    private Map<Integer, CustomCollector> customCollectors;
    private Set<Integer>                  enabledCollectors;
    private SensorManager                 sensorManager;

    private static final String LOG_TAG = "SensorCollectorManager";


    public SensorCollectorManager(Context context)
    {
        this.context = context;

        this.sensorCollectors = new HashMap<>();
        this.customCollectors = new HashMap<>();
        this.enabledCollectors = new HashSet<>();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);

        initSensors();
    }


    private void initSensors()
    {
        // Add all sensors
        SensorManager mSensorManager = (SensorManager) this.context.getSystemService(Activity.SENSOR_SERVICE);
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for(Sensor sensor : allSensors) {
            this.addSensorCollector(sensor.getType(), sensor);
        }

        for(int type : CollectorConstants.activatedCustomCollectors) {
            this.addCustomCollector(type, this.context);
        }
    }


    public int getRunningSensorAmount()
    {
        return enabledCollectors.size();
    }


    public float getPowerUsed()
    {
        float amount = 0;

        for(Integer type : enabledCollectors) {
            if(sensorCollectors.containsKey(type)) {
                amount += sensorCollectors.get(type).getSensor().getPower();
            } else {
                Log.d(LOG_TAG, "getPowerUsed for custom collector is not implemented");
            }
        }

        return amount;
    }


    public Map<Integer, SensorCollector> getSensorCollectors()
    {
        return sensorCollectors;
    }


    public Map<Integer, CustomCollector> getCustomCollectors()
    {
        return customCollectors;
    }


    public Set<Integer> getEnabledCollectors()
    {
        return this.enabledCollectors;
    }


    public boolean enableCollectors(int type)
    {
        if(!this.sensorCollectors.containsKey(type) && !this.customCollectors.containsKey(type)) {
            return false;
        }
        this.enabledCollectors.add(type);

        return true;
    }


    public boolean disableCollectors(int type)
    {
        return this.enabledCollectors.remove(type);
    }


    public boolean removeSensor(int type)
    {
        boolean result = unregisterSensorCollector(type);

        if(result) {
            disableCollectors(type);
        }

        return result;
    }


    private boolean addCustomCollector(int type, Context sensor)
    {
        CustomCollector cc = CustomCollectorFactory.getCollector(type, sensor);

        if(cc == null) {
            return false;
        }

        if(this.customCollectors.containsKey(cc.getType())) {
            return true;
        }

        this.customCollectors.put(cc.getType(), cc);

        return true;
    }


    private boolean addSensorCollector(int type, Sensor sensor)
    {
        SensorCollector sc = SensorCollectorFactory.getCollector(type, sensor);

        if(sc == null) {
            return false;
        }

        if(this.customCollectors.containsKey(sc.getType())) {
            return true;
        }

        this.sensorCollectors.put(sc.getType(), sc);

        return true;
    }


    public void registerSensorCollectors()
    {
        for(int type : this.sensorCollectors.keySet()) {
            registerSensorCollector(type);
        }
    }


    public void registerSensorCollector(int type)
    {
        SensorCollector sel = this.sensorCollectors.get(type);

        if(sel == null || !sel.registerCollector || sel.isRegistered || !enabledCollectors.contains(sel.getType())) {
            return;
        }

        if(sel.getSensor() != null) {
            this.sensorManager.registerListener(sel, sel.getSensor(), sel.getSensorRate());
            sel.isRegistered = true;
        } else {    // Fall 2: Es gibt einen Default Sensor für den Sensortyp
            if(this.sensorManager.getDefaultSensor(sel.getType()) != null) {
                this.sensorManager.registerListener(sel, this.sensorManager.getDefaultSensor(sel.getType()), sel.getSensorRate());
                sel.isRegistered = true;
            } else { // Fall 3: Es existiert kein Default Sensor für den Sensortyp
                Log.e(LOG_TAG, "Error! There is no default sensor.");
            }
        }
    }


    public void registerCustomCollectors()
    {
        for(CustomCollector cc : customCollectors.values()) {
            if(!cc.isRegistered() && enabledCollectors.contains(cc.getType())) {
                cc.register();
            }
        }
    }


    private boolean unregisterSensorCollector(int type)
    {
        for(SensorCollector sel : this.sensorCollectors.values()) {
            if(!(sel.isRegistered && sel.getType() == type && this.enabledCollectors.contains(sel.getType()))) {
                continue;
            }

            this.sensorManager.unregisterListener(sel);
            sel.isRegistered = false;
            return true;
        }

        return false;
    }


    public boolean unregisterCustomCollector(int type)
    {
        CustomCollector cc = customCollectors.get(type);
        if(!(cc != null && cc.isRegistered() && this.enabledCollectors.contains(cc.getType()))) {
            return false;
        }

        cc.deregister();
        return true;
    }


    public void unregisterSensorCollectors()
    {
        for(SensorCollector sel : this.sensorCollectors.values()) {
            if(sel.isRegistered) {
                this.sensorManager.unregisterListener(sel);
                sel.isRegistered = false;
            }
        }
    }


    public boolean isCustomCollectorRegistered(int type)
    {
        return this.enabledCollectors.contains(type);
    }
}