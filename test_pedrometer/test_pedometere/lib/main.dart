import 'package:flutter/material.dart';
import 'package:flutter_wear_os_connectivity/flutter_wear_os_connectivity.dart';
import 'package:provider/provider.dart';

void main() {
  runApp(
    ChangeNotifierProvider(
      create: (_) => HeartRateProvider(),
      child: const MyApp(),
    ),
  );
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Wear HR Receiver',
      theme: ThemeData.dark(),
      home: const HeartRateScreen(),
    );
  }
}

// Provider to hold latest HR
class HeartRateProvider extends ChangeNotifier {
  String _hr = "-";

  String get hr => _hr;

  set hr(String value) {
    _hr = value;
    notifyListeners();
  }
}

class HeartRateScreen extends StatefulWidget {
  const HeartRateScreen({super.key});

  @override
  State<HeartRateScreen> createState() => _HeartRateScreenState();
}

class _HeartRateScreenState extends State<HeartRateScreen> {
  final FlutterWearOsConnectivity _wear = FlutterWearOsConnectivity();

  @override
  void initState() {
    super.initState();
    _initWearOS();
  }

  void _initWearOS() async {
    await _wear.configureWearableAPI();

    _wear.messageReceived().listen((message) {
      if (message.path == "/heart_rate") {
        String hr = String.fromCharCodes(message.data);
        Provider.of<HeartRateProvider>(context, listen: false).hr = hr;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    String hr = Provider.of<HeartRateProvider>(context).hr;

    return Scaffold(
      appBar: AppBar(title: const Text("Wear Heart Rate")),
      body: Center(
        child: Text(
          "Heart Rate: $hr",
          style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }
}
