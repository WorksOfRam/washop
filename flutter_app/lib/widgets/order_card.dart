import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../config/theme.dart';
import '../models/order.dart';

class OrderCard extends StatelessWidget {
  final Order order;
  final VoidCallback? onTap;
  final Function(String)? onStatusUpdate;

  const OrderCard({
    super.key,
    required this.order,
    this.onTap,
    this.onStatusUpdate,
  });

  String _formatDate(String isoDate) {
    try {
      final date = DateTime.parse(isoDate);
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      final orderDate = DateTime(date.year, date.month, date.day);
      
      if (orderDate == today) {
        return 'Today ${DateFormat('h:mm a').format(date)}';
      } else if (orderDate == today.subtract(const Duration(days: 1))) {
        return 'Yesterday ${DateFormat('h:mm a').format(date)}';
      }
      return DateFormat('dd MMM, h:mm a').format(date);
    } catch (e) {
      return isoDate;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    order.id,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  _StatusChip(status: order.status),
                ],
              ),
              const SizedBox(height: 8),

              // Customer Info
              Row(
                children: [
                  const Icon(Icons.phone, size: 16, color: AppColors.textSecondary),
                  const SizedBox(width: 4),
                  Text(
                    order.customerPhone,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const Spacer(),
                  if (order.createdAt != null)
                    Text(
                      _formatDate(order.createdAt!),
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: AppColors.textLight,
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 8),

              // Items Summary
              Text(
                '${order.items.length} item(s)',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 8),

              // Footer
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: [
                      _PaymentChip(status: order.paymentStatus),
                    ],
                  ),
                  Text(
                    '₹${order.total.toInt()}',
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),

              // Quick Actions for Pending Orders
              if (order.isPending && onStatusUpdate != null) ...[
                const SizedBox(height: 12),
                const Divider(),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: () => onStatusUpdate?.call('REJECTED'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.error,
                          side: const BorderSide(color: AppColors.error),
                        ),
                        child: const Text('Reject'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      flex: 2,
                      child: ElevatedButton(
                        onPressed: () => onStatusUpdate?.call('ACCEPTED'),
                        child: const Text('Accept'),
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  final String status;

  const _StatusChip({required this.status});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: _getColor().withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        _getLabel(),
        style: TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: _getColor(),
        ),
      ),
    );
  }

  Color _getColor() {
    switch (status) {
      case 'PENDING':
        return AppColors.warning;
      case 'ACCEPTED':
        return AppColors.accent;
      case 'PREPARING':
        return AppColors.primary;
      case 'OUT_FOR_DELIVERY':
        return AppColors.primaryDark;
      case 'DELIVERED':
        return AppColors.success;
      case 'REJECTED':
      case 'CANCELLED':
        return AppColors.error;
      default:
        return AppColors.textSecondary;
    }
  }

  String _getLabel() {
    switch (status) {
      case 'PENDING':
        return 'Pending';
      case 'ACCEPTED':
        return 'Accepted';
      case 'PREPARING':
        return 'Preparing';
      case 'OUT_FOR_DELIVERY':
        return 'Out for Delivery';
      case 'DELIVERED':
        return 'Delivered';
      case 'REJECTED':
        return 'Rejected';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }
}

class _PaymentChip extends StatelessWidget {
  final String status;

  const _PaymentChip({required this.status});

  @override
  Widget build(BuildContext context) {
    final isPaid = status == 'PAID';
    final color = isPaid ? AppColors.success : AppColors.warning;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            isPaid ? Icons.check_circle : Icons.pending,
            size: 14,
            color: color,
          ),
          const SizedBox(width: 4),
          Text(
            isPaid ? 'Paid' : 'Unpaid',
            style: TextStyle(
              fontSize: 12,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
