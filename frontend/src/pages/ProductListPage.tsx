import { useState } from 'react';
import { useCart } from '../CartContext';
import { formatCents } from '../money';
import { PRODUCTS } from '../products';
import type { Product } from '../types';

export default function ProductListPage() {
  const { addToCart } = useCart();
  const [lastAdded, setLastAdded] = useState<string | null>(null);

  const handleAdd = (product: Product) => {
    addToCart(product);
    setLastAdded(product.name);
  };

  return (
    <section>
      <h1>Products</h1>
      {lastAdded && <p className="notice">Added {lastAdded} to your cart.</p>}
      <ul className="product-grid">
        {PRODUCTS.map((product) => (
          <li key={product.id} className="card">
            <h2>{product.name}</h2>
            <p className="muted">{product.description}</p>
            <p className="price">{formatCents(product.priceCents)}</p>
            <button onClick={() => handleAdd(product)}>Add to cart</button>
          </li>
        ))}
      </ul>
    </section>
  );
}
