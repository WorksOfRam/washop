class Shop {
  final String id;
  final String name;
  final String ownerPhone;
  final String? ownerName;
  final String? address;
  final String city;
  final String? whatsappNumber;
  final bool isActive;
  final String subscriptionPlan;

  Shop({
    required this.id,
    required this.name,
    required this.ownerPhone,
    this.ownerName,
    this.address,
    this.city = 'Vizag',
    this.whatsappNumber,
    this.isActive = true,
    this.subscriptionPlan = 'FREE',
  });

  factory Shop.fromJson(Map<String, dynamic> json) {
    return Shop(
      id: json['id'] as String,
      name: json['name'] as String,
      ownerPhone: json['ownerPhone'] as String,
      ownerName: json['ownerName'] as String?,
      address: json['address'] as String?,
      city: json['city'] as String? ?? 'Vizag',
      whatsappNumber: json['whatsappNumber'] as String?,
      isActive: json['isActive'] as bool? ?? true,
      subscriptionPlan: json['subscriptionPlan'] as String? ?? 'FREE',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'ownerPhone': ownerPhone,
      'ownerName': ownerName,
      'address': address,
      'city': city,
      'whatsappNumber': whatsappNumber,
      'isActive': isActive,
      'subscriptionPlan': subscriptionPlan,
    };
  }
}

class User {
  final String phone;
  final String? name;
  final String role;
  final String? shopId;
  final String? defaultAddress;
  final bool isActive;

  User({
    required this.phone,
    this.name,
    this.role = 'CUSTOMER',
    this.shopId,
    this.defaultAddress,
    this.isActive = true,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      phone: json['phone'] as String,
      name: json['name'] as String?,
      role: json['role'] as String? ?? 'CUSTOMER',
      shopId: json['shopId'] as String?,
      defaultAddress: json['defaultAddress'] as String?,
      isActive: json['isActive'] as bool? ?? true,
    );
  }

  bool get isShopOwner => role == 'SHOP_OWNER';
  bool get isAdmin => role == 'ADMIN';
}

class LoginResponse {
  final String token;
  final User user;
  final Shop? shop;

  LoginResponse({
    required this.token,
    required this.user,
    this.shop,
  });

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    return LoginResponse(
      token: json['token'] as String,
      user: User.fromJson(json['user'] as Map<String, dynamic>),
      shop: json['shop'] != null
          ? Shop.fromJson(json['shop'] as Map<String, dynamic>)
          : null,
    );
  }
}
