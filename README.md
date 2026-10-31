# Control de boletas · El Mular De Oro

Sistema para que los vendedores de la rifa El Mular De Oro controlen la venta de boletas: estado, abonos y recordatorios por WhatsApp.

Todos los vendedores ven los mismos números en tiempo casi real (la app consulta cada 6 segundos), así nadie vende un número que ya está apartado.

## Qué hace

- **Tablero** con todos los números en colores: libre, apartada, abonada y pagada. Un punto marca las transferencias por confirmar.
- **Apartar** un número con nombre, celular y pueblo del comprador, con o sin abono. Si dos vendedores intentan apartar el mismo número a la vez, solo uno lo logra.
- **Abonos** en efectivo (cuentan de una vez) o por transferencia (quedan por confirmar hasta que un administrador las aprueba).
- **WhatsApp**: botones que abren el chat del comprador con el comprobante o el recordatorio ya escritos.
- **Pendientes**: quién debe, con las fechas de la regla de la rifa (recordar 14 días antes, liberar apartadas sin abono 7 días antes; a las abonadas se les espera hasta el sorteo).
- **Liberar** números (solo administradores). La venta anterior y sus abonos quedan en el historial de la boleta.
- **Resumen** de plata: recogido, por cobrar, por confirmar, efectivo por vendedor, y una imagen de los números libres para historias de Instagram.
- **Rifas**: los administradores crean una rifa nueva con todos sus datos (100, 1000 o 10000 números, precio, lotería, fecha, premios) y la anterior queda archivada.
- **Resultado del sorteo**: se escribe el número de la lotería y la app muestra quién ganó cada premio y si su boleta estaba pagada.

## Estructura

| Carpeta | Qué hay |
| --- | --- |
| `backend/` | API REST en Java 21 + Spring Boot 4, Spring Security con JWT, PostgreSQL con Flyway |
| `frontend/` | App web en React + Vite + TypeScript, instalable en el celular |

## Correr en el computador

Requisitos: Java 21, Maven, Node 22 y PostgreSQL 16.

```bash
# Base de datos local
createuser boletas --pwprompt        # clave: boletas
createdb boletas -O boletas
createdb boletas_test -O boletas     # para las pruebas

# Backend (http://localhost:8080). La primera vez crea el administrador indicado.
cd backend
ADMIN_USUARIO=julian ADMIN_CLAVE=una-clave-segura mvn spring-boot:run

# Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Pruebas del backend: `cd backend && mvn verify` (usan la base `boletas_test`).

## Variables del backend

| Variable | Para qué | Ejemplo |
| --- | --- | --- |
| `DATABASE_URL` | Conexión JDBC a PostgreSQL | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DATABASE_USER` | Usuario de la base | `postgres.abcdefgh` |
| `DATABASE_PASSWORD` | Clave de la base | |
| `JWT_SECRET` | Llave para firmar las sesiones, mínimo 32 caracteres | una cadena larga al azar |
| `FRONTEND_URL` | Dirección del frontend (puede ser una lista separada por comas) | `https://boletas-mular.vercel.app` |
| `ADMIN_USUARIO` / `ADMIN_CLAVE` | Primer administrador; solo se usa si no hay usuarios | |
| `JWT_DIAS` | Días que dura la sesión abierta (por defecto 30) | |

Variable del frontend: `VITE_API_URL` con la dirección del backend (ver `frontend/.env.example`).

## Publicarlo

1. **Base de datos en Supabase**: crear el proyecto y copiar la cadena de conexión del **Session pooler** (Project Settings → Database). La conexión directa de Supabase es solo IPv6 y Render no la alcanza. Las tablas las crea Flyway al arrancar el backend.
2. **Backend en Render**: nuevo *Web Service* desde este repositorio, con *Root Directory* `backend` y entorno Docker (usa `backend/Dockerfile`). Poner las variables de arriba. Health check: `/api/salud`.
3. **Frontend en Vercel**: importar el repositorio con *Root Directory* `frontend`, y poner `VITE_API_URL` con la dirección de Render. Después, poner esa dirección de Vercel en `FRONTEND_URL` del backend.

El plan gratis de Render apaga el backend cuando nadie lo usa y la primera apertura tarda cerca de un minuto. En temporada de ventas conviene un plan pago.
