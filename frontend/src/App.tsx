import { Navigate, Route, Routes } from 'react-router-dom';
import { useSesion } from './auth';
import BarraNav from './components/BarraNav';
import Encabezado from './components/Encabezado';
import Compradores from './pages/Compradores';
import Login from './pages/Login';
import Mas from './pages/Mas';
import Pendientes from './pages/Pendientes';
import Resumen from './pages/Resumen';
import Rifas from './pages/Rifas';
import SinRifa from './pages/SinRifa';
import TableroPagina from './pages/Tablero';
import Usuarios from './pages/Usuarios';

export default function App() {
  const { usuario, cargando, rifa, sinRifa, esAdmin } = useSesion();

  if (cargando) return <div className="vacio">Cargando…</div>;
  if (!usuario) return <Login />;

  return (
    <div className="app">
      <Encabezado />
      {sinRifa && !rifa ? (
        <Routes>
          <Route path="/mas" element={<Mas />} />
          {esAdmin && <Route path="/rifas" element={<Rifas />} />}
          {esAdmin && <Route path="/usuarios" element={<Usuarios />} />}
          <Route path="*" element={<SinRifa />} />
        </Routes>
      ) : rifa ? (
        <Routes>
          <Route path="/" element={<TableroPagina rifa={rifa} />} />
          <Route path="/compradores" element={<Compradores rifa={rifa} />} />
          <Route path="/pendientes" element={<Pendientes rifa={rifa} />} />
          <Route path="/resumen" element={<Resumen rifa={rifa} />} />
          <Route path="/mas" element={<Mas />} />
          {esAdmin && <Route path="/rifas" element={<Rifas />} />}
          {esAdmin && <Route path="/usuarios" element={<Usuarios />} />}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      ) : (
        <div className="vacio">No se pudo cargar la rifa. Revise la conexión.</div>
      )}
      <BarraNav />
    </div>
  );
}
