import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../config/theme.dart';
import '../models/product.dart';
import '../providers/auth_provider.dart';
import '../providers/product_provider.dart';

class AddProductScreen extends ConsumerStatefulWidget {
  final String? productId;

  const AddProductScreen({super.key, this.productId});

  @override
  ConsumerState<AddProductScreen> createState() => _AddProductScreenState();
}

class _AddProductScreenState extends ConsumerState<AddProductScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _priceController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _categoryController = TextEditingController();
  
  String _selectedUnit = 'piece';
  bool _isLoading = false;
  Product? _existingProduct;

  final List<String> _units = ['piece', 'kg', 'g', 'litre', 'ml', 'pack', 'dozen'];

  @override
  void initState() {
    super.initState();
    if (widget.productId != null) {
      _loadProduct();
    }
  }

  void _loadProduct() {
    final products = ref.read(productsProvider).products;
    final product = products.firstWhere(
      (p) => p.id == widget.productId,
      orElse: () => throw Exception('Product not found'),
    );
    
    _existingProduct = product;
    _nameController.text = product.name;
    _priceController.text = product.price.toString();
    _descriptionController.text = product.description ?? '';
    _categoryController.text = product.category ?? '';
    _selectedUnit = product.unit;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _priceController.dispose();
    _descriptionController.dispose();
    _categoryController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    final productsNotifier = ref.read(productsProvider.notifier);
    bool success;

    if (_existingProduct != null) {
      success = await productsNotifier.updateProduct(
        _existingProduct!.id,
        UpdateProductRequest(
          name: _nameController.text,
          price: double.parse(_priceController.text),
          description: _descriptionController.text.isNotEmpty 
              ? _descriptionController.text 
              : null,
          category: _categoryController.text.isNotEmpty 
              ? _categoryController.text 
              : null,
          unit: _selectedUnit,
        ),
      );
    } else {
      final shopId = ref.read(authStateProvider).shop?.id;
      if (shopId == null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Shop not found')),
        );
        setState(() => _isLoading = false);
        return;
      }

      success = await productsNotifier.createProduct(
        CreateProductRequest(
          shopId: shopId,
          name: _nameController.text,
          price: double.parse(_priceController.text),
          description: _descriptionController.text.isNotEmpty 
              ? _descriptionController.text 
              : null,
          category: _categoryController.text.isNotEmpty 
              ? _categoryController.text 
              : null,
          unit: _selectedUnit,
        ),
      );
    }

    setState(() => _isLoading = false);

    if (success && mounted) {
      Navigator.pop(context);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            _existingProduct != null 
                ? 'Product updated successfully' 
                : 'Product added successfully',
          ),
        ),
      );
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Failed to save product')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = _existingProduct != null;

    return Scaffold(
      appBar: AppBar(
        title: Text(isEditing ? 'Edit Product' : 'Add Product'),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Product Name
            TextFormField(
              controller: _nameController,
              decoration: const InputDecoration(
                labelText: 'Product Name',
                hintText: 'e.g. Rice (1 kg)',
                prefixIcon: Icon(Icons.shopping_bag),
              ),
              validator: (value) {
                if (value == null || value.isEmpty) {
                  return 'Enter product name';
                }
                return null;
              },
            ),
            const SizedBox(height: 16),

            // Price and Unit
            Row(
              children: [
                Expanded(
                  flex: 2,
                  child: TextFormField(
                    controller: _priceController,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'^\d+\.?\d{0,2}')),
                    ],
                    decoration: const InputDecoration(
                      labelText: 'Price',
                      hintText: '50.00',
                      prefixIcon: Icon(Icons.currency_rupee),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Enter price';
                      }
                      final price = double.tryParse(value);
                      if (price == null || price < 0) {
                        return 'Invalid price';
                      }
                      return null;
                    },
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    value: _selectedUnit,
                    decoration: const InputDecoration(
                      labelText: 'Unit',
                    ),
                    items: _units.map((unit) {
                      return DropdownMenuItem(
                        value: unit,
                        child: Text(unit),
                      );
                    }).toList(),
                    onChanged: (value) {
                      setState(() => _selectedUnit = value!);
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Category
            TextFormField(
              controller: _categoryController,
              decoration: const InputDecoration(
                labelText: 'Category (Optional)',
                hintText: 'e.g. Groceries, Beverages',
                prefixIcon: Icon(Icons.category),
              ),
            ),
            const SizedBox(height: 16),

            // Description
            TextFormField(
              controller: _descriptionController,
              maxLines: 3,
              decoration: const InputDecoration(
                labelText: 'Description (Optional)',
                hintText: 'Add a description...',
                alignLabelWithHint: true,
              ),
            ),
            const SizedBox(height: 32),

            // Submit Button
            ElevatedButton(
              onPressed: _isLoading ? null : _submit,
              child: _isLoading
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        valueColor: AlwaysStoppedAnimation(Colors.white),
                      ),
                    )
                  : Text(isEditing ? 'Update Product' : 'Add Product'),
            ),
          ],
        ),
      ),
    );
  }
}
