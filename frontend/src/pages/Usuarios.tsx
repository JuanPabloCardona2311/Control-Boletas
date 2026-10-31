import { useEffect, useState, type FormEvent } from 'react';
import { api } from '../api';
import { useAviso } from '../components/Aviso';
import Hoja from '../components/Hoja';
import type { Rol, Usuario } from '../types';

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editando, setEditando] = useState<Usuario | 'nuevo' | null>(null);
  const { avisar, elemento } = useAviso();

  const cargar = () => api.get<Usuario[]>('/api/usuarios').then(setUsuarios, (e) => setError(e.message));
  useEffect(() => { cargar(); }, []);

  return (
    <main className="contenido">
      {error && <div className="error">{error}</div>}
      <button className="boton oro" onClick={() => setEditando('nuevo')}>Agregar persona</button>
      <div className="tarjeta">
        <h2>Equipo</h2>
        <ul className="lista">
          {usuarios.map((u) => (
            <li key={u.id} onClick={() => setEditando(u)} style={{ cursor: 'pointer', opacity: u.activo ? 1 : 0.5 }}>
              <strong>{u.nombre}</strong> <span className="etiqueta">{u.rol === 'ADMIN' ? 'Admin' : 'Vendedor'}</span>
              {!u.activo && <span className="etiqueta RECHAZADO" style={{ marginLeft: 6 }}>Inactivo</span>}
              <div className="nota">@{u.usuario}{u.celular ? ` · ${u.celular}` : ''}</div>
            </li>
          ))}
        </ul>
      </div>
      {editando && (
        <FormUsuario
          usuario={editando === 'nuevo' ? null : editando}
          alCerrar={() => setEditando(null)}
          alGuardar={(texto) => { avisar(texto); setEditando(null); cargar(); }}
        />
      )}
      {elemento}
    </main>
  );
}

function FormUsuario({ usuario, alCerrar, alGuardar }: { usuario: Usuario | null; alCerrar: () => void; alGuardar: (aviso: string) => void }) {
  const [nombre, setNombre] = useState(usuario?.nombre ?? '');
  const [login, setLogin] = useState(usuario?.usuario ?? '');
  const [clave, setClave] = useState('');
  const [rol, setRol] = useState<Rol>(usuario?.rol ?? 'VENDEDOR');
  const [celular, setCelular] = useState(usuario?.celular ?? '');
  const [activo, setActivo] = useState(usuario?.activo ?? true);
  const [error, setError] = useState<string | null>(null);

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      if (usuario) {
        await api.put(`/api/usuarios/${usuario.id}`, { nombre, rol, celular, activo });
        if (clave) await api.put(`/api/usuarios/${usuario.id}/clave`, { clave });
        alGuardar('Cambios guardados');
      } else {
        await api.post('/api/usuarios', { nombre, usuario: login, clave, rol, celular });
        alGuardar(`Listo. Pásele a ${nombre.split(' ')[0]} su usuario y clave.`);
      }
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <Hoja titulo={<h2>{usuario ? usuario.nombre : 'Nueva persona'}</h2>} alCerrar={alCerrar}>
      <form className="campos" onSubmit={enviar}>
        {error && <div className="error">{error}</div>}
        <div className="campo">
          <label htmlFor="un">Nombre</label>
          <input id="un" value={nombre} onChange={(e) => setNombre(e.target.value)} required />
        </div>
        {!usuario && (
          <div className="campo">
            <label htmlFor="ul">Usuario para entrar</label>
            <input id="ul" value={login} onChange={(e) => setLogin(e.target.value.toLowerCase())} autoCapitalize="none" required placeholder="adriana" />
          </div>
        )}
        <div className="campo">
          <label htmlFor="uc">{usuario ? 'Clave nueva (dejar vacío para no cambiarla)' : 'Clave (mínimo 6)'}</label>
          <input id="uc" value={clave} onChange={(e) => setClave(e.target.value)} required={!usuario} minLength={6} autoComplete="new-password" />
        </div>
        <div className="fila">
          <div className="campo">
            <label htmlFor="ur">Rol</label>
            <select id="ur" value={rol} onChange={(e) => setRol(e.target.value as Rol)}>
              <option value="VENDEDOR">Vendedor</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </div>
          <div className="campo">
            <label htmlFor="ucel">Celular</label>
            <input id="ucel" value={celular} onChange={(e) => setCelular(e.target.value)} inputMode="tel" />
          </div>
        </div>
        {usuario && (
          <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontWeight: 600 }}>
            <input type="checkbox" checked={activo} onChange={(e) => setActivo(e.target.checked)} style={{ width: 20, height: 20 }} />
            Puede entrar a la app
          </label>
        )}
        <button className="boton oro ancho">{usuario ? 'Guardar' : 'Crear'}</button>
      </form>
    </Hoja>
  );
}
