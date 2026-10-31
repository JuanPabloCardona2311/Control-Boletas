import { fecha, plata, rellenar } from '../format';
import type { Rifa, Tablero } from '../types';

/** Dibuja una historia de Instagram (1080x1920) con la cuadrícula: blanco = libre, café = vendido. */
export async function generarImagenLibres(rifa: Rifa, tablero: Tablero) {
  await document.fonts.load('64px Rye');
  const ancho = 1080;
  const alto = 1920;
  const lienzo = document.createElement('canvas');
  lienzo.width = ancho;
  lienzo.height = alto;
  const c = lienzo.getContext('2d')!;

  c.fillStyle = '#3B2416';
  c.fillRect(0, 0, ancho, alto);
  c.textAlign = 'center';
  c.fillStyle = '#E9C977';
  c.font = '64px Rye';
  c.fillText('El Mular De Oro', ancho / 2, 110);
  c.fillStyle = '#F4EBD9';
  c.font = '600 44px "Work Sans", sans-serif';
  c.fillText(`Quedan ${tablero.conteo.libres} números`, ancho / 2, 180);
  c.font = '32px "Work Sans", sans-serif';
  c.fillText(`Boleta ${plata(rifa.precio)} · juega el ${fecha(rifa.fechaSorteo)}`, ancho / 2, 230);

  const ocupados = new Set(tablero.ocupadas.map((o) => o.numero));
  const columnas = rifa.cantidadNumeros <= 100 ? 10 : 25;
  const filas = Math.ceil(rifa.cantidadNumeros / columnas);
  const margen = 40;
  const arriba = 280;
  const espacio = 4;
  const celda = Math.min((ancho - margen * 2) / columnas, (alto - arriba - 140) / filas);
  const inicioX = (ancho - celda * columnas) / 2;

  c.font = `600 ${Math.floor(celda * 0.38)}px "Work Sans", sans-serif`;
  c.textBaseline = 'middle';
  for (let n = 0; n < rifa.cantidadNumeros; n++) {
    const x = inicioX + (n % columnas) * celda;
    const y = arriba + Math.floor(n / columnas) * celda;
    const libre = !ocupados.has(n);
    c.fillStyle = libre ? '#FFFDF8' : '#5A3D2B';
    c.fillRect(x + espacio / 2, y + espacio / 2, celda - espacio, celda - espacio);
    if (libre) {
      c.fillStyle = '#3B2416';
      c.fillText(rellenar(n, rifa.cifras), x + celda / 2, y + celda / 2 + 1);
    }
  }

  c.textBaseline = 'alphabetic';
  c.fillStyle = '#E9C977';
  c.font = '600 34px "Work Sans", sans-serif';
  c.fillText('Los cuadros blancos están libres. ¡Escríbanos!', ancho / 2, alto - 60);

  const enlace = document.createElement('a');
  enlace.download = `numeros-libres-${new Date().toISOString().slice(0, 10)}.png`;
  enlace.href = lienzo.toDataURL('image/png');
  enlace.click();
}
