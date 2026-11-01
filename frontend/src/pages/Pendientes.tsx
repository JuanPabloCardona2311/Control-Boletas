import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { useSesion } from '../auth';
import { useAviso } from '../components/Aviso';
import BoletaHoja from '../components/BoletaHoja';
import { fecha, fechaHora, plata } from '../format';
import type { Deudor, Deudores, Rifa, TransferenciaPendiente } from '../types';
import { linkWhatsApp, mensajeRecordatorio } from '../whatsapp';

export default function Pendientes({ rifa }: { rifa: Rifa }) {
  const { esAdmin } = useSesion();
  const [deudores, setDeudores] = useState<Deudores | null>(null);
  const [transferencias, setTransferencias] = useState<TransferenciaPendiente[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [abierta, setAbierta] = useState<number | null>(null);
  const { avisar, elemento } = useAviso();

  const cargar = useCallback(() => {
    api.get<Deudores>(`/api/rifas/${rifa.id}/deudores`).then(setDeudores, (e) => setError(e.message));
    if (esAdmin) {
      api.get<TransferenciaPendiente[]>(`/api/rifas/${rifa.id}/transferencias-pendientes`).then(setTransferencias, (e) => setError(e.message));
    }
  }, [rifa.id, esAdmin]);

  useEffect(cargar, [cargar]);

  const revisar = async (t: TransferenciaPendiente, llego: boolean) => {
    if (!llego && !window.confirm(`¿Marcar que la transferencia de ${plata(t.valor)} del ${t.numeroTexto} no llegó?`)) return;
    try {
      await api.post(`/api/pagos/${t.pagoId}/${llego ? 'confirmar' : 'rechazar'}`);
      avisar(llego ? `Confirmado el pago del ${t.numeroTexto}` : `Rechazada la transferencia del ${t.numeroTexto}`);
      cargar();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  // Una apartada con transferencia por confirmar no se debe liberar: va con las que ya dieron plata
  const sinAbono = deudores?.deudores.filter((d) => d.estado === 'APARTADA' && d.porConfirmar === 0) ?? [];
  const abonadas = deudores?.deudores.filter((d) => d.estado === 'ABONADA' || d.porConfirmar > 0) ?? [];

  return (
    <main className="contenido">
      {error && <div className="error">{error}</div>}

      {esAdmin && transferencias.length > 0 && (
        <div className="tarjeta">
          <h2>Transferencias por confirmar ({transferencias.length})</h2>
          <p className="nota" style={{ marginTop: 0 }}>Revise en Bancolombia si llegaron.</p>
          <ul className="lista">
            {transferencias.map((t) => (
              <li key={t.pagoId}>
                <div className="pago">
                  <div>
                    <strong>{t.numeroTexto}</strong> · {t.comprador}
                    <div className="nota">{fechaHora(t.registradoEn)} · registró {t.registradoPor}{t.nota ? ` · ${t.nota}` : ''}</div>
                  </div>
                  <span className="valor">{plata(t.valor)}</span>
                </div>
                <div className="botones" style={{ marginTop: 8 }}>
                  <button className="boton verde chico" onClick={() => revisar(t, true)}>Sí llegó</button>
                  <button className="boton claro chico" onClick={() => revisar(t, false)}>No llegó</button>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      {deudores && (
        <>
          <div className={`banda ${deudores.tocaRecordar ? 'alerta' : 'info'}`}>
            {deudores.tocaLiberar
              ? `Desde el ${fecha(deudores.fechaLiberacion)} se pueden liberar las apartadas sin ningún abono. A las abonadas se les espera hasta el sorteo (${fecha(deudores.fechaSorteo)}).`
              : deudores.tocaRecordar
                ? `Ya toca recordarles a todos los que deben. El ${fecha(deudores.fechaLiberacion)} se liberan las apartadas sin abono.`
                : `Los recordatorios empiezan el ${fecha(deudores.fechaRecordatorio)} y la liberación de apartadas sin abono el ${fecha(deudores.fechaLiberacion)}.`}
          </div>

          <ListaDeudores titulo="Apartadas sin abono" vacia="Nadie tiene números apartados sin pagar." deudores={sinAbono}
                         rifa={rifa} abrir={setAbierta} resaltar={deudores.tocaLiberar && esAdmin} />
          <ListaDeudores titulo="Ya dieron plata (deben una parte)" vacia="Nadie debe una parte." deudores={abonadas}
                         rifa={rifa} abrir={setAbierta} resaltar={false} />
        </>
      )}

      {abierta !== null && <BoletaHoja rifa={rifa} numero={abierta} alCerrar={() => setAbierta(null)} alCambiar={cargar} />}
      {elemento}
    </main>
  );
}

function ListaDeudores({ titulo, vacia, deudores, rifa, abrir, resaltar }: {
  titulo: string;
  vacia: string;
  deudores: Deudor[];
  rifa: Rifa;
  abrir: (n: number) => void;
  resaltar: boolean;
}) {
  return (
    <div className="tarjeta">
      <h2>{titulo} ({deudores.length})</h2>
      {resaltar && deudores.length > 0 && (
        <p className="nota" style={{ marginTop: 0 }}>Toque un número para liberarlo.</p>
      )}
      {deudores.length === 0 ? (
        <p className="nota">{vacia}</p>
      ) : (
        <ul className="lista">
          {deudores.map((d) => (
            <li key={d.numero}>
              <div className="pago" onClick={() => abrir(d.numero)} style={{ cursor: 'pointer' }}>
                <div>
                  <strong style={{ fontSize: 18 }}>{d.numeroTexto}</strong> · {d.comprador}
                  <div className="nota">
                    {d.ubicacion ? `${d.ubicacion} · ` : ''}vendió {d.vendedor}
                    {d.porConfirmar > 0 ? ` · ${plata(d.porConfirmar)} por confirmar` : ''}
                  </div>
                </div>
                <span className="valor">{plata(d.saldo)}</span>
              </div>
              <a className="boton whatsapp chico" style={{ marginTop: 8 }} target="_blank" rel="noreferrer"
                 href={linkWhatsApp(d.celular, mensajeRecordatorio(rifa, {
                   nombre: d.comprador, numeroTexto: d.numeroTexto, estado: d.estado, pagado: d.pagado, saldo: d.saldo,
                 }))}>
                Recordar por WhatsApp
              </a>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
