import { Router } from 'express';
import { query } from '../db.js';
import { authMiddleware } from '../middleware/auth.js';

const router = Router();

router.get('/', async (req, res) => {
  try {
    const { shopId, customerPhone, status, page = 1, pageSize = 20 } = req.query;

    if (!shopId && !customerPhone) {
      return res.status(400).json({ success: false, error: 'shopId or customerPhone required' });
    }

    let sql = 'SELECT * FROM orders WHERE 1=1';
    const params = [];
    let idx = 1;

    if (shopId) {
      sql += ` AND shop_id = $${idx++}`;
      params.push(shopId);
    }
    if (customerPhone) {
      sql += ` AND customer_phone = $${idx++}`;
      params.push(customerPhone);
    }
    if (status) {
      sql += ` AND status = $${idx++}`;
      params.push(status);
    }

    const countResult = await query(sql.replace('SELECT *', 'SELECT COUNT(*)'), params);
    const total = parseInt(countResult.rows[0].count);

    sql += ` ORDER BY created_at DESC LIMIT $${idx++} OFFSET $${idx++}`;
    params.push(parseInt(pageSize), (parseInt(page) - 1) * parseInt(pageSize));

    const ordersResult = await query(sql, params);

    const orders = await Promise.all(ordersResult.rows.map(async (order) => {
      const itemsResult = await query('SELECT * FROM order_items WHERE order_id = $1', [order.id]);
      return {
        id: order.id,
        shopId: order.shop_id,
        customerPhone: order.customer_phone,
        customerName: order.customer_name,
        deliveryAddress: order.delivery_address,
        subtotal: parseFloat(order.subtotal),
        total: parseFloat(order.total),
        status: order.status,
        paymentStatus: order.payment_status,
        items: itemsResult.rows.map(i => ({
          id: i.id,
          productId: i.product_id,
          productName: i.product_name,
          quantity: i.quantity,
          unitPrice: parseFloat(i.unit_price),
          totalPrice: parseFloat(i.total_price)
        })),
        createdAt: order.created_at
      };
    }));

    res.json({
      success: true,
      data: { orders, total, page: parseInt(page), pageSize: parseInt(pageSize) }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.get('/stats', async (req, res) => {
  try {
    const { shopId } = req.query;
    if (!shopId) {
      return res.status(400).json({ success: false, error: 'shopId required' });
    }

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const [totalOrders, todayOrders, pendingOrders, revenue] = await Promise.all([
      query('SELECT COUNT(*) FROM orders WHERE shop_id = $1', [shopId]),
      query('SELECT COUNT(*) FROM orders WHERE shop_id = $1 AND created_at >= $2', [shopId, today]),
      query('SELECT COUNT(*) FROM orders WHERE shop_id = $1 AND status = $2', [shopId, 'PENDING']),
      query('SELECT COALESCE(SUM(total), 0) as total FROM orders WHERE shop_id = $1 AND payment_status = $2', [shopId, 'PAID'])
    ]);

    res.json({
      success: true,
      data: {
        totalOrders: parseInt(totalOrders.rows[0].count),
        todayOrders: parseInt(todayOrders.rows[0].count),
        pendingOrders: parseInt(pendingOrders.rows[0].count),
        totalRevenue: parseFloat(revenue.rows[0].total)
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const orderResult = await query('SELECT * FROM orders WHERE id = $1', [req.params.id]);
    if (!orderResult.rows[0]) {
      return res.status(404).json({ success: false, error: 'Order not found' });
    }

    const order = orderResult.rows[0];
    const itemsResult = await query('SELECT * FROM order_items WHERE order_id = $1', [order.id]);

    res.json({
      success: true,
      data: {
        id: order.id,
        shopId: order.shop_id,
        customerPhone: order.customer_phone,
        customerName: order.customer_name,
        deliveryAddress: order.delivery_address,
        subtotal: parseFloat(order.subtotal),
        total: parseFloat(order.total),
        status: order.status,
        paymentStatus: order.payment_status,
        notes: order.notes,
        items: itemsResult.rows.map(i => ({
          id: i.id,
          productId: i.product_id,
          productName: i.product_name,
          quantity: i.quantity,
          unitPrice: parseFloat(i.unit_price),
          totalPrice: parseFloat(i.total_price)
        })),
        createdAt: order.created_at
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.post('/', async (req, res) => {
  try {
    const { shopId, customerPhone, customerName, deliveryAddress, items, notes, paymentMethod } = req.body;

    if (!shopId || !customerPhone || !items || items.length === 0) {
      return res.status(400).json({ success: false, error: 'shopId, customerPhone, and items required' });
    }

    const orderId = `ORD_${Math.random().toString(36).substring(2, 10).toUpperCase()}`;

    let subtotal = 0;
    const orderItems = [];

    for (const item of items) {
      const productResult = await query('SELECT * FROM products WHERE id = $1', [item.productId]);
      const product = productResult.rows[0];
      
      if (!product) continue;
      
      const itemTotal = parseFloat(product.price) * item.quantity;
      subtotal += itemTotal;
      
      orderItems.push({
        productId: product.id,
        productName: product.name,
        quantity: item.quantity,
        unitPrice: parseFloat(product.price),
        totalPrice: itemTotal
      });
    }

    await query(
      `INSERT INTO orders (id, shop_id, customer_phone, customer_name, delivery_address, subtotal, total, notes, payment_method)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [orderId, shopId, customerPhone, customerName, deliveryAddress, subtotal, subtotal, notes, paymentMethod]
    );

    for (const item of orderItems) {
      await query(
        `INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, total_price)
         VALUES ($1, $2, $3, $4, $5, $6)`,
        [orderId, item.productId, item.productName, item.quantity, item.unitPrice, item.totalPrice]
      );
    }

    await query('DELETE FROM cart_items WHERE user_phone = $1 AND shop_id = $2', [customerPhone, shopId]);

    res.status(201).json({
      success: true,
      data: {
        id: orderId,
        shopId,
        customerPhone,
        total: subtotal,
        status: 'PENDING',
        items: orderItems
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

router.put('/:id/status', authMiddleware, async (req, res) => {
  try {
    const { status } = req.body;
    const validStatuses = ['PENDING', 'ACCEPTED', 'REJECTED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'];
    
    if (!validStatuses.includes(status)) {
      return res.status(400).json({ success: false, error: 'Invalid status' });
    }

    const orderResult = await query('SELECT * FROM orders WHERE id = $1', [req.params.id]);
    if (!orderResult.rows[0]) {
      return res.status(404).json({ success: false, error: 'Order not found' });
    }
    if (req.user.shopId !== orderResult.rows[0].shop_id) {
      return res.status(403).json({ success: false, error: 'Not authorized' });
    }

    let extraUpdate = '';
    if (status === 'ACCEPTED') extraUpdate = ', accepted_at = NOW()';
    if (status === 'DELIVERED') extraUpdate = ', delivered_at = NOW()';

    await query(`UPDATE orders SET status = $1${extraUpdate}, updated_at = NOW() WHERE id = $2`, [status, req.params.id]);

    const updated = await query('SELECT * FROM orders WHERE id = $1', [req.params.id]);
    
    res.json({
      success: true,
      data: { id: updated.rows[0].id, status: updated.rows[0].status }
    });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

export default router;
