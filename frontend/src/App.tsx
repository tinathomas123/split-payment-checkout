import { Link, Route, Routes } from 'react-router-dom';
import { useCart } from './CartContext';
import CartPage from './pages/CartPage';
import CheckoutPage from './pages/CheckoutPage';
import OrderStatusPage from './pages/OrderStatusPage';
import ProductListPage from './pages/ProductListPage';

export default function App() {
  const { itemCount } = useCart();

  return (
    <div className="app">
      <header className="header">
        <Link to="/" className="brand">Split-Pay Shop</Link>
        <nav>
          <Link to="/">Products</Link>
          <Link to="/cart">Cart ({itemCount})</Link>
        </nav>
      </header>

      <main>
        <Routes>
          <Route path="/" element={<ProductListPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/orders/:orderId" element={<OrderStatusPage />} />
          <Route path="*" element={<p>Page not found. <Link to="/">Back to products</Link></p>} />
        </Routes>
      </main>
    </div>
  );
}
