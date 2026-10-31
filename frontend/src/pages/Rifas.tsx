import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import { useSesion } from '../auth';
import { useAviso } from '../components/Aviso';
import { fecha, leerPlata, plata } from '../format';
import type { Premio, ResultadoSorteo, Rifa } from '../types';

export default function Rifas() {
  const { rifa: activa, recargarRifa } = useSesion();
  const [rifas, setRifas] = useState<Rifa[]>([]);
  const [modo, setModo] = useState<'ver' | 'crear' | 'editar'>('ver');
  const [error, setError] = useState<string | null>(null);
  const { avisar, elemento } = useAviso();

  const cargar = () => api.get<Rifa[]>('/api/rifas').then(setRifas, (e) => setError(e.message));
  useEffect(() => { cargar(); }, []);

  const guardar = async (datos: Record<string, unknown>) => {
    if (modo === 'crear') {
      await api.post('/api/rifas', datos);
      avisar('Rifa creada');
    } else if (activa) {
      await api.put(`/api/rifas/${activa.id}`, datos);
      avisar('Cambios guardados');
    }
    await recargarRifa();
    await cargar();
    setModo('ver');
  };

  if (modo !== 'ver') {
    return (
      <main className="contenido">
        <FormRifa inicial={modo === 'editar' ? activa : null} alGuardar={guardar} alCancelar={() => setModo('ver')} />
        {elemento}
      </main>
    );
  }

  return (
    <main className="contenido">
      {error && <div className="error">{error}</div>}
      {activa && (
        <div className="tarjeta campos">
          <h2>Rifa activa: {activa.nombre}</h2>
          <dl className="datos">
            <dt>Números</dt><dd>{activa.cantidadNumeros} ({'0'.repeat(activa.cifras)} a {'9'.repeat(activa.cifras)})</dd>
            <dt>Boleta</dt><dd>{plata(activa.precio)}</dd>
            <dt>Juega</dt><dd>{fecha(activa.fechaSorteo, true)} · {activa.loteria}</dd>
            <dt>Recordar</dt><dd>{activa.diasRecordatorio} días antes ({fecha(activa.fechaRecordatorio)})</dd>
            <dt>Liberar</dt><dd>{activa.diasLiberacion} días antes ({fecha(activa.fechaLiberacion)})</dd>
            {activa.responsables && (<><dt>Pagos a</dt><dd>{activa.responsables}</dd></>)}
            {activa.premios.map((p, i) => (
              <span key={i} style={{ display: 'contents' }}>
                <dt>Premio {i + 1}</dt>
                <dd>{p.descripcion} · {p.cifras} {p.posicion === 'PRIMERAS' ? 'primeras' : 'últimas'}{p.valorEfectivo ? ` o ${plata(p.valorEfectivo)}` : ''}</dd>
              </span>
            ))}
          </dl>
          <button className="boton claro" onClick={() => setModo('editar')}>Editar datos</button>
        </div>
      )}
      {activa && <Resultado rifa={activa} />}

      <div className="tarjeta campos">
        <h2>Nueva rifa</h2>
        <p className="nota" style={{ margin: 0 }}>
          {activa
            ? 'Al crear una rifa nueva, la actual se archiva con todas sus ventas y pagos, y la nueva arranca con todos los números libres.'
            : 'Cree la rifa para empezar a vender.'}
        </p>
        <button className="boton oro" onClick={() => setModo('crear')}>Crear rifa nueva</button>
      </div>

      {rifas.filter((r) => !r.activa).length > 0 && (
        <div className="tarjeta">
          <h2>Rifas anteriores</h2>
          <ul className="lista">
            {rifas.filter((r) => !r.activa).map((r) => (
              <li key={r.id}>
                <strong>{r.nombre}</strong>
                <div className="nota">{fecha(r.fechaSorteo, true)} · {r.cantidadNumeros} números · {plata(r.precio)}{r.resultado ? ` · resultado ${r.resultado}` : ''}</div>
              </li>
            ))}
          </ul>
        </div>
      )}
      {elemento}
    </main>
  );
}

