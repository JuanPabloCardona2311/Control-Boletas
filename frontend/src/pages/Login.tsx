import { useState, type FormEvent } from 'react';
import { useSesion } from '../auth';
import logo from '../logo.png';

export default function Login() {
  const { entrar } = useSesion();
  const [usuario, setUsuario] = useState('');
  const [clave, setClave] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [ocupado, setOcupado] = useState(false);

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    setOcupado(true);
    setError(null);
    try {
      await entrar(usuario, clave);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setOcupado(false);
    }
  };

  return (
    <div className="login">
      <form className="tarjeta" onSubmit={enviar}>
        <img src={logo} alt="El Mular De Oro" />
        <h1 className="titulo-marca">Control de boletas</h1>
        {error && <div className="error">{error}</div>}
        <div className="campo">
          <label htmlFor="usuario">Usuario</label>
          <input id="usuario" value={usuario} onChange={(e) => setUsuario(e.target.value)} autoCapitalize="none" autoComplete="username" required />
        </div>
        <div className="campo">
          <label htmlFor="clave">Clave</label>
          <input id="clave" type="password" value={clave} onChange={(e) => setClave(e.target.value)} autoComplete="current-password" required />
        </div>
        <button className="boton oro ancho" disabled={ocupado}>{ocupado ? 'Entrando…' : 'Entrar'}</button>
      </form>
    </div>
  );
}
