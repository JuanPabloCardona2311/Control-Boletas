import { useCallback, useEffect, useState } from 'react';

/** Mensaje corto que aparece abajo y se va solo. */
export function useAviso() {
  const [texto, setTexto] = useState<string | null>(null);
  useEffect(() => {
    if (!texto) return;
    const t = setTimeout(() => setTexto(null), 2800);
    return () => clearTimeout(t);
  }, [texto]);
  const avisar = useCallback((t: string) => setTexto(t), []);
  const elemento = texto ? <div className="aviso" role="status">{texto}</div> : null;
  return { avisar, elemento };
}