function Resultado({ rifa }: { rifa: Rifa }) {
  const [resultado, setResultado] = useState(rifa.resultado ?? '');
  const [sorteo, setSorteo] = useState<ResultadoSorteo | null>(null);
  const [error, setError] = useState<string | null>(null);

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      setSorteo(await api.post<ResultadoSorteo>(`/api/rifas/${rifa.id}/resultado`, { resultado }));
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <form className="tarjeta campos" onSubmit={enviar}>
      <h2>Resultado del sorteo</h2>
      <p className="nota" style={{ margin: 0 }}>Escriba el número del premio mayor de la {rifa.loteria} cuando juegue.</p>
      {error && <div className="error">{error}</div>}
      <div className="fila">
        <div className="campo">
          <input value={resultado} onChange={(e) => setResultado(e.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" placeholder="1234" aria-label="Resultado" />
        </div>
        <button className="boton" style={{ flex: 'none' }}>Ver ganadores</button>
      </div>
      {sorteo?.ganadores.map((g, i) => (
        <div key={i} className="banda info">
          <strong>{g.premio}</strong> ({g.cifras}):{' '}
          {g.numero === null ? 'ver a mano' : g.comprador ? `${g.comprador} · ${g.celular} · ${g.juega ? 'pagada, ¡gana!' : 'no pagó completa: no juega'}` : 'no se vendió'}
        </div>
      ))}
    </form>
  );
}

const PREMIO_VACIO: Premio = { descripcion: '', posicion: 'ULTIMAS', cifras: 3, valorEfectivo: null };

function FormRifa({ inicial, alGuardar, alCancelar }: {
  inicial: Rifa | null;
  alGuardar: (datos: Record<string, unknown>) => Promise<void>;
  alCancelar: () => void;
}) {
  const [nombre, setNombre] = useState(inicial?.nombre ?? '');
  const [cantidad, setCantidad] = useState(inicial?.cantidadNumeros ?? 1000);
  const [precio, setPrecio] = useState(String(inicial?.precio ?? 60000));
  const [loteria, setLoteria] = useState(inicial?.loteria ?? 'Lotería de Medellín');
  const [fechaSorteo, setFechaSorteo] = useState(inicial?.fechaSorteo ?? '');
  const [diasRecordatorio, setDiasRecordatorio] = useState(inicial?.diasRecordatorio ?? 14);
  const [diasLiberacion, setDiasLiberacion] = useState(inicial?.diasLiberacion ?? 7);
  const [responsables, setResponsables] = useState(inicial?.responsables ?? '');
  const [premios, setPremios] = useState<Premio[]>(inicial?.premios.length ? inicial.premios : [{ ...PREMIO_VACIO }]);
  const [error, setError] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);
  const cifras = String(cantidad - 1).length;

  const cambiarPremio = (i: number, cambio: Partial<Premio>) =>
    setPremios(premios.map((p, j) => (j === i ? { ...p, ...cambio } : p)));

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    if (!inicial && !window.confirm('¿Crear la rifa nueva? La rifa actual quedará archivada.')) return;
    setOcupado(true);
    setError(null);
    try {
      await alGuardar({
        nombre, precio: leerPlata(precio), loteria, fechaSorteo, diasRecordatorio, diasLiberacion, responsables,
        premios: premios.map((p) => ({ ...p, cifras: Math.min(p.cifras, cifras) })),
        ...(inicial ? {} : { cantidadNumeros: cantidad }),
      });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setOcupado(false);
    }
  };

  return (
    <form className="tarjeta campos" onSubmit={enviar}>
      <h2>{inicial ? 'Editar rifa' : 'Nueva rifa'}</h2>
      {error && <div className="error">{error}</div>}
      <div className="campo">
        <label htmlFor="nombre">Nombre</label>
        <input id="nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} placeholder="Gran Rifa diciembre 2026" required />
      </div>
      <div className="fila">
        <div className="campo">
          <label htmlFor="cantidad">Números</label>
          <select id="cantidad" value={cantidad} onChange={(e) => setCantidad(Number(e.target.value))} disabled={!!inicial}>
            <option value={100}>100 (00-99)</option>
            <option value={1000}>1000 (000-999)</option>
            <option value={10000}>10000 (0000-9999)</option>
          </select>
        </div>
        <div className="campo">
          <label htmlFor="precio">Precio boleta</label>
          <input id="precio" value={precio} onChange={(e) => setPrecio(e.target.value)} inputMode="numeric" required />
        </div>
      </div>
      <div className="fila">
        <div className="campo">
          <label htmlFor="loteria">Lotería</label>
          <input id="loteria" value={loteria} onChange={(e) => setLoteria(e.target.value)} required />
        </div>
        <div className="campo">
          <label htmlFor="fecha">Fecha del sorteo</label>
          <input id="fecha" type="date" value={fechaSorteo} onChange={(e) => setFechaSorteo(e.target.value)} required />
        </div>
      </div>
      <div className="fila">
        <div className="campo">
          <label htmlFor="dr">Recordar (días antes)</label>
          <input id="dr" type="number" min={0} max={90} value={diasRecordatorio} onChange={(e) => setDiasRecordatorio(Number(e.target.value))} />
        </div>
        <div className="campo">
          <label htmlFor="dl">Liberar (días antes)</label>
          <input id="dl" type="number" min={0} max={90} value={diasLiberacion} onChange={(e) => setDiasLiberacion(Number(e.target.value))} />
        </div>
      </div>
      <div className="campo">
        <label htmlFor="resp">A quién se paga (sale en el recordatorio)</label>
        <input id="resp" value={responsables} onChange={(e) => setResponsables(e.target.value)} placeholder="Julián o Adriana, cuenta Bancolombia" />
      </div>

      <h2 style={{ marginTop: 8 }}>Premios</h2>
      {premios.map((p, i) => (
        <div key={i} className="premio-editor">
          <div className="campo">
            <label>Premio {i + 1}</label>
            <input value={p.descripcion} onChange={(e) => cambiarPremio(i, { descripcion: e.target.value })} placeholder="Moto Honda XR 190" required />
          </div>
          <div className="fila">
            <div className="campo">
              <label>Juega con</label>
              <select value={p.posicion} onChange={(e) => cambiarPremio(i, { posicion: e.target.value as Premio['posicion'] })}>
                <option value="PRIMERAS">Primeras cifras</option>
                <option value="ULTIMAS">Últimas cifras</option>
              </select>
            </div>
            <div className="campo">
              <label>Cifras</label>
              <input type="number" min={1} max={cifras} value={p.cifras} onChange={(e) => cambiarPremio(i, { cifras: Number(e.target.value) })} />
            </div>
          </div>
          <div className="campo">
            <label>O en efectivo (opcional)</label>
            <input value={p.valorEfectivo ?? ''} inputMode="numeric"
                   onChange={(e) => cambiarPremio(i, { valorEfectivo: leerPlata(e.target.value) || null })} placeholder="15000000" />
          </div>
          {premios.length > 1 && (
            <button type="button" className="boton claro chico" onClick={() => setPremios(premios.filter((_, j) => j !== i))}>Quitar premio</button>
          )}
        </div>
      ))}
      <button type="button" className="boton claro" onClick={() => setPremios([...premios, { ...PREMIO_VACIO, cifras }])}>Agregar premio</button>

      <div className="botones">
        <button type="button" className="boton claro" onClick={alCancelar}>Cancelar</button>
        <button className="boton oro" disabled={ocupado}>{ocupado ? 'Guardando…' : inicial ? 'Guardar' : 'Crear rifa'}</button>
      </div>
    </form>
  );
}
