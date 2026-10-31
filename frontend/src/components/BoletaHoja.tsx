import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import { useSesion } from '../auth';
import { NOMBRE_ESTADO, NOMBRE_ESTADO_PAGO, NOMBRE_METODO, fechaHora, leerPlata, plata } from '../format';
import type { Boleta, MetodoPago, Rifa } from '../types';
import { linkWhatsApp, mensajeComprobante, mensajeRecordatorio } from '../whatsapp';
import Hoja from './Hoja';

interface Props {
  rifa: Rifa;
  numero: number;
  alCerrar: () => void;
  /** Se llama después de cualquier cambio, para refrescar el tablero. */
  alCambiar?: () => void;
}

export default function BoletaHoja({ rifa, numero, alCerrar, alCambiar }: Props) {
  const { esAdmin } = useSesion();
  const [boleta, setBoleta] = useState<Boleta | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const cargar = useCallback(() => {
    api.get<Boleta>(`/api/rifas/${rifa.id}/boletas/${numero}`).then(setBoleta, (e) => setError(e.message));
  }, [rifa.id, numero]);

  useEffect(cargar, [cargar]);

  /** Ejecuta una acción que devuelve la boleta actualizada. */
  const accion = async (fn: () => Promise<Boleta>) => {
    setOcupado(true);
    setError(null);
    try {
      setBoleta(await fn());
      alCambiar?.();
      return true;
    } catch (e) {
      setError((e as Error).message);
      // Si otro vendedor la tomó mientras tanto, mostrar cómo quedó
      cargar();
      return false;
    } finally {
      setOcupado(false);
    }
  };

  const numeroTexto = boleta?.numeroTexto ?? String(numero).padStart(rifa.cifras, '0');

  return (
    <Hoja
      alCerrar={alCerrar}
      titulo={
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span className="numero-grande">{numeroTexto}</span>
          {boleta && <span className={`estado ${boleta.estado}`}>{NOMBRE_ESTADO[boleta.estado]}</span>}
        </div>
      }
    >
      {error && <div className="error">{error}</div>}
      {!boleta ? (
        !error && <div className="vacio">Cargando…</div>
      ) : boleta.venta === null ? (
        rifa.activa ? (
          <FormApartar rifa={rifa} ocupado={ocupado} alApartar={(datos) => accion(() =>
            api.post<Boleta>(`/api/rifas/${rifa.id}/boletas/${numero}/apartar`, datos))} />
        ) : (
          <div className="vacio">Este número no se vendió.</div>
        )
      ) : (
        <Ocupada rifa={rifa} boleta={boleta} esAdmin={esAdmin} ocupado={ocupado} accion={accion} />
      )}
      {boleta && boleta.historial.length > 0 && (
        <div className="tarjeta">
          <h2>Antes la tuvieron</h2>
          <ul className="lista">
            {boleta.historial.map((h, i) => (
              <li key={i}>
                <strong>{h.comprador}</strong> · {h.celular}
                <div className="nota">
                  Liberada {fechaHora(h.liberadaEn)}{h.liberadaPor ? ` por ${h.liberadaPor}` : ''}
                  {h.abonado > 0 ? ` · había abonado ${plata(h.abonado)}` : ''}
                  {h.motivo ? ` · ${h.motivo}` : ''}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Hoja>
  );
}

interface DatosApartar {
  nombre: string;
  celular: string;
  ubicacion: string;
  abono: { valor: number; metodo: MetodoPago } | null;
}

function FormApartar({ rifa, ocupado, alApartar }: { rifa: Rifa; ocupado: boolean; alApartar: (d: DatosApartar) => Promise<boolean> }) {
  const [nombre, setNombre] = useState('');
  const [celular, setCelular] = useState('');
  const [ubicacion, setUbicacion] = useState('');
  const [conAbono, setConAbono] = useState(false);
  const [valor, setValor] = useState(String(rifa.precio));
  const [metodo, setMetodo] = useState<MetodoPago>('EFECTIVO');

  const enviar = (e: FormEvent) => {
    e.preventDefault();
    alApartar({ nombre, celular, ubicacion, abono: conAbono ? { valor: leerPlata(valor), metodo } : null });
  };

  return (
    <form className="tarjeta campos" onSubmit={enviar}>
      <h2>Apartar este número</h2>
      <div className="campo">
        <label htmlFor="nombre">Nombre del comprador</label>
        <input id="nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required autoComplete="off" />
      </div>
      <div className="campo">
        <label htmlFor="celular">Celular (WhatsApp)</label>
        <input id="celular" value={celular} onChange={(e) => setCelular(e.target.value)} required inputMode="tel" autoComplete="off" />
      </div>
      <div className="campo">
        <label htmlFor="ubicacion">Pueblo o dirección</label>
        <input id="ubicacion" value={ubicacion} onChange={(e) => setUbicacion(e.target.value)} autoComplete="off" />
      </div>
      <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontWeight: 600 }}>
        <input type="checkbox" checked={conAbono} onChange={(e) => setConAbono(e.target.checked)} style={{ width: 20, height: 20 }} />
        Ya dio plata
      </label>
      {conAbono && (
        <div className="fila">
          <div className="campo">
            <label htmlFor="valor">Valor</label>
            <input id="valor" value={valor} onChange={(e) => setValor(e.target.value)} inputMode="numeric" />
          </div>
          <div className="campo">
            <label htmlFor="metodo">Cómo pagó</label>
            <select id="metodo" value={metodo} onChange={(e) => setMetodo(e.target.value as MetodoPago)}>
              <option value="EFECTIVO">Efectivo</option>
              <option value="TRANSFERENCIA">Transferencia</option>
            </select>
          </div>
        </div>
      )}
      <button className="boton oro ancho" disabled={ocupado}>{ocupado ? 'Guardando…' : 'Apartar'}</button>
      <p className="nota" style={{ margin: 0 }}>Si el celular ya está registrado, se usa el mismo comprador.</p>
    </form>
  );
}

function Ocupada({ rifa, boleta, esAdmin, ocupado, accion }: {
  rifa: Rifa;
  boleta: Boleta;
  esAdmin: boolean;
  ocupado: boolean;
  accion: (fn: () => Promise<Boleta>) => Promise<boolean>;
}) {
  const venta = boleta.venta!;
  const falta = venta.saldo - venta.porConfirmar;
  const [valor, setValor] = useState(String(Math.max(falta, 0)));
  const [metodo, setMetodo] = useState<MetodoPago>('EFECTIVO');
  const [liberando, setLiberando] = useState(false);
  const [motivo, setMotivo] = useState('');

  useEffect(() => setValor(String(Math.max(falta, 0))), [falta]);

  const datosMensaje = {
    nombre: venta.comprador.nombre,
    numeroTexto: boleta.numeroTexto,
    estado: boleta.estado,
    pagado: venta.pagado,
    saldo: venta.saldo,
  };

  const pagar = (e: FormEvent) => {
    e.preventDefault();
    accion(() => api.post<Boleta>(`/api/rifas/${rifa.id}/boletas/${boleta.numero}/pagos`, { valor: leerPlata(valor), metodo }));
  };

  const liberar = async () => {
    const ok = await accion(() => api.post<Boleta>(`/api/rifas/${rifa.id}/boletas/${boleta.numero}/liberar`, { motivo }));
    if (ok) setLiberando(false);
  };

  return (
    <>
      <div className="tarjeta">
        <dl className="datos">
          <dt>Comprador</dt>
          <dd>{venta.comprador.nombre}</dd>
          <dt>Celular</dt>
          <dd><a href={`tel:${venta.comprador.celular}`}>{venta.comprador.celular}</a></dd>
          {venta.comprador.ubicacion && (<><dt>Pueblo</dt><dd>{venta.comprador.ubicacion}</dd></>)}
          <dt>Vendió</dt>
          <dd>{venta.vendedor} · {fechaHora(venta.creadaEn)}</dd>
        </dl>
      </div>

      <div className="cifras">
        <div className="cifra"><span>Pagado</span><strong>{plata(venta.pagado)}</strong></div>
        <div className="cifra"><span>Falta</span><strong>{plata(venta.saldo)}</strong></div>
        {venta.porConfirmar > 0 && (
          <div className="cifra" style={{ gridColumn: '1 / -1', background: '#fde7b0' }}>
            <span>Transferencia por confirmar</span><strong>{plata(venta.porConfirmar)}</strong>
          </div>
        )}
      </div>

      <div className="botones">
        <a className="boton whatsapp" target="_blank" rel="noreferrer"
           href={linkWhatsApp(venta.comprador.celular, mensajeComprobante(rifa, datosMensaje))}>
          Mandar comprobante
        </a>
        {venta.saldo > 0 && (
          <a className="boton whatsapp" target="_blank" rel="noreferrer"
             href={linkWhatsApp(venta.comprador.celular, mensajeRecordatorio(rifa, datosMensaje))}>
            Recordar pago
          </a>
        )}
      </div>

      {rifa.activa && falta > 0 && (
        <form className="tarjeta campos" onSubmit={pagar}>
          <h2>Registrar abono</h2>
          <div className="fila">
            <div className="campo">
              <label htmlFor="valor">Valor</label>
              <input id="valor" value={valor} onChange={(e) => setValor(e.target.value)} inputMode="numeric" />
            </div>
            <div className="campo">
              <label htmlFor="metodo">Cómo pagó</label>
              <select id="metodo" value={metodo} onChange={(e) => setMetodo(e.target.value as MetodoPago)}>
                <option value="EFECTIVO">Efectivo</option>
                <option value="TRANSFERENCIA">Transferencia</option>
              </select>
            </div>
          </div>
          {metodo === 'TRANSFERENCIA' && !esAdmin && (
            <p className="nota" style={{ margin: 0 }}>Queda por confirmar hasta que Julián o Adriana la vean en la cuenta.</p>
          )}
          <button className="boton verde ancho" disabled={ocupado}>{ocupado ? 'Guardando…' : 'Guardar abono'}</button>
        </form>
      )}

      {venta.pagos.length > 0 && (
        <div className="tarjeta">
          <h2>Pagos</h2>
          <ul className="lista">
            {venta.pagos.map((p) => (
              <li key={p.id}>
                <div className={`pago ${p.estado}`}>
                  <div>
                    <div>{NOMBRE_METODO[p.metodo]} <span className={`etiqueta ${p.estado}`}>{NOMBRE_ESTADO_PAGO[p.estado]}</span></div>
                    <div className="nota">{fechaHora(p.registradoEn)} · {p.registradoPor}{p.revisadoPor ? ` · revisó ${p.revisadoPor}` : ''}</div>
                  </div>
                  <span className="valor">{plata(p.valor)}</span>
                </div>
                {esAdmin && rifa.activa && p.estado !== 'RECHAZADO' && (
                  <div className="botones" style={{ marginTop: 8 }}>
                    {p.estado === 'POR_CONFIRMAR' && (
                      <button className="boton verde chico" disabled={ocupado}
                              onClick={() => accion(() => api.post<Boleta>(`/api/pagos/${p.id}/confirmar`))}>
                        Sí llegó
                      </button>
                    )}
                    <button className="boton claro chico" disabled={ocupado}
                            onClick={() => {
                              const pregunta = p.estado === 'POR_CONFIRMAR' ? '¿Marcar que esta transferencia no llegó?' : '¿Anular este pago?';
                              if (window.confirm(pregunta)) accion(() => api.post<Boleta>(`/api/pagos/${p.id}/rechazar`));
                            }}>
                      {p.estado === 'POR_CONFIRMAR' ? 'No llegó' : 'Anular'}
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {esAdmin && rifa.activa && (
        liberando ? (
          <div className="tarjeta campos">
            <h2>Liberar el {boleta.numeroTexto}</h2>
            <p className="nota" style={{ margin: 0 }}>
              El número queda libre para otro comprador.
              {venta.pagado > 0 ? ` Lo abonado (${plata(venta.pagado)}) se pierde y queda en el historial.` : ''}
            </p>
            <div className="campo">
              <label htmlFor="motivo">Motivo (opcional)</label>
              <input id="motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="No pagó a tiempo" />
            </div>
            <div className="botones">
              <button className="boton claro" onClick={() => setLiberando(false)}>Cancelar</button>
              <button className="boton peligro" disabled={ocupado} onClick={liberar}>Liberar</button>
            </div>
          </div>
        ) : (
          <button className="boton claro ancho" onClick={() => setLiberando(true)}>Liberar número</button>
        )
      )}
    </>
  );
}
