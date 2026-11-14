import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import type { CartItem, Product } from './types';

interface CartContextValue {
  items: CartItem[];
  totalCents: number;
  itemCount: number;
  addToCart: (product: Product) => void;
  setQuantity: (productId: string, quantity: number) => void;
  removeFromCart: (productId: string) => void;
  clearCart: () => void;
}

const CartContext = createContext<CartContextValue | null>(null);

/** Holds the cart for the whole app, so every page sees the same items. */
export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);

  const addToCart = (product: Product) => {
    setItems((current) => {
      const existing = current.find((item) => item.product.id === product.id);
      if (existing) {
        return current.map((item) =>
          item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item,
        );
      }
      return [...current, { product, quantity: 1 }];
    });
  };

  const setQuantity = (productId: string, quantity: number) => {
    if (quantity < 1) {
      removeFromCart(productId);
      return;
    }
    setItems((current) =>
      current.map((item) => (item.product.id === productId ? { ...item, quantity } : item)),
    );
  };

  const removeFromCart = (productId: string) => {
    setItems((current) => current.filter((item) => item.product.id !== productId));
  };

  const clearCart = () => setItems([]);

  // Recalculated only when the items change.
  const { totalCents, itemCount } = useMemo(
    () => ({
      totalCents: items.reduce((sum, item) => sum + item.product.priceCents * item.quantity, 0),
      itemCount: items.reduce((sum, item) => sum + item.quantity, 0),
    }),
    [items],
  );

  return (
    <CartContext.Provider
      value={{ items, totalCents, itemCount, addToCart, setQuantity, removeFromCart, clearCart }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart(): CartContextValue {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used inside CartProvider');
  }
  return context;
}
