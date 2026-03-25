import { Router } from 'express';
import jwt from 'jsonwebtoken';
import { query } from '../db.js';

const router = Router();

router.post('/login', async (req, res) => {
  try {
    const { phone } = req.body;
    
    if (!phone || phone.length !== 10) {
      return res.status(400).json({ success: false, error: 'Invalid phone number' });
    }

    let userResult = await query('SELECT * FROM users WHERE phone = $1', [phone]);
    let user = userResult.rows[0];

    if (!user) {
      await query(
        'INSERT INTO users (phone, role) VALUES ($1, $2)',
        [phone, 'CUSTOMER']
      );
      userResult = await query('SELECT * FROM users WHERE phone = $1', [phone]);
      user = userResult.rows[0];
    }

    await query('UPDATE users SET last_active_at = NOW() WHERE phone = $1', [phone]);

    const shopResult = await query('SELECT * FROM shops WHERE owner_phone = $1', [phone]);
    const shop = shopResult.rows[0] || null;

    const token = jwt.sign(
      { phone: user.phone, role: user.role, shopId: shop?.id },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    res.json({
      success: true,
      data: {
        token,
        user: {
          phone: user.phone,
          name: user.name,
          role: user.role,
          shopId: user.shop_id
        },
        shop: shop ? {
          id: shop.id,
          name: shop.name,
          ownerPhone: shop.owner_phone,
          ownerName: shop.owner_name,
          address: shop.address,
          city: shop.city
        } : null
      }
    });
  } catch (err) {
    console.error('Login error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/register', async (req, res) => {
  try {
    const { phone, shopName, ownerName, address } = req.body;

    if (!phone || phone.length !== 10) {
      return res.status(400).json({ success: false, error: 'Invalid phone number' });
    }
    if (!shopName) {
      return res.status(400).json({ success: false, error: 'Shop name required' });
    }

    const existing = await query('SELECT * FROM shops WHERE owner_phone = $1', [phone]);
    if (existing.rows[0]) {
      return res.status(400).json({ success: false, error: 'Shop already exists for this phone' });
    }

    const shopId = `SHOP_${Math.random().toString(36).substring(2, 10).toUpperCase()}`;
    
    await query(
      `INSERT INTO shops (id, name, owner_phone, owner_name, address) 
       VALUES ($1, $2, $3, $4, $5)`,
      [shopId, shopName, phone, ownerName, address]
    );

    let userResult = await query('SELECT * FROM users WHERE phone = $1', [phone]);
    if (!userResult.rows[0]) {
      await query(
        'INSERT INTO users (phone, name, role, shop_id) VALUES ($1, $2, $3, $4)',
        [phone, ownerName, 'SHOP_OWNER', shopId]
      );
    } else {
      await query(
        'UPDATE users SET role = $1, shop_id = $2, name = COALESCE($3, name) WHERE phone = $4',
        ['SHOP_OWNER', shopId, ownerName, phone]
      );
    }

    userResult = await query('SELECT * FROM users WHERE phone = $1', [phone]);
    const user = userResult.rows[0];
    
    const shopResult = await query('SELECT * FROM shops WHERE id = $1', [shopId]);
    const shop = shopResult.rows[0];

    const token = jwt.sign(
      { phone: user.phone, role: user.role, shopId: shop.id },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );

    res.status(201).json({
      success: true,
      data: {
        token,
        user: { phone: user.phone, name: user.name, role: user.role, shopId: user.shop_id },
        shop: { id: shop.id, name: shop.name, ownerPhone: shop.owner_phone, ownerName: shop.owner_name }
      }
    });
  } catch (err) {
    console.error('Register error:', err);
    res.status(500).json({ success: false, error: err.message });
  }
});

export default router;
