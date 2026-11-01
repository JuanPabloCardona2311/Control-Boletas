import { NavLink } from 'react-router-dom';

const ICONOS = {
  tablero: <path d="M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z" />,
  compradores: <path d="M16 11a4 4 0 1 0-8 0M3 21a9 9 0 0 1 18 0M12 7a4 4 0 1 0 0 8 4 4 0 0 0 0-8z" />,
  pendientes: <path d="M12 7v5l3 2M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z" />,
  resumen: <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />,
  mas: <path d="M4 6h16M4 12h16M4 18h16" />,
};

function Icono({ nombre }: { nombre: keyof typeof ICONOS }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {ICONOS[nombre]}
    </svg>
  );
}

const ENLACES: { a: string; texto: string; icono: keyof typeof ICONOS }[] = [
  { a: '/', texto: 'Números', icono: 'tablero' },
  { a: '/compradores', texto: 'Compradores', icono: 'compradores' },
  { a: '/pendientes', texto: 'Pendientes', icono: 'pendientes' },
  { a: '/resumen', texto: 'Resumen', icono: 'resumen' },
  { a: '/mas', texto: 'Más', icono: 'mas' },
];

export default function BarraNav() {
  return (
    <nav className="nav">
      {ENLACES.map((e) => (
        <NavLink key={e.a} to={e.a} end={e.a === '/'} className={({ isActive }) => (isActive ? 'activo' : '')}>
          <Icono nombre={e.icono} />
          {e.texto}
        </NavLink>
      ))}
    </nav>
  );
}
