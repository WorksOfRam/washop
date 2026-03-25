import 'package:hive/hive.dart';

class StorageService {
  static const String _authBoxName = 'auth';
  static const String _cacheBoxName = 'cache';

  Box get _authBox => Hive.box(_authBoxName);
  Box get _cacheBox => Hive.box(_cacheBoxName);

  // Auth related
  String? get token => _authBox.get('token');
  set token(String? value) => _authBox.put('token', value);

  String? get userPhone => _authBox.get('user');
  set userPhone(String? value) => _authBox.put('user', value);

  String? get shopId => _authBox.get('shop');
  set shopId(String? value) => _authBox.put('shop', value);

  bool get isLoggedIn => token != null;

  Future<void> clearAuth() async {
    await _authBox.clear();
  }

  // Cache related
  Future<void> cacheData(String key, dynamic data) async {
    await _cacheBox.put(key, {
      'data': data,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  dynamic getCachedData(String key, {Duration maxAge = const Duration(minutes: 5)}) {
    final cached = _cacheBox.get(key);
    if (cached == null) return null;

    final timestamp = cached['timestamp'] as int;
    final age = DateTime.now().millisecondsSinceEpoch - timestamp;
    
    if (age > maxAge.inMilliseconds) {
      _cacheBox.delete(key);
      return null;
    }

    return cached['data'];
  }

  Future<void> clearCache() async {
    await _cacheBox.clear();
  }

  Future<void> clearAll() async {
    await clearAuth();
    await clearCache();
  }
}
