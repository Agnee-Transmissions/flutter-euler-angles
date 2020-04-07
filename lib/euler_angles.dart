import 'package:flutter/services.dart';
import 'package:plugin_scaffold/plugin_scaffold.dart';

enum DisplayRotation { angle_0, angle_90, angle_180, angle_270 }

class EulerSensorData {
  final double roll, pitch, yaw;
  final DisplayRotation rotation;

  EulerSensorData({this.roll, this.pitch, this.yaw, this.rotation});

  EulerSensorData._fromList(List angles, int rotation)
      : roll = angles[0],
        pitch = angles[1],
        yaw = angles[2],
        rotation = DisplayRotation.values[rotation];

  @override
  String toString() {
    return '$runtimeType(roll: $roll, pitch: $pitch, yaw: $yaw, rotation: $rotation)';
  }
}

class EulerAngles {
  static const MethodChannel channel = MethodChannel(
    'com.scientifichackers/euler_angles',
  );

  static Stream<EulerSensorData> _stream;

  static Stream<EulerSensorData> get stream {
    _stream ??= PluginScaffold.createStream(channel, 'sensor').map((it) {
      return EulerSensorData._fromList(it['angles'], it['rotation']);
    });
    return _stream.asBroadcastStream();
  }

  static void addListener(void onData(EulerSensorData event)) {
    stream.listen(onData);
  }
}
