
"use client";
import { useEffect, useState } from "react";
import { supabase } from "../utils/supabaseClient";

export default function Home() {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const tableName = "Registros"; // Reemplaza con el nombre de tu tabla

  useEffect(() => {
    // Fetch initial data
    const fetchData = async () => {
      const { data, error } = await supabase
        .from(tableName)
        .select("*")
        .order("ID_medicion", { ascending: false })
        .limit(100);

      if (error) {
        setError(error.message);
        setRecords([]);
      } else {
        setError(null);
        setRecords(data || []);
      }
      setLoading(false);
    };
    fetchData();

    // Subscribe to real-time changes
    const channel = supabase
      .channel("realtime-list")
      .on(
        "postgres_changes",
        { event: "*", schema: "public", table: tableName },
        (payload) => {
          // Refetch data on any change
          fetchData();
        }
      )
      .subscribe();
    return () => {
      supabase.removeChannel(channel);
    };
  }, []);

  return (
    <main className="min-h-screen w-full flex flex-col items-center p-8 bg-neutral-50 text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
      <h1 className="text-3xl font-extrabold mb-6 text-center">Registros en tiempo real desde Supabase</h1>

      <div className="w-full max-w-4xl">
        {loading && (
          <div className="text-sm text-neutral-500 dark:text-neutral-400">Cargando…</div>
        )}
        {error && (
          <div className="mb-4 rounded border border-red-400/40 bg-red-50 dark:bg-red-950/30 p-3 text-red-700 dark:text-red-300 text-sm">
            Error al cargar: {error}
          </div>
        )}

        {!loading && !error && (
          <div className="overflow-hidden rounded-lg border border-neutral-200 dark:border-neutral-800 shadow">
            <div className="max-h-[70vh] overflow-auto">
              <table className="min-w-full table-fixed">
                <thead className="bg-neutral-100 dark:bg-neutral-900/60 text-neutral-700 dark:text-neutral-200 sticky top-0">
                  <tr>
                    <th className="px-4 py-3 text-left font-semibold w-28">ID</th>
                    <th className="px-4 py-3 text-left font-semibold">Tipo</th>
                    <th className="px-4 py-3 text-left font-semibold w-28">Valor</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-200 dark:divide-neutral-800">
                  {records.length === 0 ? (
                    <tr>
                      <td colSpan={3} className="px-4 py-4 text-neutral-500 dark:text-neutral-400">
                        No hay registros.
                      </td>
                    </tr>
                  ) : (
                    records.map((r) => (
                      <tr key={r.ID_medicion ?? r.id ?? Math.random()} className="odd:bg-white even:bg-neutral-50 dark:odd:bg-neutral-900 dark:even:bg-neutral-900/60">
                        <td className="px-4 py-3 font-mono text-sm text-neutral-700 dark:text-neutral-300">
                          {r.ID_medicion ?? r.id ?? "—"}
                        </td>
                        <td className="px-4 py-3">
                          <span className="inline-flex items-center rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300 px-2 py-0.5 text-xs font-medium">
                            {r.Tipo ?? "—"}
                          </span>
                        </td>
                        <td className="px-4 py-3 font-semibold">
                          {r.Valor ?? "—"}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}
