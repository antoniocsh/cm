import 'package:flutter/services.dart';

class WearOSCommunication {
  static const EventChannel _hrEventChannel =
      EventChannel('com.samsung.health.hrdatatransfer/heart_rate');

  static Stream<String> get heartRateStream =>
      _hrEventChannel.receiveBroadcastStream().map((event) => event.toString());
}
