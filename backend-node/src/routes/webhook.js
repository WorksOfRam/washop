import { Router } from 'express';
import { query } from '../db.js';

const router = Router();
const DEFAULT_SHOP_ID = 'SHOP_001';

router.get('/', (req, res) => {
  const mode = req.query['hub.mode'];
  const token = req.query['hub.verify_token'];
  const challenge = req.query['hub.challenge'];

  if (mode === 'subscribe' && token === process.env.WHATSAPP_VERIFY_TOKEN) {
    console.log('Webhook verified');
    res.status(200).send(challenge);
  } else {
    res.status(403).send('Verification failed');
  }
});

router.post('/', async (req, res) => {
  try {
    const { entry } = req.body;
    
    if (entry) {
      for (const e of entry) {
        const changes = e.changes || [];
        for (const change of changes) {
          const messages = change.value?.messages || [];
          for (const message of messages) {
            if (message.from && message.text?.body) {
              await handleMessage(message.from, message.text.body);
            }
          }
        }
      }
    }
    
    res.status(200).json({ success: true });
  } catch (err) {
    console.error('Webhook error:', err);
    res.status(200).json({ success: true });
  }
});

router.post('/test', async (req, res) => {
  try {
    const { from, message } = req.body;
    const response = await handleMessage(from, message);
    res.json({ success: true, data: { response } });
  } catch (err) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// MSG91 WhatsApp Webhook
router.post('/msg91', async (req, res) => {
  try {
    console.log('MSG91 Webhook:', JSON.stringify(req.body));
    
    // MSG91 payload format: { customerNumber, message, messageId, timestamp }
    const { customerNumber, message } = req.body;
    
    if (customerNumber && message) {
      // Remove country code if present (91XXXXXXXXXX -> XXXXXXXXXX)
      const phone = customerNumber.replace(/^91/, '');
      const botResponse = await handleMessage(phone, message);
      
      // Send reply via MSG91
      if (process.env.MSG91_AUTH_KEY) {
        await sendMsg91Reply(customerNumber, botResponse);
      } else {
        console.log('MSG91 Reply (no auth key):', botResponse);
      }
    }
    
    res.status(200).json({ success: true });
  } catch (err) {
    console.error('MSG91 Webhook error:', err);
    res.status(200).json({ success: true });
  }
});

// Send message via MSG91
async function sendMsg91Reply(to, message) {
  try {
    // Ensure number has country code
    const toNumber = to.startsWith('91') ? to : `91${to}`;
    
    const response = await fetch('https://api.msg91.com/api/v5/whatsapp/whatsapp-outbound-message/bulk/', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'authkey': process.env.MSG91_AUTH_KEY
      },
      body: JSON.stringify({
        integrated_number: process.env.MSG91_PHONE_NUMBER,
        content_type: 'text',
        payload: {
          messaging_product: 'whatsapp',
          type: 'text',
          text: {
            body: message
          }
        },
        recipients: [
          { mobiles: toNumber }
        ]
      })
    });
    
    const result = await response.text();
    console.log('MSG91 Reply sent:', response.status, result);
  } catch (err) {
    console.error('MSG91 Send error:', err);
  }
}

async function handleMessage(from, text) {
  const command = parseCommand(text);
  let response;

  switch (command.type) {
    case 'GREETING':
      response = getGreetingMessage();
      break;
    case 'LIST_PRODUCTS':
      response = await getProductsMessage();
      break;
    case 'ADD_TO_CART':
      response = await addToCartMessage(from, command.productId, command.quantity, command.unit, command.displayQty);
      break;
    case 'VIEW_CART':
      response = await getCartMessage(from);
      break;
    case 'CLEAR_CART':
      response = await clearCartMessage(from);
      break;
    case 'CHECKOUT':
      response = await checkoutMessage(from);
      break;
    case 'HELP':
      response = getHelpMessage();
      break;
    default:
      response = getUnknownMessage();
  }

  console.log(`[${from}] ${text} -> ${response.substring(0, 50)}...`);
  return response;
}

