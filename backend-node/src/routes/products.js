import { Router } from 'express';
import { query } from '../db.js';
import { authMiddleware } from '../middleware/auth.js';

const router = Router();

router.get('/', async (req, res) => {
  try {
    const { shopId, category, available } = req.query;
    
    if (!shopId) {
      return res.status(400).json({ success: false, error: 'shopId required' });
    }

    let sql = 'SELECT * FROM products WHERE shop_id = $1';
    const params = [shopId];

    if (available === 'true') {
      sql += ' AND is_available = true';
    }
    if (category) {
      sql += ` AND category = $${params.length + 1}`;
      params.push(category);
    }

    sql += ' ORDER BY display_order ASC, name ASC';

    const result = await query(sql, params);
    
    res.json({
      success: true,
      data: result.rows.map(p => ({
        id: p.id,
        shopId: p.shop_id,
        name: p.name,
        description: p.description,
        price: parseFloat(p.price),
        unit: p.unit,
        imageUrl: p.image_url,
        category: p.category,
        stock: p.stock,
        isAvailable: p.is_available,
        displayOrder: p.display_order
      }))
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const result = await query('SELECT * FROM products WHERE id = $1', [req.params.id]);
    
    if (!result.rows[0]) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }

    const p = result.rows[0];
    res.json({
      success: true,
      data: {
        id: p.id,
        shopId: p.shop_id,
        name: p.name,
        description: p.description,
        price: parseFloat(p.price),
        unit: p.unit,
        imageUrl: p.image_url,
        category: p.category,
        stock: p.stock,
        isAvailable: p.is_available
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/', authMiddleware, async (req, res) => {
  try {
    const { shopId, name, description, price, unit, imageUrl, category, stock } = req.body;

    if (req.user.shopId !== shopId) {
      return res.status(403).json({ success: false, error: 'Not authorized' });
    }

    const productId = `P_${Math.random().toString(36).substring(2, 10).toUpperCase()}`;

    await query(
      `INSERT INTO products (id, shop_id, name, description, price, unit, image_url, category, stock)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [productId, shopId, name, description, price, unit || 'piece', imageUrl, category, stock ?? -1]
    );

    const result = await query('SELECT * FROM products WHERE id = $1', [productId]);
    const p = result.rows[0];

    res.status(201).json({
      success: true,
      data: {
        id: p.id,
        shopId: p.shop_id,
        name: p.name,
        price: parseFloat(p.price),
        unit: p.unit,
        isAvailable: p.is_available
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.put('/:id', authMiddleware, async (req, res) => {
  try {
    const { name, description, price, unit, imageUrl, category, stock, isAvailable } = req.body;

    const existing = await query('SELECT * FROM products WHERE id = $1', [req.params.id]);
    if (!existing.rows[0]) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }
    if (req.user.shopId !== existing.rows[0].shop_id) {
      return res.status(403).json({ success: false, error: 'Not authorized' });
    }

    const updates = [];
    const values = [];
    let idx = 1;

    if (name !== undefined) { updates.push(`name = $${idx++}`); values.push(name); }
    if (description !== undefined) { updates.push(`description = $${idx++}`); values.push(description); }
    if (price !== undefined) { updates.push(`price = $${idx++}`); values.push(price); }
    if (unit !== undefined) { updates.push(`unit = $${idx++}`); values.push(unit); }
    if (imageUrl !== undefined) { updates.push(`image_url = $${idx++}`); values.push(imageUrl); }
    if (category !== undefined) { updates.push(`category = $${idx++}`); values.push(category); }
    if (stock !== undefined) { updates.push(`stock = $${idx++}`); values.push(stock); }
    if (isAvailable !== undefined) { updates.push(`is_available = $${idx++}`); values.push(isAvailable); }

    if (updates.length === 0) {
      return res.status(400).json({ success: false, error: 'No fields to update' });
    }

    values.push(req.params.id);
    await query(`UPDATE products SET ${updates.join(', ')}, updated_at = NOW() WHERE id = $${idx}`, values);

    const result = await query('SELECT * FROM products WHERE id = $1', [req.params.id]);
    const p = result.rows[0];

    res.json({
      success: true,
      data: {
        id: p.id,
        shopId: p.shop_id,
        name: p.name,
        price: parseFloat(p.price),
        unit: p.unit,
        isAvailable: p.is_available
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.put('/:id/toggle', authMiddleware, async (req, res) => {
  try {
    const existing = await query('SELECT * FROM products WHERE id = $1', [req.params.id]);
    if (!existing.rows[0]) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }
    if (req.user.shopId !== existing.rows[0].shop_id) {
      return res.status(403).json({ success: false, error: 'Not authorized' });
    }

    await query(
      'UPDATE products SET is_available = NOT is_available, updated_at = NOW() WHERE id = $1',
      [req.params.id]
    );

    const result = await query('SELECT * FROM products WHERE id = $1', [req.params.id]);
    const p = result.rows[0];

    res.json({
      success: true,
      data: { id: p.id, name: p.name, isAvailable: p.is_available }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.delete('/:id', authMiddleware, async (req, res) => {
  try {
    const existing = await query('SELECT * FROM products WHERE id = $1', [req.params.id]);
    if (!existing.rows[0]) {
      return res.status(404).json({ success: false, error: 'Product not found' });
    }
    if (req.user.shopId !== existing.rows[0].shop_id) {
      return res.status(403).json({ success: false, error: 'Not authorized' });
    }

    await query('DELETE FROM products WHERE id = $1', [req.params.id]);

    res.json({ success: true, data: { message: 'Product deleted' } });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

export default router;
