import 'package:euler_angles/euler_angles.dart';
import 'package:flutter/material.dart';
import 'package:vector_math/vector_math.dart' show radians;

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        backgroundColor: Colors.black,
        body: Home(),
      ),
    );
  }
}

class Home extends StatefulWidget {
  @override
  _HomeState createState() => _HomeState();
}

class _HomeState extends State<Home> {
  @override
  Widget build(BuildContext context) {
    var size = MediaQuery.of(context).size;

    // calculate average values from moving window
    var pitch =
        window.map((it) => it.pitch).reduce((a, b) => a + b) / window.length;
    var yaw =
        window.map((it) => it.yaw).reduce((a, b) => a + b) / window.length;

    return Stack(
      children: <Widget>[
        if (window != null)
          Positioned.fill(
            child: Center(
              child: Transform.translate(
                offset: Offset(
                  radians(yaw) * size.width / 2,
                  -radians(pitch) * size.height / 2,
                ),
                child: Image.asset(
                  'assets/rocket.png',
                  width: 50,
                ),
              ),
            ),
          ),
      ],
    );
  }

  @override
  void initState() {
    super.initState();
    EulerAngles.addListener(onData);
  }

  var window = <EulerSensorData>[];

  void onData(EulerSensorData value) {
    setState(() {
      if (window.length > 50) {
        window.removeAt(0);
      }
      window.add(value);
    });
  }
}