function parseCommand(text) {
  const normalized = text.trim().toLowerCase();

  if (['hi', 'hello', 'hey', 'start', 'menu'].includes(normalized)) {
    return { type: 'GREETING' };
  }
  if (['products', 'list', 'catalog', 'items'].includes(normalized)) {
    return { type: 'LIST_PRODUCTS' };
  }
  if (['cart', 'view cart', 'my cart'].includes(normalized)) {
    return { type: 'VIEW_CART' };
  }
  if (['checkout', 'order', 'buy', 'confirm'].includes(normalized)) {
    return { type: 'CHECKOUT' };
  }
  if (['clear', 'clear cart', 'empty cart'].includes(normalized)) {
    return { type: 'CLEAR_CART' };
  }
  if (['help', '?'].includes(normalized)) {
    return { type: 'HELP' };
  }

  // Pattern: "1 500g" or "1 2kg" or "1 1.5kg" (product with weight)
  const weightMatch = normalized.match(/^(\d+)\s+([\d.]+)\s*(g|gm|gms|gram|grams|kg|kgs|kilo|kilos)$/i);
  if (weightMatch) {
    const productId = weightMatch[1];
    let amount = parseFloat(weightMatch[2]);
    const unit = weightMatch[3].toLowerCase();
    
    // Convert everything to kg for calculation
    if (['g', 'gm', 'gms', 'gram', 'grams'].includes(unit)) {
      amount = amount / 1000; // Convert grams to kg
    }
    
    return { 
      type: 'ADD_TO_CART', 
      productId, 
      quantity: amount, 
      unit: 'kg',
      displayQty: weightMatch[2] + weightMatch[3]
    };
  }

  // Pattern: "1 2" (product with count quantity)
  if (/^\d+\s+\d+$/.test(normalized)) {
    const [id, qty] = normalized.split(/\s+/);
    return { type: 'ADD_TO_CART', productId: id, quantity: parseInt(qty), unit: 'piece' };
  }

  // Pattern: just "1" (single item)
  if (/^\d+$/.test(normalized)) {
    return { type: 'ADD_TO_CART', productId: normalized, quantity: 1, unit: 'piece' };
  }

  // Pattern: "add 1 500g" or "add 2 1kg"
  if (normalized.startsWith('add ')) {
    const rest = normalized.replace('add ', '').trim();
    const addWeightMatch = rest.match(/^(\d+)\s+([\d.]+)\s*(g|gm|gms|gram|grams|kg|kgs|kilo|kilos)$/i);
    if (addWeightMatch) {
      const productId = addWeightMatch[1];
      let amount = parseFloat(addWeightMatch[2]);
      const unit = addWeightMatch[3].toLowerCase();
      
      if (['g', 'gm', 'gms', 'gram', 'grams'].includes(unit)) {
        amount = amount / 1000;
      }
      
      return { 
        type: 'ADD_TO_CART', 
        productId, 
        quantity: amount, 
        unit: 'kg',
        displayQty: addWeightMatch[2] + addWeightMatch[3]
      };
    }
    
    const parts = rest.split(/\s+/);
    return { type: 'ADD_TO_CART', productId: parts[0], quantity: parseInt(parts[1]) || 1, unit: 'piece' };
  }

  return { type: 'UNKNOWN' };
}

function getGreetingMessage() {
  return `🙏 *Welcome to WA Shop!*

I'm here to help you order products.

📋 *Quick Commands:*
• *products* - View all items
• *cart* - View your cart
• *checkout* - Place order
• *help* - All commands

_Type "products" to start shopping!_`;
}

