import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useSesion } from '../auth';
import { useAviso } from '../components/Aviso';

export default function Mas() {
  const { usuario, esAdmin, salir } = useSesion();
  const [actual, setActual] = useState('');
  const [nueva, setNueva] = useState('');
  const [error, setError] = useState<string | null>(null);
  const { avisar, elemento } = useAviso();
  const navegar = useNavigate();

  const cambiarClave = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      await api.put('/api/auth/yo/clave', { claveActual: actual, claveNueva: nueva });
      setActual('');
      setNueva('');
      avisar('Clave cambiada');
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <main className="contenido">
      <div className="tarjeta">
        <h2>{usuario?.nombre}</h2>
        <div className="nota">@{usuario?.usuario} · {esAdmin ? 'Administrador' : 'Vendedor'}</div>
      </div>

      {esAdmin && (
        <div className="tarjeta">
          <h2>Administración</h2>
          <ul className="lista menu">
            <li><Link to="/rifas">Rifas: crear, editar y resultado <span>›</span></Link></li>
            <li><Link to="/usuarios">Vendedores y administradores <span>›</span></Link></li>
          </ul>
        </div>
      )}

      <form className="tarjeta campos" onSubmit={cambiarClave}>
        <h2>Cambiar mi clave</h2>
        {error && <div className="error">{error}</div>}
        <div className="campo">
          <label htmlFor="actual">Clave actual</label>
          <input id="actual" type="password" value={actual} onChange={(e) => setActual(e.target.value)} required autoComplete="current-password" />
        </div>
        <div className="campo">
          <label htmlFor="nueva">Clave nueva (mínimo 6)</label>
          <input id="nueva" type="password" value={nueva} onChange={(e) => setNueva(e.target.value)} required minLength={6} autoComplete="new-password" />
        </div>
        <button className="boton">Cambiar clave</button>
      </form>

      <button className="boton claro ancho" onClick={() => { salir(); navegar('/'); }}>Salir</button>
      {elemento}
    </main>
  );
}
