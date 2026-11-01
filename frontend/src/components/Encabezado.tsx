import { useSesion } from '../auth';
import { fecha } from '../format';
import logo from '../logo.png';

export default function Encabezado() {
  const { rifa } = useSesion();
  return (
    <header className="encabezado">
      <img src={logo} alt="" />
      <div>
        <div className="titulo-marca">El Mular De Oro</div>
        <small>{rifa ? `${rifa.nombre} · juega el ${fecha(rifa.fechaSorteo)}` : 'Control de boletas'}</small>
      </div>
    </header>
  );
}
