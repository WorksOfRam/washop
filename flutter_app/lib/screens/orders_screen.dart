import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../config/routes.dart';
import '../config/theme.dart';
import '../providers/order_provider.dart';
import '../widgets/order_card.dart';

class OrdersScreen extends ConsumerStatefulWidget {
  const OrdersScreen({super.key});

  @override
  ConsumerState<OrdersScreen> createState() => _OrdersScreenState();
}

class _OrdersScreenState extends ConsumerState<OrdersScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(() {
      ref.read(ordersProvider.notifier).loadOrders(refresh: true);
    });
  }

  @override
  Widget build(BuildContext context) {
    final ordersState = ref.watch(ordersProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Orders'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.read(ordersProvider.notifier).loadOrders(refresh: true),
          ),
        ],
      ),
      body: Column(
        children: [
          _buildFilterTabs(ordersState),
          const Divider(height: 1),
          Expanded(
            child: ordersState.isLoading && ordersState.orders.isEmpty
                ? const Center(child: CircularProgressIndicator())
                : ordersState.orders.isEmpty
                    ? _buildEmptyState()
                    : _buildOrderList(ordersState),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterTabs(OrdersState state) {
    final filters = [
      {'label': 'All', 'value': null},
      {'label': 'Pending', 'value': 'PENDING'},
      {'label': 'Accepted', 'value': 'ACCEPTED'},
      {'label': 'Delivered', 'value': 'DELIVERED'},
    ];

    return Container(
      height: 50,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: filters.length,
        separatorBuilder: (_, __) => const SizedBox(width: 8),
        itemBuilder: (context, index) {
          final filter = filters[index];
          final isSelected = state.filterStatus == filter['value'];
          
          return FilterChip(
            label: Text(filter['label'] as String),
            selected: isSelected,
            onSelected: (_) {
              ref.read(ordersProvider.notifier).setFilter(filter['value'] as String?);
            },
            selectedColor: AppColors.primary.withOpacity(0.2),
            checkmarkColor: AppColors.primary,
          );
        },
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.receipt_long,
            size: 80,
            color: AppColors.textLight,
          ),
          const SizedBox(height: 16),
          Text(
            'No orders yet',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: 8),
          Text(
            'Orders from WhatsApp will appear here',
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ],
      ),
    );
  }

  Widget _buildOrderList(OrdersState state) {
    return RefreshIndicator(
      onRefresh: () async {
        await ref.read(ordersProvider.notifier).loadOrders(refresh: true);
      },
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: state.orders.length + (state.hasMore ? 1 : 0),
        itemBuilder: (context, index) {
          if (index == state.orders.length) {
            ref.read(ordersProvider.notifier).loadMore();
            return const Center(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: CircularProgressIndicator(),
              ),
            );
          }

          final order = state.orders[index];
          return OrderCard(
            order: order,
            onTap: () => Navigator.pushNamed(
              context,
              AppRoutes.orderDetail,
              arguments: order.id,
            ),
            onStatusUpdate: (status) {
              ref.read(ordersProvider.notifier).updateOrderStatus(order.id, status);
            },
          );
        },
      ),
    );
  }
}