async function getProductsMessage() {
  const result = await query(
    'SELECT * FROM products WHERE shop_id = $1 AND is_available = true ORDER BY display_order, name',
    [DEFAULT_SHOP_ID]
  );

  if (result.rows.length === 0) {
    return 'No products available at the moment.';
  }

  const productList = result.rows.map((p, i) => 
    `*${i + 1}.* ${p.name} - ₹${parseFloat(p.price).toFixed(0)}/${p.unit}`
  ).join('\n');

  return `🛍️ *Available Products*

${productList}

━━━━━━━━━━━━━━━━
_To add: Type the number_
_Example: "1" adds Rice_
_Or: "1 2" adds 2 Rice_`;
}

async function addToCartMessage(userPhone, productId, quantity, unit = 'piece', displayQty = null) {
  const products = await query(
    'SELECT * FROM products WHERE shop_id = $1 AND is_available = true ORDER BY display_order, name',
    [DEFAULT_SHOP_ID]
  );

  const index = parseInt(productId);
  const product = (index > 0 && index <= products.rows.length) 
    ? products.rows[index - 1] 
    : null;

  if (!product) {
    return 'Product not found. Type *products* to see available items.';
  }

  // Calculate price based on product unit and ordered quantity
  let itemTotal;
  let quantityDisplay;
  const productUnit = product.unit.toLowerCase();
  
  if (unit === 'kg' && ['kg', 'kgs', 'kilo'].includes(productUnit)) {
    // Ordering by weight for kg-priced products
    itemTotal = parseFloat(product.price) * quantity;
    quantityDisplay = displayQty || `${quantity}kg`;
  } else if (unit === 'kg' && ['g', 'gm', 'gram', 'grams'].includes(productUnit)) {
    // Product priced per gram, convert
    itemTotal = parseFloat(product.price) * quantity * 1000;
    quantityDisplay = displayQty || `${quantity * 1000}g`;
  } else {
    // Regular piece/pack ordering
    itemTotal = parseFloat(product.price) * quantity;
    quantityDisplay = `${quantity} ${product.unit}`;
  }

  // Store quantity in database (for kg products, store as decimal kg)
  const existing = await query(
    'SELECT * FROM cart_items WHERE user_phone = $1 AND shop_id = $2 AND product_id = $3',
    [userPhone, DEFAULT_SHOP_ID, product.id]
  );

  if (existing.rows[0]) {
    await query(
      'UPDATE cart_items SET quantity = quantity + $1 WHERE id = $2',
      [quantity, existing.rows[0].id]
    );
  } else {
    await query(
      'INSERT INTO cart_items (user_phone, shop_id, product_id, quantity) VALUES ($1, $2, $3, $4)',
      [userPhone, DEFAULT_SHOP_ID, product.id, quantity]
    );
  }

  const cartResult = await query(
    `SELECT SUM(ci.quantity * p.price) as total
     FROM cart_items ci JOIN products p ON ci.product_id = p.id
     WHERE ci.user_phone = $1 AND ci.shop_id = $2`,
    [userPhone, DEFAULT_SHOP_ID]
  );

  const cartTotal = parseFloat(cartResult.rows[0]?.total || 0);

  return `✅ *Added to cart!*

${product.name} x ${quantityDisplay} = ₹${itemTotal.toFixed(0)}

🛒 Cart Total: ₹${cartTotal.toFixed(0)}

_Type "cart" to view or "checkout" to order_`;
}

