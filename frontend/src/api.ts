const BASE = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');
const LLAVE_TOKEN = 'boletas.token';

export class ApiError extends Error {
  constructor(public status: number, mensaje: string) {
    super(mensaje);
  }
}

let alExpirar: () => void = () => {};

export function cuandoExpire(fn: () => void) {
  alExpirar = fn;
}

export function leerToken(): string | null {
  try {
    return localStorage.getItem(LLAVE_TOKEN);
  } catch {
    return null;
  }
}

export function guardarToken(token: string | null) {
  try {
    if (token) localStorage.setItem(LLAVE_TOKEN, token);
    else localStorage.removeItem(LLAVE_TOKEN);
  } catch {
    // Sin almacenamiento (modo privado): la sesión dura mientras la pestaña esté abierta
  }
}

async function pedir<T>(metodo: string, ruta: string, cuerpo?: unknown): Promise<T> {
  const token = leerToken();
  let res: Response;
  try {
    res = await fetch(BASE + ruta, {
      method: metodo,
      headers: {
        ...(cuerpo !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Sin conexión. Revise la señal e intente de nuevo.');
  }
  if (res.status === 401 && ruta !== '/api/auth/login') {
    alExpirar();
    throw new ApiError(401, 'La sesión se venció. Vuelva a entrar.');
  }
  if (!res.ok) {
    let mensaje = 'Algo salió mal. Intente de nuevo.';
    try {
      mensaje = (await res.json()).mensaje ?? mensaje;
    } catch {
      // respuesta sin cuerpo JSON
    }
    throw new ApiError(res.status, mensaje);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  get: <T>(ruta: string) => pedir<T>('GET', ruta),
  post: <T>(ruta: string, cuerpo: unknown = {}) => pedir<T>('POST', ruta, cuerpo),
  put: <T>(ruta: string, cuerpo: unknown) => pedir<T>('PUT', ruta, cuerpo),
};

/** Pide el tablero mandando el ETag anterior; devuelve null si nada cambió (304). */
export async function pedirConEtag<T>(ruta: string, etag: string | null): Promise<{ datos: T; etag: string | null } | null> {
  const token = leerToken();
  const res = await fetch(BASE + ruta, {
    cache: 'no-store',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(etag ? { 'If-None-Match': etag } : {}),
    },
  });
  if (res.status === 304) return null;
  if (res.status === 401) {
    alExpirar();
    throw new ApiError(401, 'La sesión se venció. Vuelva a entrar.');
  }
  if (!res.ok) throw new ApiError(res.status, 'No se pudo cargar el tablero');
  return { datos: (await res.json()) as T, etag: res.headers.get('ETag') };
}
