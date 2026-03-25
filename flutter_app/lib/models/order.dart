class Order {
  final String id;
  final String shopId;
  final String customerPhone;
  final String? customerName;
  final String? deliveryAddress;
  final double subtotal;
  final double deliveryCharge;
  final double discount;
  final double total;
  final String status;
  final String paymentStatus;
  final String? paymentMethod;
  final String? notes;
  final List<OrderItem> items;
  final String? createdAt;

  Order({
    required this.id,
    required this.shopId,
    required this.customerPhone,
    this.customerName,
    this.deliveryAddress,
    required this.subtotal,
    this.deliveryCharge = 0,
    this.discount = 0,
    required this.total,
    this.status = 'PENDING',
    this.paymentStatus = 'UNPAID',
    this.paymentMethod,
    this.notes,
    this.items = const [],
    this.createdAt,
  });

  factory Order.fromJson(Map<String, dynamic> json) {
    return Order(
      id: json['id'] as String,
      shopId: json['shopId'] as String,
      customerPhone: json['customerPhone'] as String,
      customerName: json['customerName'] as String?,
      deliveryAddress: json['deliveryAddress'] as String?,
      subtotal: (json['subtotal'] as num).toDouble(),
      deliveryCharge: (json['deliveryCharge'] as num?)?.toDouble() ?? 0,
      discount: (json['discount'] as num?)?.toDouble() ?? 0,
      total: (json['total'] as num).toDouble(),
      status: json['status'] as String? ?? 'PENDING',
      paymentStatus: json['paymentStatus'] as String? ?? 'UNPAID',
      paymentMethod: json['paymentMethod'] as String?,
      notes: json['notes'] as String?,
      items: (json['items'] as List<dynamic>?)
              ?.map((e) => OrderItem.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
      createdAt: json['createdAt'] as String?,
    );
  }

  String get statusDisplay {
    switch (status) {
      case 'PENDING':
        return 'Pending';
      case 'ACCEPTED':
        return 'Accepted';
      case 'REJECTED':
        return 'Rejected';
      case 'PREPARING':
        return 'Preparing';
      case 'OUT_FOR_DELIVERY':
        return 'Out for Delivery';
      case 'DELIVERED':
        return 'Delivered';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  String get paymentStatusDisplay {
    switch (paymentStatus) {
      case 'UNPAID':
        return 'Unpaid';
      case 'PAID':
        return 'Paid';
      case 'REFUNDED':
        return 'Refunded';
      case 'COD':
        return 'Cash on Delivery';
      default:
        return paymentStatus;
    }
  }

  bool get isPending => status == 'PENDING';
  bool get isAccepted => status == 'ACCEPTED';
  bool get isDelivered => status == 'DELIVERED';
  bool get isCancelled => status == 'CANCELLED' || status == 'REJECTED';
}

class OrderItem {
  final int id;
  final String? productId;
  final String productName;
  final double quantity;
  final double unitPrice;
  final double totalPrice;

  OrderItem({
    required this.id,
    this.productId,
    required this.productName,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
  });

  factory OrderItem.fromJson(Map<String, dynamic> json) {
    // Handle quantity as string or number
    double qty;
    if (json['quantity'] is String) {
      qty = double.parse(json['quantity'] as String);
    } else {
      qty = (json['quantity'] as num).toDouble();
    }
    
    return OrderItem(
      id: json['id'] as int,
      productId: json['productId'] as String?,
      productName: json['productName'] as String,
      quantity: qty,
      unitPrice: (json['unitPrice'] as num).toDouble(),
      totalPrice: (json['totalPrice'] as num).toDouble(),
    );
  }
  
  String get quantityDisplay {
    if (quantity < 1) {
      return '${(quantity * 1000).toInt()}g';
    } else if (quantity == quantity.toInt()) {
      return '${quantity.toInt()}';
    } else {
      return '${quantity}kg';
    }
  }
}

class OrderListResponse {
  final List<Order> orders;
  final int total;
  final int page;
  final int pageSize;

  OrderListResponse({
    required this.orders,
    required this.total,
    required this.page,
    required this.pageSize,
  });

  factory OrderListResponse.fromJson(Map<String, dynamic> json) {
    return OrderListResponse(
      orders: (json['orders'] as List<dynamic>)
          .map((e) => Order.fromJson(e as Map<String, dynamic>))
          .toList(),
      total: json['total'] as int,
      page: json['page'] as int,
      pageSize: json['pageSize'] as int,
    );
  }
}

class OrderStats {
  final int totalOrders;
  final int todayOrders;
  final int pendingOrders;
  final double totalRevenue;

  OrderStats({
    required this.totalOrders,
    required this.todayOrders,
    required this.pendingOrders,
    required this.totalRevenue,
  });

  factory OrderStats.fromJson(Map<String, dynamic> json) {
    return OrderStats(
      totalOrders: json['totalOrders'] as int,
      todayOrders: json['todayOrders'] as int,
      pendingOrders: json['pendingOrders'] as int,
      totalRevenue: (json['totalRevenue'] as num).toDouble(),
    );
  }
}