async function getCartMessage(userPhone) {
  const result = await query(
    `SELECT ci.quantity, p.name, p.price, p.unit, (ci.quantity * p.price) as total
     FROM cart_items ci JOIN products p ON ci.product_id = p.id
     WHERE ci.user_phone = $1 AND ci.shop_id = $2`,
    [userPhone, DEFAULT_SHOP_ID]
  );

  if (result.rows.length === 0) {
    return `🛒 *Your cart is empty*

_Type "products" to start shopping_`;
  }

  const items = result.rows.map((item, i) => {
    const qty = parseFloat(item.quantity);
    const unit = item.unit.toLowerCase();
    let qtyDisplay;
    
    // Format quantity based on unit type
    if (['kg', 'kgs', 'kilo'].includes(unit)) {
      if (qty < 1) {
        qtyDisplay = `${(qty * 1000).toFixed(0)}g`;
      } else {
        qtyDisplay = qty % 1 === 0 ? `${qty}kg` : `${qty}kg`;
      }
    } else if (['g', 'gm', 'gram', 'grams'].includes(unit)) {
      qtyDisplay = `${qty}g`;
    } else {
      qtyDisplay = qty % 1 === 0 ? `${qty}` : `${qty.toFixed(2)}`;
    }
    
    return `${i + 1}. ${item.name} x ${qtyDisplay} = ₹${parseFloat(item.total).toFixed(0)}`;
  }).join('\n');

  const total = result.rows.reduce((sum, item) => sum + parseFloat(item.total), 0);

  return `🛒 *Your Cart*

${items}

━━━━━━━━━━━━━━━━
*Total: ₹${total.toFixed(0)}*

• *checkout* - Place order
• *clear* - Empty cart`;
}

async function clearCartMessage(userPhone) {
  await query(
    'DELETE FROM cart_items WHERE user_phone = $1 AND shop_id = $2',
    [userPhone, DEFAULT_SHOP_ID]
  );
  
  return `🗑️ *Cart cleared!*

_Type "products" to start shopping again_`;
}

async function checkoutMessage(userPhone) {
  const cartResult = await query(
    `SELECT ci.*, p.name, p.price
     FROM cart_items ci JOIN products p ON ci.product_id = p.id
     WHERE ci.user_phone = $1 AND ci.shop_id = $2`,
    [userPhone, DEFAULT_SHOP_ID]
  );

  if (cartResult.rows.length === 0) {
    return 'Your cart is empty. Add some products first!';
  }

  const orderId = `ORD_${Math.random().toString(36).substring(2, 10).toUpperCase()}`;
  let total = 0;

  await query(
    `INSERT INTO orders (id, shop_id, customer_phone, subtotal, total, notes, payment_method)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [orderId, DEFAULT_SHOP_ID, userPhone, 0, 0, 'Order via WhatsApp', 'COD']
  );

  for (const item of cartResult.rows) {
    const itemTotal = parseFloat(item.price) * item.quantity;
    total += itemTotal;
    
    await query(
      `INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, total_price)
       VALUES ($1, $2, $3, $4, $5, $6)`,
      [orderId, item.product_id, item.name, item.quantity, item.price, itemTotal]
    );
  }

  await query('UPDATE orders SET subtotal = $1, total = $2 WHERE id = $3', [total, total, orderId]);
  await query('DELETE FROM cart_items WHERE user_phone = $1 AND shop_id = $2', [userPhone, DEFAULT_SHOP_ID]);

  return `✅ *Order Placed Successfully!*

📦 Order ID: *${orderId}*
💰 Total: *₹${total.toFixed(0)}*
📍 Status: PENDING

━━━━━━━━━━━━━━━━
The shop owner has been notified.

Payment: Cash on Delivery`;
}

function getHelpMessage() {
  return `📖 *Available Commands*

🛍️ *Shopping*
• *products* - View all items
• *1* - Add 1 piece of item #1
• *1 2* - Add 2 pieces of item #1

⚖️ *Order by Weight*
• *1 500g* - Add 500 grams of item #1
• *1 1kg* - Add 1 kg of item #1
• *2 250g* - Add 250g of item #2
• *3 1.5kg* - Add 1.5 kg of item #3

🛒 *Cart*
• *cart* - View your cart
• *clear* - Empty cart
• *checkout* - Place order

_Type "hi" to start!_`;
}

function getUnknownMessage() {
  return `Sorry, I didn't understand that.

_Type "help" for available commands_
_Or "products" to see our catalog_`;
}

export default router;
