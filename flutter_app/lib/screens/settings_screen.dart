import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../config/routes.dart';
import '../config/theme.dart';
import '../providers/auth_provider.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authStateProvider);
    final shop = authState.shop;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: ListView(
        children: [
          // Shop Info Card
          if (shop != null)
            Container(
              padding: const EdgeInsets.all(20),
              color: AppColors.primary.withOpacity(0.1),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 40,
                    backgroundColor: AppColors.primary,
                    child: Text(
                      shop.name.substring(0, 1).toUpperCase(),
                      style: const TextStyle(
                        fontSize: 32,
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    shop.name,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  Text(
                    shop.ownerPhone,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  const SizedBox(height: 8),
                  Chip(
                    label: Text(
                      shop.subscriptionPlan,
                      style: const TextStyle(color: Colors.white),
                    ),
                    backgroundColor: AppColors.primary,
                  ),
                ],
              ),
            ),

          const SizedBox(height: 16),

          // Settings List
          _SettingsTile(
            icon: Icons.store,
            title: 'Shop Details',
            subtitle: 'Update shop name, address',
            onTap: () {
              // TODO: Navigate to shop edit screen
            },
          ),
          _SettingsTile(
            icon: Icons.notifications,
            title: 'Notifications',
            subtitle: 'Configure order notifications',
            onTap: () {
              // TODO: Navigate to notifications screen
            },
          ),
          _SettingsTile(
            icon: Icons.payment,
            title: 'Payment Settings',
            subtitle: 'Configure payment methods',
            onTap: () {
              // TODO: Navigate to payment settings
            },
          ),
          _SettingsTile(
            icon: Icons.qr_code,
            title: 'WhatsApp QR Code',
            subtitle: 'Share with customers',
            onTap: () {
              // TODO: Show QR code
            },
          ),
          
          const Divider(),
          
          _SettingsTile(
            icon: Icons.help,
            title: 'Help & Support',
            subtitle: 'FAQ, contact us',
            onTap: () {
              // TODO: Navigate to help screen
            },
          ),
          _SettingsTile(
            icon: Icons.info,
            title: 'About',
            subtitle: 'Version 1.0.0',
            onTap: () {
              showAboutDialog(
                context: context,
                applicationName: 'WA Shop',
                applicationVersion: '1.0.0',
                applicationLegalese: '© 2024 WA Shop',
              );
            },
          ),
          
          const Divider(),
          
          _SettingsTile(
            icon: Icons.logout,
            title: 'Logout',
            subtitle: 'Sign out of your account',
            textColor: AppColors.error,
            onTap: () => _showLogoutDialog(context, ref),
          ),
        ],
      ),
    );
  }

  void _showLogoutDialog(BuildContext context, WidgetRef ref) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Logout'),
        content: const Text('Are you sure you want to logout?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              ref.read(authStateProvider.notifier).logout();
              Navigator.of(context).pushNamedAndRemoveUntil(
                AppRoutes.login,
                (route) => false,
              );
            },
            style: TextButton.styleFrom(foregroundColor: AppColors.error),
            child: const Text('Logout'),
          ),
        ],
      ),
    );
  }
}

class _SettingsTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final Color? textColor;

  const _SettingsTile({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
    this.textColor,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon, color: textColor ?? AppColors.textSecondary),
      title: Text(
        title,
        style: TextStyle(color: textColor),
      ),
      subtitle: Text(subtitle),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    );
  }
}
