import type { Product } from './types';

// A fixed catalogue keeps the project focused on checkout and payments.
// (A real shop would load this from a product service.)
export const PRODUCTS: Product[] = [
  { id: 'headphones', name: 'Headphones', priceCents: 2500, description: 'Over-ear, wireless' },
  { id: 'backpack', name: 'Backpack', priceCents: 5000, description: '25 litre, water-resistant' },
  { id: 'water-bottle', name: 'Water Bottle', priceCents: 1999, description: 'Insulated, 750 ml' },
  { id: 'keyboard', name: 'Keyboard', priceCents: 8950, description: 'Mechanical, compact layout' },
  { id: 'desk-lamp', name: 'Desk Lamp', priceCents: 3475, description: 'LED, adjustable arm' },
];
