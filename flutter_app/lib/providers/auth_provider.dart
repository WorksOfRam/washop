import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/shop.dart';
import '../services/api_service.dart';

export '../models/shop.dart' show User, Shop, LoginResponse;

final apiServiceProvider = Provider<ApiService>((ref) => ApiService());

final authStateProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref.read(apiServiceProvider));
});

class AuthState {
  final bool isLoading;
  final bool isLoggedIn;
  final User? user;
  final Shop? shop;
  final String? error;

  AuthState({
    this.isLoading = false,
    this.isLoggedIn = false,
    this.user,
    this.shop,
    this.error,
  });

  AuthState copyWith({
    bool? isLoading,
    bool? isLoggedIn,
    User? user,
    Shop? shop,
    String? error,
  }) {
    return AuthState(
      isLoading: isLoading ?? this.isLoading,
      isLoggedIn: isLoggedIn ?? this.isLoggedIn,
      user: user ?? this.user,
      shop: shop ?? this.shop,
      error: error,
    );
  }
}

class AuthNotifier extends StateNotifier<AuthState> {
  final ApiService _apiService;

  AuthNotifier(this._apiService) : super(AuthState());

  Future<bool> checkAuth() async {
    if (_apiService.isLoggedIn && _apiService.currentShopId != null) {
      try {
        final shop = await _apiService.getShop(_apiService.currentShopId!);
        state = state.copyWith(isLoggedIn: true, shop: shop);
        return true;
      } catch (e) {
        await _apiService.logout();
        state = AuthState();
        return false;
      }
    }
    return false;
  }

  Future<bool> login(String phone) async {
    state = state.copyWith(isLoading: true, error: null);
    
    try {
      final response = await _apiService.login(phone);
      state = state.copyWith(
        isLoading: false,
        isLoggedIn: true,
        user: response.user,
        shop: response.shop,
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
      return false;
    }
  }

  Future<bool> register({
    required String phone,
    required String shopName,
    String? ownerName,
    String? address,
  }) async {
    state = state.copyWith(isLoading: true, error: null);
    
    try {
      final response = await _apiService.register(
        phone: phone,
        shopName: shopName,
        ownerName: ownerName,
        address: address,
      );
      state = state.copyWith(
        isLoading: false,
        isLoggedIn: true,
        user: response.user,
        shop: response.shop,
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
      return false;
    }
  }

  Future<void> logout() async {
    await _apiService.logout();
    state = AuthState();
  }
}
