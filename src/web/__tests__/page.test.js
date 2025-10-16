import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import Home from '../src/app/page';

test('renders header text', () => {
  // Render does not run Supabase calls because page is client component that calls supabase in useEffect
  const { getByText } = render(<Home />);
  const header = getByText(/Registros en tiempo real desde Supabase/i);
  expect(header).toBeInTheDocument();
});
