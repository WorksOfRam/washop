import 'package:flutter/material.dart';
import '../screens/splash_screen.dart';
import '../screens/login_screen.dart';
import '../screens/home_screen.dart';
import '../screens/products_screen.dart';
import '../screens/add_product_screen.dart';
import '../screens/orders_screen.dart';
import '../screens/order_detail_screen.dart';
import '../screens/settings_screen.dart';

class AppRoutes {
  static const splash = '/';
  static const login = '/login';
  static const home = '/home';
  static const products = '/products';
  static const addProduct = '/products/add';
  static const editProduct = '/products/edit';
  static const orders = '/orders';
  static const orderDetail = '/orders/detail';
  static const settings = '/settings';

  static Route<dynamic> generateRoute(RouteSettings settings) {
    switch (settings.name) {
      case splash:
        return MaterialPageRoute(builder: (_) => const SplashScreen());
      
      case login:
        return MaterialPageRoute(builder: (_) => const LoginScreen());
      
      case home:
        return MaterialPageRoute(builder: (_) => const HomeScreen());
      
      case products:
        return MaterialPageRoute(builder: (_) => const ProductsScreen());
      
      case addProduct:
        return MaterialPageRoute(builder: (_) => const AddProductScreen());
      
      case editProduct:
        final productId = settings.arguments as String?;
        return MaterialPageRoute(
          builder: (_) => AddProductScreen(productId: productId),
        );
      
      case orders:
        return MaterialPageRoute(builder: (_) => const OrdersScreen());
      
      case orderDetail:
        final orderId = settings.arguments as String;
        return MaterialPageRoute(
          builder: (_) => OrderDetailScreen(orderId: orderId),
        );
      
      case AppRoutes.settings:
        return MaterialPageRoute(builder: (_) => const SettingsScreen());
      
      default:
        return MaterialPageRoute(
          builder: (_) => Scaffold(
            body: Center(
              child: Text('No route defined for ${settings.name}'),
            ),
          ),
        );
    }
  }
}
