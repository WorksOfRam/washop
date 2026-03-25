import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:url_launcher/url_launcher.dart';
import '../config/theme.dart';
import '../models/order.dart';
import '../providers/auth_provider.dart';
import '../providers/order_provider.dart';

class OrderDetailScreen extends ConsumerStatefulWidget {
  final String orderId;

  const OrderDetailScreen({super.key, required this.orderId});

  @override
  ConsumerState<OrderDetailScreen> createState() => _OrderDetailScreenState();
}

class _OrderDetailScreenState extends ConsumerState<OrderDetailScreen> {
  Order? _order;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadOrder();
  }

  Future<void> _loadOrder() async {
    setState(() => _isLoading = true);
    try {
      final order = await ref.read(apiServiceProvider).getOrder(widget.orderId);
      setState(() {
        _order = order;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to load order: $e')),
        );
      }
    }
  }

  Future<void> _updateStatus(String status) async {
    final success = await ref.read(ordersProvider.notifier)
        .updateOrderStatus(widget.orderId, status);
    
    if (success) {
      _loadOrder();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Order status updated')),
        );
      }
    }
  }

  void _callCustomer() async {
    if (_order == null) return;
    final url = Uri.parse('tel:+91${_order!.customerPhone}');
    if (await canLaunchUrl(url)) {
      await launchUrl(url);
    }
  }

  void _whatsappCustomer() async {
    if (_order == null) return;
    final url = Uri.parse('https://wa.me/91${_order!.customerPhone}');
    if (await canLaunchUrl(url)) {
      await launchUrl(url);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_order?.id ?? 'Order Details'),
        actions: [
          if (_order != null) ...[
            IconButton(
              icon: const Icon(Icons.phone),
              onPressed: _callCustomer,
            ),
            IconButton(
              icon: const Icon(Icons.message),
              onPressed: _whatsappCustomer,
            ),
          ],
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _order == null
              ? const Center(child: Text('Order not found'))
              : _buildOrderDetails(),
      bottomNavigationBar: _order != null && _order!.isPending
          ? _buildActionBar()
          : null,
    );
  }

  Widget _buildOrderDetails() {
    final order = _order!;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Status Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: [
                  CircleAvatar(
                    radius: 24,
                    backgroundColor: _getStatusColor(order.status).withOpacity(0.1),
                    child: Icon(
                      _getStatusIcon(order.status),
                      color: _getStatusColor(order.status),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          order.statusDisplay,
                          style: Theme.of(context).textTheme.titleLarge?.copyWith(
                            color: _getStatusColor(order.status),
                          ),
                        ),
                        if (order.createdAt != null)
                          Text(
                            order.createdAt!,
                            style: Theme.of(context).textTheme.bodyMedium,
                          ),
                      ],
                    ),
                  ),
                  Chip(
                    label: Text(
                      order.paymentStatusDisplay,
                      style: TextStyle(
                        color: order.paymentStatus == 'PAID'
                            ? AppColors.success
                            : AppColors.warning,
                      ),
                    ),
                    backgroundColor: (order.paymentStatus == 'PAID'
                            ? AppColors.success
                            : AppColors.warning)
                        .withOpacity(0.1),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Customer Info
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Customer',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      const Icon(Icons.phone, size: 20, color: AppColors.textSecondary),
                      const SizedBox(width: 8),
                      Text(
                        '+91 ${order.customerPhone}',
                        style: Theme.of(context).textTheme.bodyLarge,
                      ),
                    ],
                  ),
                  if (order.customerName != null) ...[
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        const Icon(Icons.person, size: 20, color: AppColors.textSecondary),
                        const SizedBox(width: 8),
                        Text(order.customerName!),
                      ],
                    ),
                  ],
                  if (order.deliveryAddress != null) ...[
                    const SizedBox(height: 8),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.location_on, size: 20, color: AppColors.textSecondary),
                        const SizedBox(width: 8),
                        Expanded(child: Text(order.deliveryAddress!)),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Order Items
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Items',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 12),
                  ...order.items.map((item) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    child: Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                item.productName,
                                style: Theme.of(context).textTheme.bodyLarge,
                              ),
                              Text(
                                '₹${item.unitPrice.toInt()} x ${item.quantityDisplay}',
                                style: Theme.of(context).textTheme.bodyMedium,
                              ),
                            ],
                          ),
                        ),
                        Text(
                          '₹${item.totalPrice.toInt()}',
                          style: Theme.of(context).textTheme.titleMedium,
                        ),
                      ],
                    ),
                  )),
                  const Divider(),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Total',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      Text(
                        '₹${order.total.toInt()}',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),

          if (order.notes != null) ...[
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Notes',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    Text(order.notes!),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildActionBar() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surface,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 10,
            offset: const Offset(0, -5),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton(
              onPressed: () => _updateStatus('REJECTED'),
              style: OutlinedButton.styleFrom(
                foregroundColor: AppColors.error,
                side: const BorderSide(color: AppColors.error),
              ),
              child: const Text('Reject'),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            flex: 2,
            child: ElevatedButton(
              onPressed: () => _updateStatus('ACCEPTED'),
              child: const Text('Accept Order'),
            ),
          ),
        ],
      ),
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
