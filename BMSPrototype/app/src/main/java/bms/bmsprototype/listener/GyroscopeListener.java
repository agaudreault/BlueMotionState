package bms.bmsprototype.listener;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Listener allowing us to retrieve the most accurate azimuth, pitch, roll and inclination with
 * the help of gravity sensor, accelerometer sensor and geomagnetic sensor.
 */
public class GyroscopeListener implements SensorEventListener {

    private static final String TAG = "GyroscopeListener";
    private GyroscopeCallback callback;
    private SensorManager sensorManager;

    float[] gravityData = new float[3];           // Gravity or accelerometer
    float[] geomagneticData = new float[3];           // Magnetometer
    float[] result = new float[3];
    float[] rawRotationMatrix = new float[9];
    float[] rotationMatrix = new float[9];
    float[] inclinationMatrix = new float[9];
    boolean haveGravity = false;
    boolean haveAccelerometer = false;
    boolean haveGeomagnetic = false;


    /**
     * Callback to use when gyroscope information has changed.
     */
    public interface GyroscopeCallback {
        void call(final int azimuth, final int pitch, final int roll, final int inclination);
    }


    /**
     * Constructor to use
     *
     * @param manager Then instantiated {@link SensorManager}.
     */
    public GyroscopeListener(SensorManager manager){
        sensorManager = manager;
    }

    /**
     * Register to three different sensors in order to get an accurate gyroscopic information
     *
     * @param callback The callback to call when new values are known.
     */
    public void registerListener(GyroscopeCallback callback)
    {
        Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, geomagneticSensor, SensorManager.SENSOR_DELAY_GAME);

        this.callback = callback;
    }

    /**
     * Unregister the listener.
     */
    public void unregisterListener()
    {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     * Called each time a registered sensor get a new value, then compute
     * the orientation matrix.
     *
     * @param event The SensorEvent.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch( event.sensor.getType() ) {
            case Sensor.TYPE_GRAVITY:
                gravityData[0] = event.values[0];
                gravityData[1] = event.values[1];
                gravityData[2] = event.values[2];
                haveGravity = true;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (haveGravity) break;    // don't need it, we have better
                gravityData[0] = event.values[0];
                gravityData[1] = event.values[1];
                gravityData[2] = event.values[2];
                haveAccelerometer = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagneticData[0] = event.values[0];
                geomagneticData[1] = event.values[1];
                geomagneticData[2] = event.values[2];
                haveGeomagnetic = true;
                break;
            default:
                return;
        }

        if ((haveGravity || haveAccelerometer) && haveGeomagnetic) {
            SensorManager.getRotationMatrix(rawRotationMatrix, inclinationMatrix, gravityData, geomagneticData);
            SensorManager.remapCoordinateSystem(rawRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrix);

            SensorManager.getOrientation(rotationMatrix, result);

            final int azimuth = (int)Math.toDegrees(result[0]);
            final int pitch = (int)Math.toDegrees(result[1]);
            final int roll = (int)Math.toDegrees(result[2]);
            final int inclination = (int) Math.toDegrees(SensorManager.getInclination(inclinationMatrix));
            callback.call(azimuth, pitch, roll, inclination);
        }
    }
}