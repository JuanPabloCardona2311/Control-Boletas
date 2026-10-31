export type Rol = 'ADMIN' | 'VENDEDOR';
export type EstadoBoleta = 'LIBRE' | 'APARTADA' | 'ABONADA' | 'PAGADA';
export type MetodoPago = 'EFECTIVO' | 'TRANSFERENCIA';
export type EstadoPago = 'CONFIRMADO' | 'POR_CONFIRMAR' | 'RECHAZADO';
export type PosicionPremio = 'PRIMERAS' | 'ULTIMAS';

export interface Usuario {
  id: number;
  nombre: string;
  usuario: string;
  rol: Rol;
  celular: string | null;
  activo: boolean;
}

export interface Premio {
  id?: number | null;
  descripcion: string;
  posicion: PosicionPremio;
  cifras: number;
  valorEfectivo: number | null;
}

export interface Rifa {
  id: number;
  nombre: string;
  cantidadNumeros: number;
  cifras: number;
  precio: number;
  loteria: string;
  fechaSorteo: string;
  diasRecordatorio: number;
  diasLiberacion: number;
  fechaRecordatorio: string;
  fechaLiberacion: string;
  responsables: string | null;
  activa: boolean;
  resultado: string | null;
  premios: Premio[];
}

export interface Ocupada {
  numero: number;
  estado: EstadoBoleta;
  porConfirmar: boolean;
}

export interface Tablero {
  rifaId: number;
  cantidadNumeros: number;
  cifras: number;
  precio: number;
  conteo: { libres: number; apartadas: number; abonadas: number; pagadas: number };
  ocupadas: Ocupada[];
}

export interface Comprador {
  id: number;
  nombre: string;
  celular: string;
  ubicacion: string | null;
}

export interface Pago {
  id: number;
  valor: number;
  metodo: MetodoPago;
  estado: EstadoPago;
  nota: string | null;
  registradoPor: string;
  registradoEn: string;
  revisadoPor: string | null;
  revisadoEn: string | null;
}

export interface Venta {
  id: number;
  comprador: Comprador;
  vendedorId: number;
  vendedor: string;
  creadaEn: string;
  pagado: number;
  porConfirmar: number;
  saldo: number;
  pagos: Pago[];
}

export interface Liberada {
  comprador: string;
  celular: string;
  abonado: number;
  liberadaEn: string;
  liberadaPor: string | null;
  motivo: string | null;
}

export interface Boleta {
  numero: number;
  numeroTexto: string;
  estado: EstadoBoleta;
  precio: number;
  venta: Venta | null;
  historial: Liberada[];
}

export interface CompradorConBoletas {
  comprador: Comprador;
  boletas: { numero: number; numeroTexto: string; estado: EstadoBoleta; saldo: number }[];
}

export interface Deudor {
  numero: number;
  numeroTexto: string;
  estado: EstadoBoleta;
  compradorId: number;
  comprador: string;
  celular: string;
  ubicacion: string | null;
  vendedor: string;
  pagado: number;
  porConfirmar: number;
  saldo: number;
}

export interface Deudores {
  fechaRecordatorio: string;
  fechaLiberacion: string;
  fechaSorteo: string;
  tocaRecordar: boolean;
  tocaLiberar: boolean;
  deudores: Deudor[];
}

export interface TransferenciaPendiente {
  pagoId: number;
  numero: number;
  numeroTexto: string;
  comprador: string;
  valor: number;
  nota: string | null;
  registradoPor: string;
  registradoEn: string;
}

export interface Resumen {
  vendidas: number;
  libres: number;
  recaudado: number;
  porConfirmar: number;
  porCobrar: number;
  abonosPerdidos: number;
  efectivoPorVendedor: { vendedorId: number; vendedor: string; total: number }[];
}

export interface Ganador {
  premio: string;
  cifras: string;
  numero: number | null;
  numeroTexto: string | null;
  comprador: string | null;
  celular: string | null;
  estado: EstadoBoleta | null;
  juega: boolean;
}

export interface ResultadoSorteo {
  resultado: string | null;
  ganadores: Ganador[];
}
