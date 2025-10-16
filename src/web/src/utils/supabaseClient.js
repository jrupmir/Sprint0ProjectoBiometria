import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://feghqusjsotnrxhfadrm.supabase.co';
const supabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZlZ2hxdXNqc290bnJ4aGZhZHJtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0NTAzNTgsImV4cCI6MjA3NjAyNjM1OH0.CNGX-u5MS8ifh6QS4QhATJovj1bjLNa55iMYdh5n-Ug'; // Reemplaza con tu anon key

export const supabase = createClient(supabaseUrl, supabaseAnonKey);
