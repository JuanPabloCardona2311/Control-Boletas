import { useEffect, type ReactNode } from 'react';

/** Panel que sube desde abajo, cómodo para usar con una mano en el celular. */
export default function Hoja({ titulo, alCerrar, children }: { titulo: ReactNode; alCerrar: () => void; children: ReactNode }) {
  useEffect(() => {
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && alCerrar();
    document.addEventListener('keydown', tecla);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', tecla);
      document.body.style.overflow = '';
    };
  }, [alCerrar]);

  return (
    <div className="fondo" onClick={alCerrar}>
      <div className="hoja" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
        <div className="hoja-cabeza">
          <div>{titulo}</div>
          <button className="cerrar" onClick={alCerrar} aria-label="Cerrar">×</button>
        </div>
        {children}
      </div>
    </div>
  );
}
