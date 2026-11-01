import { useEffect, useState } from 'react';
import { api } from '../api';
import { fecha, plata } from '../format';
import type { Resumen as ResumenDto, ResultadoSorteo, Rifa, Tablero } from '../types';
import { generarImagenLibres } from './imagenLibres';

export default function Resumen({ rifa }: { rifa: Rifa }) {
  const [resumen, setResumen] = useState<ResumenDto | null>(null);
  const [sorteo, setSorteo] = useState<ResultadoSorteo | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [generando, setGenerando] = useState(false);

  useEffect(() => {
    api.get<ResumenDto>(`/api/rifas/${rifa.id}/resumen`).then(setResumen, (e) => setError(e.message));
    api.get<ResultadoSorteo>(`/api/rifas/${rifa.id}/ganadores`).then(setSorteo, () => {});
  }, [rifa.id]);

  const imagen = async () => {
    setGenerando(true);
    try {
      const tablero = await api.get<Tablero>(`/api/rifas/${rifa.id}/tablero`);
      await generarImagenLibres(rifa, tablero);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setGenerando(false);
    }
  };

  return (
    <main className="contenido">
      {error && <div className="error">{error}</div>}
      {resumen && (
        <>
          <div className="tarjeta">
            <h2>Boletas</h2>
            <div className="cifras">
              <div className="cifra"><span>Vendidas</span><strong>{resumen.vendidas}</strong></div>
              <div className="cifra"><span>Libres</span><strong>{resumen.libres}</strong></div>
            </div>
          </div>
          <div className="tarjeta">
            <h2>Plata</h2>
            <div className="cifras">
              <div className="cifra"><span>Recogido</span><strong>{plata(resumen.recaudado)}</strong></div>
              <div className="cifra"><span>Falta por cobrar</span><strong>{plata(resumen.porCobrar)}</strong></div>
              <div className="cifra"><span>Transferencias por confirmar</span><strong>{plata(resumen.porConfirmar)}</strong></div>
              <div className="cifra"><span>Abonos de números liberados</span><strong>{plata(resumen.abonosPerdidos)}</strong></div>
            </div>
            <p className="nota">Si se venden todas: {plata(rifa.precio * rifa.cantidadNumeros)}.</p>
          </div>
          {resumen.efectivoPorVendedor.length > 0 && (
            <div className="tarjeta">
              <h2>Efectivo recibido por cada uno</h2>
              <ul className="lista">
                {resumen.efectivoPorVendedor.map((v) => (
                  <li key={v.vendedorId} className="pago"><span>{v.vendedor}</span><span className="valor">{plata(v.total)}</span></li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}

      <div className="tarjeta campos">
        <h2>Imagen para Instagram</h2>
        <p className="nota" style={{ margin: 0 }}>Una imagen tamaño historia con los números que quedan libres, sin nombres de nadie.</p>
        {rifa.cantidadNumeros <= 1000 ? (
          <button className="boton oro" onClick={imagen} disabled={generando}>{generando ? 'Armando…' : 'Descargar imagen'}</button>
        ) : (
          <p className="nota" style={{ margin: 0 }}>Disponible para rifas de hasta 1000 números.</p>
        )}
      </div>

      {sorteo?.resultado && (
        <div className="tarjeta">
          <h2>Resultado: {sorteo.resultado}</h2>
          <ul className="lista">
            {sorteo.ganadores.map((g, i) => (
              <li key={i}>
                <strong>{g.premio}</strong> · cifras {g.cifras}
                <div className="nota">
                  {g.numero === null ? 'Ver a mano: juega con menos cifras que la boleta'
                    : g.comprador ? `${g.comprador} (${g.celular}) · ${g.juega ? 'pagada, ¡gana!' : 'no estaba pagada completa: no juega'}`
                    : 'El número no se vendió'}
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}
      <p className="nota" style={{ textAlign: 'center' }}>Sorteo el {fecha(rifa.fechaSorteo, true)} · {rifa.loteria}</p>
    </main>
  );
}
