import 'package:flutter/services.dart';

class AndroidBridge {
  static const platform = MethodChannel('com.example.flutter/channel');

  Future<String> getData() async {
    try {
      final String result = await platform.invokeMethod('getDataFromAndroid');
      return result;
    } on PlatformException catch (e) {
      return "Failed: '${e.message}'.";
    }
  }
}
