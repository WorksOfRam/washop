import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../config/routes.dart';
import '../config/theme.dart';
import '../providers/auth_provider.dart';
import '../providers/order_provider.dart';
import '../providers/product_provider.dart';
import '../widgets/stats_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  int _currentIndex = 0;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  void _loadData() {
    ref.read(productsProvider.notifier).loadProducts();
    ref.read(ordersProvider.notifier).loadOrders(refresh: true);
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authStateProvider);
    final productsState = ref.watch(productsProvider);
    final ordersState = ref.watch(ordersProvider);
    final statsAsync = ref.watch(orderStatsProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(authState.shop?.name ?? 'WA Shop'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadData,
          ),
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () => Navigator.pushNamed(context, AppRoutes.settings),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async => _loadData(),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Stats Cards
              statsAsync.when(
                data: (stats) => _buildStatsSection(stats),
                loading: () => const Center(child: CircularProgressIndicator()),
                error: (e, _) => _buildStatsSection(null),
              ),
              const SizedBox(height: 24),

              // Quick Actions
              Text(
                'Quick Actions',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 12),
              _buildQuickActions(),
              const SizedBox(height: 24),

              // Recent Orders
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Recent Orders',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  TextButton(
                    onPressed: () => Navigator.pushNamed(context, AppRoutes.orders),
                    child: const Text('View All'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _buildRecentOrders(ordersState),
            ],
          ),
        ),
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          if (index == 1) {
            Navigator.pushNamed(context, AppRoutes.products);
          } else if (index == 2) {
            Navigator.pushNamed(context, AppRoutes.orders);
          }
        },
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home),
            label: 'Home',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.inventory),
            label: 'Products',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.receipt_long),
            label: 'Orders',
          ),
        ],
      ),
    );
  }

  Widget _buildStatsSection(dynamic stats) {
    final currencyFormat = NumberFormat.currency(locale: 'en_IN', symbol: '₹');
    
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 12,
      mainAxisSpacing: 12,
      childAspectRatio: 1.5,
      children: [
        StatsCard(
          title: 'Today Orders',
          value: '${stats?.todayOrders ?? 0}',
          icon: Icons.today,
          color: AppColors.primary,
        ),
        StatsCard(
          title: 'Pending',
          value: '${stats?.pendingOrders ?? 0}',
          icon: Icons.pending_actions,
          color: AppColors.warning,
        ),
        StatsCard(
          title: 'Total Orders',
          value: '${stats?.totalOrders ?? 0}',
          icon: Icons.shopping_bag,
          color: AppColors.accent,
        ),
        StatsCard(
          title: 'Revenue',
          value: currencyFormat.format(stats?.totalRevenue ?? 0),
          icon: Icons.currency_rupee,
          color: AppColors.success,
          fontSize: 16,
        ),
      ],
    );
  }

  Widget _buildQuickActions() {
    return Row(
      children: [
        Expanded(
          child: _QuickActionButton(
            icon: Icons.add_box,
            label: 'Add Product',
            onTap: () => Navigator.pushNamed(context, AppRoutes.addProduct),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _QuickActionButton(
            icon: Icons.inventory_2,
            label: 'Products',
            onTap: () => Navigator.pushNamed(context, AppRoutes.products),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _QuickActionButton(
            icon: Icons.receipt,
            label: 'Orders',
            onTap: () => Navigator.pushNamed(context, AppRoutes.orders),
          ),
        ),
      ],
    );
  }

  Widget _buildRecentOrders(OrdersState ordersState) {
    if (ordersState.isLoading && ordersState.orders.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }

    if (ordersState.orders.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Icon(
                Icons.receipt_long,
                size: 48,
                color: AppColors.textLight,
              ),
              const SizedBox(height: 12),
              Text(
                'No orders yet',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      children: ordersState.orders.take(5).map((order) {
        return Card(
          margin: const EdgeInsets.only(bottom: 8),
          child: ListTile(
            leading: CircleAvatar(
              backgroundColor: _getStatusColor(order.status).withOpacity(0.1),
              child: Icon(
                _getStatusIcon(order.status),
                color: _getStatusColor(order.status),
              ),
            ),
            title: Text(order.id),
            subtitle: Text(
              '${order.customerPhone} • ₹${order.total.toInt()}',
            ),
            trailing: Chip(
              label: Text(
                order.statusDisplay,
                style: TextStyle(
                  color: _getStatusColor(order.status),
                  fontSize: 12,
                ),
              ),
              backgroundColor: _getStatusColor(order.status).withOpacity(0.1),
            ),
            onTap: () => Navigator.pushNamed(
              context,
              AppRoutes.orderDetail,
              arguments: order.id,
            ),
          ),
        );
      }).toList(),
    );
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'PENDING':
        return AppColors.warning;
      case 'ACCEPTED':
        return AppColors.accent;
      case 'PREPARING':
        return AppColors.primary;
      case 'DELIVERED':
        return AppColors.success;
      case 'REJECTED':
      case 'CANCELLED':
        return AppColors.error;
      default:
        return AppColors.textSecondary;
    }
  }

  IconData _getStatusIcon(String status) {
    switch (status) {
      case 'PENDING':
        return Icons.pending;
      case 'ACCEPTED':
        return Icons.check_circle;
      case 'PREPARING':
        return Icons.restaurant;
      case 'DELIVERED':
        return Icons.done_all;
      case 'REJECTED':
      case 'CANCELLED':
        return Icons.cancel;
      default:
        return Icons.help;
    }
  }
}

class _QuickActionButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _QuickActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              Icon(icon, color: AppColors.primary, size: 28),
              const SizedBox(height: 8),
              Text(
                label,
                style: Theme.of(context).textTheme.labelLarge,
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
