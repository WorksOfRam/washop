import { Router } from 'express';
import { query } from '../db.js';

const router = Router();

router.get('/', async (req, res) => {
  try {
    const { userId, shopId } = req.query;
    
    if (!userId || !shopId) {
      return res.status(400).json({ success: false, error: 'userId and shopId required' });
    }

    const result = await query(
      `SELECT ci.*, p.name as product_name, p.price as product_price, p.unit
       FROM cart_items ci
       JOIN products p ON ci.product_id = p.id
       WHERE ci.user_phone = $1 AND ci.shop_id = $2`,
      [userId, shopId]
    );

    const items = result.rows.map(item => ({
      id: item.id,
      productId: item.product_id,
      productName: item.product_name,
      productPrice: parseFloat(item.product_price),
      quantity: item.quantity,
      itemTotal: parseFloat(item.product_price) * item.quantity
    }));

    const subtotal = items.reduce((sum, item) => sum + item.itemTotal, 0);

    res.json({
      success: true,
      data: {
        userPhone: userId,
        shopId,
        items,
        subtotal,
        itemCount: items.reduce((sum, item) => sum + item.quantity, 0)
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/add', async (req, res) => {
  try {
    const { userPhone, shopId, productId, quantity = 1 } = req.body;

    if (!userPhone || !shopId || !productId) {
      return res.status(400).json({ success: false, error: 'userPhone, shopId, productId required' });
    }

    const product = await query('SELECT * FROM products WHERE id = $1', [productId]);
    if (!product.rows[0]) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }
    if (!product.rows[0].is_available) {
      return res.status(400).json({ success: false, error: 'Product not available' });
    }

    const existing = await query(
      'SELECT * FROM cart_items WHERE user_phone = $1 AND shop_id = $2 AND product_id = $3',
      [userPhone, shopId, productId]
    );

    if (existing.rows[0]) {
      await query(
        'UPDATE cart_items SET quantity = quantity + $1, updated_at = NOW() WHERE id = $2',
        [quantity, existing.rows[0].id]
      );
    } else {
      await query(
        'INSERT INTO cart_items (user_phone, shop_id, product_id, quantity) VALUES ($1, $2, $3, $4)',
        [userPhone, shopId, productId, quantity]
      );
    }

    const cartResult = await query(
      `SELECT ci.*, p.name as product_name, p.price as product_price
       FROM cart_items ci
       JOIN products p ON ci.product_id = p.id
       WHERE ci.user_phone = $1 AND ci.shop_id = $2`,
      [userPhone, shopId]
    );

    const items = cartResult.rows.map(item => ({
      productId: item.product_id,
      productName: item.product_name,
      quantity: item.quantity,
      itemTotal: parseFloat(item.product_price) * item.quantity
    }));

    res.json({
      success: true,
      data: {
        items,
        subtotal: items.reduce((sum, item) => sum + item.itemTotal, 0),
        itemCount: items.reduce((sum, item) => sum + item.quantity, 0)
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/remove', async (req, res) => {
  try {
    const { userPhone, shopId, productId } = req.body;

    await query(
      'DELETE FROM cart_items WHERE user_phone = $1 AND shop_id = $2 AND product_id = $3',
      [userPhone, shopId, productId]
    );

    res.json({ success: true, data: { message: 'Item removed' } });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.delete('/clear', async (req, res) => {
  try {
    const { userId, shopId } = req.query;

    const result = await query(
      'DELETE FROM cart_items WHERE user_phone = $1 AND shop_id = $2',
      [userId, shopId]
    );

    res.json({ success: true, data: { itemsRemoved: result.rowCount } });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

export default router;
