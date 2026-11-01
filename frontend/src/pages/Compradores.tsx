import { useEffect, useState } from 'react';
import { api } from '../api';
import BoletaHoja from '../components/BoletaHoja';
import { NOMBRE_ESTADO, plata } from '../format';
import type { CompradorConBoletas, Rifa } from '../types';

export default function Compradores({ rifa }: { rifa: Rifa }) {
  const [q, setQ] = useState('');
  const [resultados, setResultados] = useState<CompradorConBoletas[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [abierta, setAbierta] = useState<number | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const texto = q.trim();
    if (texto.length < 2) {
      setResultados(null);
      return;
    }
    const t = setTimeout(() => {
      api.get<CompradorConBoletas[]>(`/api/rifas/${rifa.id}/compradores?q=${encodeURIComponent(texto)}`)
        .then((r) => { setResultados(r); setError(null); }, (e) => setError(e.message));
    }, 300);
    return () => clearTimeout(t);
  }, [q, rifa.id, version]);

  return (
    <main className="contenido">
      <div className="campo">
        <label htmlFor="q">Buscar por nombre o celular</label>
        <input id="q" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Ej: Pedro o 311" autoComplete="off" />
      </div>
      {error && <div className="error">{error}</div>}
      {resultados === null ? (
        <div className="vacio">Escriba al menos 2 letras o números.</div>
      ) : resultados.length === 0 ? (
        <div className="vacio">No se encontró a nadie.</div>
      ) : (
        resultados.map((r) => (
          <div key={r.comprador.id} className="tarjeta">
            <h2>{r.comprador.nombre}</h2>
            <div className="nota">{r.comprador.celular}{r.comprador.ubicacion ? ` · ${r.comprador.ubicacion}` : ''}</div>
            {r.boletas.length === 0 ? (
              <p className="nota">No tiene números en esta rifa.</p>
            ) : (
              <ul className="lista">
                {r.boletas.map((b) => (
                  <li key={b.numero} onClick={() => setAbierta(b.numero)} style={{ cursor: 'pointer', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span><strong style={{ fontSize: 20 }}>{b.numeroTexto}</strong> <span className={`estado ${b.estado}`}>{NOMBRE_ESTADO[b.estado]}</span></span>
                    {b.saldo > 0 && <span className="nota">debe {plata(b.saldo)}</span>}
                  </li>
                ))}
              </ul>
            )}
          </div>
        ))
      )}
      {abierta !== null && (
        <BoletaHoja rifa={rifa} numero={abierta} alCerrar={() => setAbierta(null)} alCambiar={() => setVersion((v) => v + 1)} />
      )}
    </main>
  );
}
