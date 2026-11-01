import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { pedirConEtag } from '../api';
import BoletaHoja from '../components/BoletaHoja';
import { NOMBRE_ESTADO, rellenar } from '../format';
import type { EstadoBoleta, Ocupada, Rifa, Tablero } from '../types';

const CADA_MS = 6000;

type Filtro = 'TODOS' | EstadoBoleta | 'POR_CONFIRMAR';

export default function TableroPagina({ rifa }: { rifa: Rifa }) {
  const [tablero, setTablero] = useState<Tablero | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [filtro, setFiltro] = useState<Filtro>('TODOS');
  const [pagina, setPagina] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [abierta, setAbierta] = useState<number | null>(null);
  const etag = useRef<string | null>(null);

  const actualizar = useCallback(async () => {
    try {
      const res = await pedirConEtag<Tablero>(`/api/rifas/${rifa.id}/tablero`, etag.current);
      if (res) {
        etag.current = res.etag;
        setTablero(res.datos);
      }
      setError(null);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [rifa.id]);

  // Consulta cada pocos segundos mientras la app está a la vista, para ver lo que apartan los demás
  useEffect(() => {
    actualizar();
    const intervalo = setInterval(() => document.visibilityState === 'visible' && actualizar(), CADA_MS);
    const alVolver = () => document.visibilityState === 'visible' && actualizar();
    document.addEventListener('visibilitychange', alVolver);
    return () => {
      clearInterval(intervalo);
      document.removeEventListener('visibilitychange', alVolver);
    };
  }, [actualizar]);

  const ocupadas = useMemo(() => {
    const mapa = new Map<number, Ocupada>();
    tablero?.ocupadas.forEach((o) => mapa.set(o.numero, o));
    return mapa;
  }, [tablero]);

  const cantidad = rifa.cantidadNumeros;
  const paginas = Math.ceil(cantidad / 100);

  const estadoDe = (n: number): EstadoBoleta => ocupadas.get(n)?.estado ?? 'LIBRE';
  const cumple = (n: number) => {
    if (filtro === 'TODOS') return true;
    if (filtro === 'POR_CONFIRMAR') return ocupadas.get(n)?.porConfirmar ?? false;
    return estadoDe(n) === filtro;
  };

  const numeros = useMemo(() => {
    if (filtro === 'TODOS') return Array.from({ length: Math.min(100, cantidad) }, (_, i) => pagina * 100 + i);
    return Array.from({ length: cantidad }, (_, i) => i);
  }, [filtro, pagina, cantidad]);

  const visibles = filtro === 'TODOS' ? numeros : numeros.filter(cumple);

  const buscar = (e?: FormEvent) => {
    e?.preventDefault();
    const n = Number(busqueda);
    if (busqueda === '' || Number.isNaN(n) || n < 0 || n >= cantidad) return;
    setPagina(Math.floor(n / 100));
    setAbierta(n);
    setBusqueda('');
  };

  const porConfirmar = tablero?.ocupadas.filter((o) => o.porConfirmar).length ?? 0;

  return (
    <main className="contenido">
      <form className="buscador" onSubmit={buscar}>
        <input
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value.replace(/\D/g, '').slice(0, rifa.cifras))}
          placeholder={'0'.repeat(rifa.cifras)}
          inputMode="numeric"
          aria-label="Buscar número"
        />
        <button className="boton oro" disabled={busqueda === ''}>Ver</button>
      </form>

      {error && <div className="error">{error}</div>}

      {tablero && (
        <div className="conteo">
          {(['LIBRE', 'APARTADA', 'ABONADA', 'PAGADA'] as EstadoBoleta[]).map((e) => (
            <button key={e} className={filtro === e ? 'activo' : ''} onClick={() => setFiltro(filtro === e ? 'TODOS' : e)}>
              <strong>{tablero.conteo[({ LIBRE: 'libres', APARTADA: 'apartadas', ABONADA: 'abonadas', PAGADA: 'pagadas' } as const)[e]]}</strong>
              <span className={`estado ${e}`} style={{ padding: 0 }}>{NOMBRE_ESTADO[e]}s</span>
            </button>
          ))}
        </div>
      )}

      <div className="chips">
        <button className={`chip ${filtro === 'TODOS' ? 'activo' : ''}`} onClick={() => setFiltro('TODOS')}>Todos</button>
        <button className={`chip ${filtro === 'POR_CONFIRMAR' ? 'activo' : ''}`} onClick={() => setFiltro('POR_CONFIRMAR')}>
          Transferencias por confirmar{porConfirmar ? ` (${porConfirmar})` : ''}
        </button>
      </div>

      {filtro === 'TODOS' && paginas > 1 && (
        paginas <= 10 ? (
          <div className="chips">
            {Array.from({ length: paginas }, (_, i) => (
              <button key={i} className={`chip ${pagina === i ? 'activo' : ''}`} onClick={() => setPagina(i)}>
                {rellenar(i * 100, rifa.cifras)}
              </button>
            ))}
          </div>
        ) : (
          <div className="campo">
            <select value={pagina} onChange={(e) => setPagina(Number(e.target.value))} aria-label="Grupo de números">
              {Array.from({ length: paginas }, (_, i) => (
                <option key={i} value={i}>{rellenar(i * 100, rifa.cifras)} – {rellenar(i * 100 + 99, rifa.cifras)}</option>
              ))}
            </select>
          </div>
        )
      )}

      {!tablero ? (
        !error && <div className="vacio">Cargando números…</div>
      ) : visibles.length === 0 ? (
        <div className="vacio">No hay números en este grupo.</div>
      ) : (
        <div className="cuadricula">
          {visibles.map((n) => {
            const o = ocupadas.get(n);
            return (
              <button key={n} className={`celda ${o?.estado ?? 'LIBRE'}`} onClick={() => setAbierta(n)}
                      aria-label={`${rellenar(n, rifa.cifras)} ${NOMBRE_ESTADO[o?.estado ?? 'LIBRE']}`}>
                {rifa.cifras > 3 && filtro === 'TODOS' ? rellenar(n % 100, 2) : rellenar(n, rifa.cifras)}
                {o?.porConfirmar && <span className="reloj" />}
              </button>
            );
          })}
        </div>
      )}

      <div className="leyenda">
        {(['LIBRE', 'APARTADA', 'ABONADA', 'PAGADA'] as EstadoBoleta[]).map((e) => (
          <span key={e} className={`estado ${e}`} style={{ padding: 0 }}>{NOMBRE_ESTADO[e]}</span>
        ))}
        <span><span className="celda" style={{ display: 'inline-block', width: 10, height: 10, border: 0, background: 'var(--cafe)', borderRadius: '50%', verticalAlign: 'middle' }} /> Transferencia por confirmar</span>
      </div>

      {abierta !== null && (
        <BoletaHoja rifa={rifa} numero={abierta} alCerrar={() => setAbierta(null)} alCambiar={actualizar} />
      )}
    </main>
  );
}
