import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/order.dart';
import 'auth_provider.dart';

final ordersProvider = StateNotifierProvider<OrdersNotifier, OrdersState>((ref) {
  return OrdersNotifier(ref.read(apiServiceProvider));
});

final orderStatsProvider = FutureProvider<OrderStats>((ref) async {
  final apiService = ref.read(apiServiceProvider);
  return await apiService.getOrderStats();
});

class OrdersState {
  final bool isLoading;
  final List<Order> orders;
  final int total;
  final int page;
  final String? error;
  final String? filterStatus;

  OrdersState({
    this.isLoading = false,
    this.orders = const [],
    this.total = 0,
    this.page = 1,
    this.error,
    this.filterStatus,
  });

  OrdersState copyWith({
    bool? isLoading,
    List<Order>? orders,
    int? total,
    int? page,
    String? error,
    String? filterStatus,
    bool clearFilter = false,
  }) {
    return OrdersState(
      isLoading: isLoading ?? this.isLoading,
      orders: orders ?? this.orders,
      total: total ?? this.total,
      page: page ?? this.page,
      error: error,
      filterStatus: clearFilter ? null : (filterStatus ?? this.filterStatus),
    );
  }

  int get pendingCount => orders.where((o) => o.status == 'PENDING').length;
  bool get hasMore => orders.length < total;
}

class OrdersNotifier extends StateNotifier<OrdersState> {
  final _apiService;

  OrdersNotifier(this._apiService) : super(OrdersState());

  Future<void> loadOrders({String? status, bool refresh = false}) async {
    // Determine the effective filter status
    final effectiveStatus = refresh ? status : (status ?? state.filterStatus);
    
    if (refresh) {
      state = state.copyWith(
        page: 1, 
        orders: [], 
        filterStatus: status,
        clearFilter: status == null,
      );
    }
    
    state = state.copyWith(isLoading: true, error: null);
    
    try {
      final response = await _apiService.getOrders(
        status: effectiveStatus,
        page: refresh ? 1 : state.page,
      );
      
      state = state.copyWith(
        isLoading: false,
        orders: refresh ? response.orders : [...state.orders, ...response.orders],
        total: response.total,
        page: response.page,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
      );
    }
  }

  Future<void> loadMore() async {
    if (state.isLoading || !state.hasMore) return;
    
    state = state.copyWith(page: state.page + 1);
    await loadOrders();
  }

  Future<bool> updateOrderStatus(String orderId, String status) async {
    try {
      final order = await _apiService.updateOrderStatus(orderId, status);
      state = state.copyWith(
        orders: state.orders.map<Order>((o) => o.id == orderId ? order : o).toList(),
      );
      return true;
    } catch (e) {
      state = state.copyWith(error: e.toString());
      return false;
    }
  }

  void setFilter(String? status) {
    loadOrders(status: status, refresh: true);
  }
}
