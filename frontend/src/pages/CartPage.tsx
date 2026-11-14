import { Link } from 'react-router-dom';
import { useCart } from '../CartContext';
import { formatCents } from '../money';

export default function CartPage() {
  const { items, totalCents, setQuantity, removeFromCart } = useCart();

  if (items.length === 0) {
    return (
      <section>
        <h1>Your cart</h1>
        <p>Your cart is empty. <Link to="/">Browse products</Link></p>
      </section>
    );
  }

  return (
    <section>
      <h1>Your cart</h1>
      <table className="table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Price</th>
            <th>Quantity</th>
            <th>Subtotal</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {items.map(({ product, quantity }) => (
            <tr key={product.id}>
              <td>{product.name}</td>
              <td>{formatCents(product.priceCents)}</td>
              <td>
                <button aria-label="Decrease quantity" onClick={() => setQuantity(product.id, quantity - 1)}>−</button>
                <span className="qty">{quantity}</span>
                <button aria-label="Increase quantity" onClick={() => setQuantity(product.id, quantity + 1)}>+</button>
              </td>
              <td>{formatCents(product.priceCents * quantity)}</td>
              <td>
                <button className="link-button" onClick={() => removeFromCart(product.id)}>Remove</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="total">Total: {formatCents(totalCents)}</p>
      <Link to="/checkout" className="button">Go to checkout</Link>
    </section>
  );
}
