import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/product.dart';
import 'auth_provider.dart';

final productsProvider = StateNotifierProvider<ProductsNotifier, ProductsState>((ref) {
  return ProductsNotifier(ref.read(apiServiceProvider));
});

class ProductsState {
  final bool isLoading;
  final List<Product> products;
  final String? error;

  ProductsState({
    this.isLoading = false,
    this.products = const [],
    this.error,
  });

  ProductsState copyWith({
    bool? isLoading,
    List<Product>? products,
    String? error,
  }) {
    return ProductsState(
      isLoading: isLoading ?? this.isLoading,
      products: products ?? this.products,
      error: error,
    );
  }

  int get availableCount => products.where((p) => p.isAvailable).length;
  int get unavailableCount => products.where((p) => !p.isAvailable).length;
}

class ProductsNotifier extends StateNotifier<ProductsState> {
  final _apiService;

  ProductsNotifier(this._apiService) : super(ProductsState());

  Future<void> loadProducts() async {
    state = state.copyWith(isLoading: true, error: null);
    
    try {
      final products = await _apiService.getProducts();
      state = state.copyWith(
        isLoading: false,
        products: products,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
    }
  }

  Future<bool> createProduct(CreateProductRequest request) async {
    try {
      final product = await _apiService.createProduct(request);
      state = state.copyWith(
        products: [...state.products, product],
      );
      return true;
    } catch (e) {
      state = state.copyWith(error: e.toString());
      return false;
    }
  }

  Future<bool> updateProduct(String productId, UpdateProductRequest request) async {
    try {
      final product = await _apiService.updateProduct(productId, request);
      state = state.copyWith(
        products: state.products.map<Product>((p) => p.id == productId ? product : p).toList(),
      );
      return true;
    } catch (e) {
      state = state.copyWith(error: e.toString());
      return false;
    }
  }

  Future<bool> deleteProduct(String productId) async {
    try {
      await _apiService.deleteProduct(productId);
      state = state.copyWith(
        products: state.products.where((p) => p.id != productId).toList(),
      );
      return true;
    } catch (e) {
      state = state.copyWith(error: e.toString());
      return false;
    }
  }

  Future<bool> toggleAvailability(String productId) async {
    try {
      final product = await _apiService.toggleProductAvailability(productId);
      state = state.copyWith(
        products: state.products.map<Product>((p) => p.id == productId ? product : p).toList(),
      );
      return true;
    } catch (e) {
      state = state.copyWith(error: e.toString());
      return false;
    }
  }
}
