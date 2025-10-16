import { supabase } from '../src/utils/supabaseClient';

test('supabase client is defined', () => {
  expect(supabase).toBeDefined();
});
