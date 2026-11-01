import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, cuandoExpire, guardarToken, leerToken } from './api';
import type { Rifa, Usuario } from './types';

interface Sesion {
  usuario: Usuario | null;
  cargando: boolean;
  esAdmin: boolean;
  rifa: Rifa | null;
  sinRifa: boolean;
  entrar: (usuario: string, clave: string) => Promise<void>;
  salir: () => void;
  recargarRifa: () => Promise<void>;
}

const Contexto = createContext<Sesion | null>(null);

export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [rifa, setRifa] = useState<Rifa | null>(null);
  const [sinRifa, setSinRifa] = useState(false);
  const [cargando, setCargando] = useState(true);

  const salir = useCallback(() => {
    guardarToken(null);
    setUsuario(null);
    setRifa(null);
  }, []);

  const recargarRifa = useCallback(async () => {
    try {
      setRifa(await api.get<Rifa>('/api/rifas/activa'));
      setSinRifa(false);
    } catch (e) {
      if ((e as { status?: number }).status === 404) {
        setRifa(null);
        setSinRifa(true);
      } else {
        throw e;
      }
    }
  }, []);

  useEffect(() => {
    cuandoExpire(salir);
    if (!leerToken()) {
      setCargando(false);
      return;
    }
    api
      .get<Usuario>('/api/auth/yo')
      .then(async (u) => {
        setUsuario(u);
        await recargarRifa();
      })
      .catch(() => {})
      .finally(() => setCargando(false));
  }, [salir, recargarRifa]);

  const entrar = async (u: string, clave: string) => {
    const res = await api.post<{ token: string; usuario: Usuario }>('/api/auth/login', { usuario: u, clave });
    guardarToken(res.token);
    setUsuario(res.usuario);
    await recargarRifa();
  };

  return (
    <Contexto.Provider
      value={{ usuario, cargando, esAdmin: usuario?.rol === 'ADMIN', rifa, sinRifa, entrar, salir, recargarRifa }}
    >
      {children}
    </Contexto.Provider>
  );
}

export function useSesion(): Sesion {
  const s = useContext(Contexto);
  if (!s) throw new Error('useSesion fuera de ProveedorSesion');
  return s;
}
