import type { EstadoBoleta, EstadoPago, MetodoPago } from './types';

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });

export function plata(valor: number): string {
  return pesos.format(valor);
}

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];

/** "2026-12-18" → "18 de diciembre" */
export function fecha(iso: string, conAnio = false): string {
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
  return `${d} de ${MESES[m - 1]}${conAnio ? ` de ${a}` : ''}`;
}

export function fechaHora(iso: string): string {
  return new Date(iso).toLocaleString('es-CO', { day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });
}

export function rellenar(numero: number, cifras: number): string {
  return String(numero).padStart(cifras, '0');
}

export const NOMBRE_ESTADO: Record<EstadoBoleta, string> = {
  LIBRE: 'Libre',
  APARTADA: 'Apartada',
  ABONADA: 'Abonada',
  PAGADA: 'Pagada',
};

export const NOMBRE_METODO: Record<MetodoPago, string> = {
  EFECTIVO: 'Efectivo',
  TRANSFERENCIA: 'Transferencia',
};

export const NOMBRE_ESTADO_PAGO: Record<EstadoPago, string> = {
  CONFIRMADO: 'Confirmado',
  POR_CONFIRMAR: 'Por confirmar',
  RECHAZADO: 'Rechazado',
};

/** Convierte lo que se escribe en el campo de plata ("20.000") a número. */
export function leerPlata(texto: string): number {
  return Number(texto.replace(/\D/g, '')) || 0;
}
