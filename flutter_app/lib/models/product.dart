class Product {
  final String id;
  final String shopId;
  final String name;
  final String? description;
  final double price;
  final String unit;
  final String? imageUrl;
  final String? category;
  final int stock;
  final bool isAvailable;
  final int displayOrder;

  Product({
    required this.id,
    required this.shopId,
    required this.name,
    this.description,
    required this.price,
    this.unit = 'piece',
    this.imageUrl,
    this.category,
    this.stock = -1,
    this.isAvailable = true,
    this.displayOrder = 0,
  });

  factory Product.fromJson(Map<String, dynamic> json) {
    return Product(
      id: json['id'] as String,
      shopId: json['shopId'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      price: (json['price'] as num).toDouble(),
      unit: json['unit'] as String? ?? 'piece',
      imageUrl: json['imageUrl'] as String?,
      category: json['category'] as String?,
      stock: json['stock'] as int? ?? -1,
      isAvailable: json['isAvailable'] as bool? ?? true,
      displayOrder: json['displayOrder'] as int? ?? 0,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'shopId': shopId,
      'name': name,
      'description': description,
      'price': price,
      'unit': unit,
      'imageUrl': imageUrl,
      'category': category,
      'stock': stock,
      'isAvailable': isAvailable,
      'displayOrder': displayOrder,
    };
  }

  Product copyWith({
    String? id,
    String? shopId,
    String? name,
    String? description,
    double? price,
    String? unit,
    String? imageUrl,
    String? category,
    int? stock,
    bool? isAvailable,
    int? displayOrder,
  }) {
    return Product(
      id: id ?? this.id,
      shopId: shopId ?? this.shopId,
      name: name ?? this.name,
      description: description ?? this.description,
      price: price ?? this.price,
      unit: unit ?? this.unit,
      imageUrl: imageUrl ?? this.imageUrl,
      category: category ?? this.category,
      stock: stock ?? this.stock,
      isAvailable: isAvailable ?? this.isAvailable,
      displayOrder: displayOrder ?? this.displayOrder,
    );
  }
}

class CreateProductRequest {
  final String shopId;
  final String name;
  final String? description;
  final double price;
  final String unit;
  final String? imageUrl;
  final String? category;
  final int stock;

  CreateProductRequest({
    required this.shopId,
    required this.name,
    this.description,
    required this.price,
    this.unit = 'piece',
    this.imageUrl,
    this.category,
    this.stock = -1,
  });

  Map<String, dynamic> toJson() {
    return {
      'shopId': shopId,
      'name': name,
      'description': description,
      'price': price,
      'unit': unit,
      'imageUrl': imageUrl,
      'category': category,
      'stock': stock,
    };
  }
}

class UpdateProductRequest {
  final String? name;
  final String? description;
  final double? price;
  final String? unit;
  final String? imageUrl;
  final String? category;
  final int? stock;
  final bool? isAvailable;

  UpdateProductRequest({
    this.name,
    this.description,
    this.price,
    this.unit,
    this.imageUrl,
    this.category,
    this.stock,
    this.isAvailable,
  });

  Map<String, dynamic> toJson() {
    final map = <String, dynamic>{};
    if (name != null) map['name'] = name;
    if (description != null) map['description'] = description;
    if (price != null) map['price'] = price;
    if (unit != null) map['unit'] = unit;
    if (imageUrl != null) map['imageUrl'] = imageUrl;
    if (category != null) map['category'] = category;
    if (stock != null) map['stock'] = stock;
    if (isAvailable != null) map['isAvailable'] = isAvailable;
    return map;
  }
}
