package com.scientifichackers.euler_angles

import android.app.Activity
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.annotation.NonNull
import com.pycampers.plugin_scaffold.createPluginScaffold
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.lang.Math.toDegrees

class EulerAnglesPlugin : FlutterPlugin, ActivityAware {
    var methods: PluginMethods? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        methods = PluginMethods(flutterPluginBinding.applicationContext)
        initPlugin(flutterPluginBinding.binaryMessenger, methods!!)
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val methods = PluginMethods(registrar.context())
            methods.activity = registrar.activity()
            initPlugin(registrar.messenger(), methods)
        }

        fun initPlugin(messenger: BinaryMessenger, methods: PluginMethods): MethodChannel {
            return createPluginScaffold(messenger, "com.scientifichackers/euler_angles", methods)
        }
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        methods?.activity = binding.activity
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {}
    override fun onDetachedFromActivity() {}
    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}
    override fun onDetachedFromActivityForConfigChanges() {}
}

class PluginMethods(val ctx: Context) : Application.ActivityLifecycleCallbacks,
    SensorEventListener {

    val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sinks = mutableMapOf<Int, EventSink>()

    var activity: Activity? = null
        set(value) {
            field?.application?.unregisterActivityLifecycleCallbacks(this)
            value?.let {
                field = value
                value.application?.registerActivityLifecycleCallbacks(this)
            }
        }

    fun sensorOnListen(id: Int, args: Any?, sink: EventSink) {
        registerSensorListener()
        sinks[id] = sink
    }

    fun sensorOnCancel(id: Int, args: Any?) {
        sinks.remove(id)
    }

    override fun onActivityResumed(activity: Activity?) {
        if (sinks.isEmpty()) {
            sensorManager.unregisterListener(this)
        } else {
            registerSensorListener()
        }
    }

    override fun onActivityPaused(activity: Activity?) {
        sensorManager.unregisterListener(this)
    }

    val accel = FloatArray(3)
    val magnet = FloatArray(3)

    fun registerSensorListener() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )

        val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(
            this,
            magneticField,
            SensorManager.SENSOR_DELAY_NORMAL,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    // Get readings from accelerometer and magnetometer
    override fun onSensorChanged(event: SensorEvent) {
        when {
            event.sensor.type == Sensor.TYPE_ACCELEROMETER -> System.arraycopy(
                event.values,
                0,
                accel,
                0,
                accel.size
            )
            event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(
                event.values,
                0,
                magnet,
                0,
                magnet.size
            )
        }
        updateOrientationAngles()
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    fun updateOrientationAngles() {
        val rotationMatrix = FloatArray(9)
        val orientationRadians = FloatArray(3)

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)

        // "rotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(rotationMatrix, orientationRadians)

        // convert to degrees
        val orientationDegrees = orientationRadians.map { toDegrees(it.toDouble()) }

        // push this information to flutter
        for (sink in sinks.values) {
            sink.success(
                hashMapOf(
                    "angles" to orientationDegrees,
                    "rotation" to getDisplayRotation()
                )
            )
        }
    }

    fun getDisplayRotation(): Int = activity?.run { windowManager.defaultDisplay.rotation } ?: 0

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    override fun onActivityStarted(activity: Activity?) {}
    override fun onActivityDestroyed(activity: Activity?) {}
    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}
    override fun onActivityStopped(activity: Activity?) {}
}