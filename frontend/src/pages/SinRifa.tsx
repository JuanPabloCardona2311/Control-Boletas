import { Link } from 'react-router-dom';
import { useSesion } from '../auth';

export default function SinRifa() {
  const { esAdmin } = useSesion();
  return (
    <main className="contenido">
      <div className="tarjeta campos">
        <h2>Todavía no hay una rifa activa</h2>
        {esAdmin ? (
          <>
            <p className="nota" style={{ margin: 0 }}>Cree la rifa con sus datos (números, precio, premios y fecha) para empezar a vender.</p>
            <Link className="boton oro" to="/rifas">Crear rifa</Link>
          </>
        ) : (
          <p className="nota" style={{ margin: 0 }}>Un administrador tiene que crear la rifa para empezar a vender.</p>
        )}
      </div>
    </main>
  );
}
