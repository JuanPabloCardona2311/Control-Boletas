import { fecha, plata } from './format';
import type { EstadoBoleta, Rifa } from './types';

/** Link de WhatsApp con el mensaje listo; el vendedor solo oprime enviar. */
export function linkWhatsApp(celular: string, mensaje: string): string {
  let numero = celular.replace(/\D/g, '');
  if (numero.length === 10 && numero.startsWith('3')) numero = '57' + numero;
  return `https://wa.me/${numero}?text=${encodeURIComponent(mensaje)}`;
}

interface DatosBoleta {
  nombre: string;
  numeroTexto: string;
  estado: EstadoBoleta;
  pagado: number;
  saldo: number;
}

function primerNombre(nombre: string) {
  return nombre.trim().split(/\s+/)[0];
}

export function mensajeComprobante(rifa: Rifa, b: DatosBoleta): string {
  const detalle =
    b.estado === 'PAGADA'
      ? `quedó *pagada completa* (${plata(rifa.precio)}). ¡Ya está jugando!`
      : b.estado === 'ABONADA'
        ? `quedó con un abono de ${plata(b.pagado)}. Le falta ${plata(b.saldo)}.`
        : `quedó *apartada* a su nombre. El valor es ${plata(rifa.precio)}.`;
  return [
    `Hola ${primerNombre(b.nombre)}, le saluda El Mular De Oro.`,
    `Su boleta *${b.numeroTexto}* ${detalle}`,
    `Juega el ${fecha(rifa.fechaSorteo)} con la ${rifa.loteria}.`,
    'Gracias por su compra y ¡mucha suerte!',
  ].join('\n');
}

export function mensajeRecordatorio(rifa: Rifa, b: DatosBoleta): string {
  return [
    `Hola ${primerNombre(b.nombre)}, le saluda El Mular De Oro.`,
    `Le recordamos que su boleta *${b.numeroTexto}* tiene un saldo de ${plata(b.saldo)}.`,
    `El sorteo es el ${fecha(rifa.fechaSorteo)} con la ${rifa.loteria}. Recuerde que boleta sin cancelar no juega.`,
    rifa.responsables ? `Puede pagar en efectivo o por transferencia a ${rifa.responsables}.` : 'Puede pagar en efectivo o por transferencia.',
  ].join('\n');
}
