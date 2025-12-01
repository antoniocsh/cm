import 'package:flutter/material.dart';
import 'package:flutter_smart_watch/flutter_smart_watch.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return const MaterialApp(
      home: ConnectivityCheckerScreen(),
    );
  }
}

class ConnectivityCheckerScreen extends StatefulWidget {
  const ConnectivityCheckerScreen({Key? key}) : super(key: key);

  @override
  State<ConnectivityCheckerScreen> createState() =>
      _ConnectivityCheckerScreenState();
}

class _ConnectivityCheckerScreenState extends State<ConnectivityCheckerScreen> {
  late final FlutterWearOsConnectivity _flutterWearOsConnectivity;

  String _wearOsStatus = 'Initializing...';
  String _connectedDevices = '';

  @override
  void initState() {
    super.initState();
    // 1. Get plugin instances
    _flutterWearOsConnectivity = FlutterSmartWatch().wearOS;

    // 2. Initialize connectivity
    _initializeConnectivity();
  }

  Future<void> _initializeConnectivity() async {
    try {
      // 1. CONFIGURATION
      await _flutterWearOsConnectivity.configureWearableAPI();
      

      // 3. CHECK CONNECTED DEVICES 
      List<WearOsDevice> devices = [];
      devices = await _flutterWearOsConnectivity.getConnectedDevices();
      

      if (!mounted) return;

      setState(() {
          _wearOsStatus = 'WearOS is Supported';
          _connectedDevices = devices.isNotEmpty 
              ? '${devices.length} device(s) found: \n${devices.map((d) => d.name).join(", ")}' 
              : 'No devices connected via Bluetooth/Cloud.';
     
      });

    } catch (e) {
      if (!mounted) return;
      setState(() {
        _wearOsStatus = 'Error: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('WearOS Connectivity'),
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Icon(Icons.watch, size: 64, color: Colors.green),
              const SizedBox(height: 20),
              Text(
                _wearOsStatus,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 20),
              const Divider(),
              const SizedBox(height: 20),
              const Text("Connected Devices:", style: TextStyle(color: Colors.grey)),
              const SizedBox(height: 10),
              Text(
                _connectedDevices,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 16),
              ),
            ],
          ),
        ),
      ),
    );
  }
}