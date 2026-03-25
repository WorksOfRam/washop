import 'package:dio/dio.dart';
import 'package:hive/hive.dart';
import '../models/product.dart';
import '../models/order.dart';
import '../models/shop.dart';

class ApiService {
  // Use 10.0.2.2 for Android emulator, localhost for iOS simulator
  // For physical device, use your computer's IP address
  static const String baseUrl = 'http://10.0.2.2:8080/api';
  
  late final Dio _dio;
  final Box _authBox = Hive.box('auth');

  ApiService() {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 30),
      receiveTimeout: const Duration(seconds: 30),
      headers: {
        'Content-Type': 'application/json',
      },
    ));

    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        final token = _authBox.get('token');
        if (token != null) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        return handler.next(options);
      },
      onError: (error, handler) {
        if (error.response?.statusCode == 401) {
          _authBox.delete('token');
          _authBox.delete('user');
          _authBox.delete('shop');
        }
        return handler.next(error);
      },
    ));
  }

  // Auth APIs
  Future<LoginResponse> login(String phone) async {
    final response = await _dio.post('/auth/login', data: {'phone': phone});
    final data = _extractData(response);
    final loginResponse = LoginResponse.fromJson(data);
    
    await _authBox.put('token', loginResponse.token);
    await _authBox.put('user', loginResponse.user.phone);
    if (loginResponse.shop != null) {
      await _authBox.put('shop', loginResponse.shop!.id);
    }
    
    return loginResponse;
  }

  Future<LoginResponse> register({
    required String phone,
    required String shopName,
    String? ownerName,
    String? address,
  }) async {
    final response = await _dio.post('/auth/register', data: {
      'phone': phone,
      'shopName': shopName,
      'ownerName': ownerName,
      'address': address,
    });
    final data = _extractData(response);
    final loginResponse = LoginResponse.fromJson(data);
    
    await _authBox.put('token', loginResponse.token);
    await _authBox.put('user', loginResponse.user.phone);
    if (loginResponse.shop != null) {
      await _authBox.put('shop', loginResponse.shop!.id);
    }
    
    return loginResponse;
  }

  Future<void> logout() async {
    await _authBox.clear();
  }

  bool get isLoggedIn => _authBox.get('token') != null;
  String? get currentShopId => _authBox.get('shop');
  String? get currentToken => _authBox.get('token');

  // Product APIs
  Future<List<Product>> getProducts({bool availableOnly = false}) async {
    final shopId = currentShopId;
    if (shopId == null) throw Exception('No shop selected');
    
    final response = await _dio.get('/products', queryParameters: {
      'shopId': shopId,
      'available': availableOnly,
    });
    final data = _extractData(response) as List;
    return data.map((e) => Product.fromJson(e)).toList();
  }

  Future<Product> getProduct(String productId) async {
    final response = await _dio.get('/products/$productId');
    final data = _extractData(response);
    return Product.fromJson(data);
  }

  Future<Product> createProduct(CreateProductRequest request) async {
    final response = await _dio.post('/products', data: request.toJson());
    final data = _extractData(response);
    return Product.fromJson(data);
  }

  Future<Product> updateProduct(String productId, UpdateProductRequest request) async {
    final response = await _dio.put('/products/$productId', data: request.toJson());
    final data = _extractData(response);
    return Product.fromJson(data);
  }

  Future<void> deleteProduct(String productId) async {
    await _dio.delete('/products/$productId');
  }

  Future<Product> toggleProductAvailability(String productId) async {
    final response = await _dio.put('/products/$productId/toggle');
    final data = _extractData(response);
    return Product.fromJson(data);
  }

  // Order APIs
  Future<OrderListResponse> getOrders({
    String? status,
    int page = 1,
    int pageSize = 20,
  }) async {
    final shopId = currentShopId;
    if (shopId == null) throw Exception('No shop selected');
    
    final queryParams = <String, dynamic>{
      'shopId': shopId,
      'page': page,
      'pageSize': pageSize,
    };
    if (status != null) {
      queryParams['status'] = status;
    }
    
    final response = await _dio.get('/orders', queryParameters: queryParams);
    final data = _extractData(response);
    return OrderListResponse.fromJson(data);
  }

  Future<Order> getOrder(String orderId) async {
    final response = await _dio.get('/orders/$orderId');
    final data = _extractData(response);
    return Order.fromJson(data);
  }

  Future<Order> updateOrderStatus(String orderId, String status) async {
    final response = await _dio.put('/orders/$orderId/status', data: {
      'status': status,
    });
    final data = _extractData(response);
    return Order.fromJson(data);
  }

  Future<OrderStats> getOrderStats() async {
    final shopId = currentShopId;
    if (shopId == null) throw Exception('No shop selected');
    
    final response = await _dio.get('/orders/stats', queryParameters: {
      'shopId': shopId,
    });
    final data = _extractData(response);
    return OrderStats.fromJson(data);
  }

  // Shop APIs
  Future<Shop> getShop(String shopId) async {
    final response = await _dio.get('/shops/$shopId');
    final data = _extractData(response);
    return Shop.fromJson(data);
  }

  Future<Shop> updateShop(String shopId, Map<String, dynamic> request) async {
    final response = await _dio.put('/shops/$shopId', data: request);
    final data = _extractData(response);
    return Shop.fromJson(data);
  }

  dynamic _extractData(Response response) {
    final body = response.data as Map<String, dynamic>;
    if (body['success'] == true) {
      return body['data'];
    } else {
      throw Exception(body['error'] ?? 'Unknown error');
    }
  }
}
