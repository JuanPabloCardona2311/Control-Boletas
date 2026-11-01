package co.elmulardeoro.boletas.boleta;

import co.elmulardeoro.boletas.boleta.BoletaDtos.ApartarRequest;
import co.elmulardeoro.boletas.boleta.BoletaDtos.BoletaDeComprador;
import co.elmulardeoro.boletas.boleta.BoletaDtos.BoletaDto;
import co.elmulardeoro.boletas.boleta.BoletaDtos.CompradorConBoletas;
import co.elmulardeoro.boletas.boleta.BoletaDtos.CompradorDto;
import co.elmulardeoro.boletas.boleta.BoletaDtos.Conteo;
import co.elmulardeoro.boletas.boleta.BoletaDtos.EditarComprador;
import co.elmulardeoro.boletas.boleta.BoletaDtos.LiberadaDto;
import co.elmulardeoro.boletas.boleta.BoletaDtos.Ocupada;
import co.elmulardeoro.boletas.boleta.BoletaDtos.PagoDto;
import co.elmulardeoro.boletas.boleta.BoletaDtos.PagoRequest;
import co.elmulardeoro.boletas.boleta.BoletaDtos.TableroDto;
import co.elmulardeoro.boletas.boleta.BoletaDtos.VentaDto;
import co.elmulardeoro.boletas.comun.ApiException;
import co.elmulardeoro.boletas.rifa.Rifa;
import co.elmulardeoro.boletas.rifa.RifaRepository;
import co.elmulardeoro.boletas.usuario.Usuario;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class BoletaService {

    private final RifaRepository rifas;
    private final BoletaRepository boletas;
    private final VentaRepository ventas;
    private final PagoRepository pagos;
    private final CompradorRepository compradores;
    private final JdbcClient jdbc;

    public BoletaService(RifaRepository rifas, BoletaRepository boletas, VentaRepository ventas,
                         PagoRepository pagos, CompradorRepository compradores, JdbcClient jdbc) {
        this.rifas = rifas;
        this.boletas = boletas;
        this.ventas = ventas;
        this.pagos = pagos;
        this.compradores = compradores;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public TableroDto tablero(Long rifaId) {
        Rifa rifa = rifa(rifaId);
        List<Ocupada> ocupadas = jdbc.sql("""
                        SELECT b.numero,
                               COALESCE(SUM(p.valor) FILTER (WHERE p.estado = 'CONFIRMADO'), 0) AS pagado,
                               COUNT(p.id) FILTER (WHERE p.estado = 'POR_CONFIRMAR') AS por_confirmar
                        FROM boleta b
                        LEFT JOIN pago p ON p.venta_id = b.venta_id
                        WHERE b.rifa_id = :rifaId AND b.venta_id IS NOT NULL
                        GROUP BY b.numero
                        ORDER BY b.numero
                        """)
                .param("rifaId", rifaId)
                .query((rs, i) -> new Ocupada(rs.getInt("numero"),
                        EstadoBoleta.segun(rs.getLong("pagado"), rifa.getPrecio()),
                        rs.getLong("por_confirmar") > 0))
                .list();
        Map<EstadoBoleta, Long> porEstado = ocupadas.stream()
                .collect(Collectors.groupingBy(Ocupada::estado, () -> new EnumMap<>(EstadoBoleta.class), Collectors.counting()));
        Conteo conteo = new Conteo(rifa.getCantidadNumeros() - ocupadas.size(),
                porEstado.getOrDefault(EstadoBoleta.APARTADA, 0L).intValue(),
                porEstado.getOrDefault(EstadoBoleta.ABONADA, 0L).intValue(),
                porEstado.getOrDefault(EstadoBoleta.PAGADA, 0L).intValue());
        return new TableroDto(rifa.getId(), rifa.getCantidadNumeros(), rifa.cifras(), rifa.getPrecio(), conteo, ocupadas);
    }

    @Transactional(readOnly = true)
    public BoletaDto detalle(Long rifaId, int numero) {
        Rifa rifa = rifa(rifaId);
        Boleta boleta = boletas.findByRifaIdAndNumero(rifaId, numero).orElseThrow(() -> numeroInvalido(rifa));
        return armar(rifa, boleta);
    }

    @Transactional
    public BoletaDto apartar(Long rifaId, int numero, ApartarRequest req, Usuario vendedor) {
        Rifa rifa = rifaActiva(rifaId);
        Boleta boleta = boletas.bloquear(rifaId, numero).orElseThrow(() -> numeroInvalido(rifa));
        if (boleta.getVenta() != null) {
            throw ApiException.conflicto("El " + rifa.formatear(numero) + " ya lo tiene "
                    + boleta.getVenta().getComprador().getNombre());
        }
        Venta venta = ventas.save(new Venta(boleta, comprador(req), vendedor));
        boleta.asignar(venta);
        if (req.abono() != null) {
            nuevoPago(rifa, venta, req.abono(), vendedor);
        }
        return armar(rifa, boleta);
    }

    @Transactional
    public BoletaDto registrarPago(Long rifaId, int numero, PagoRequest req, Usuario quien) {
        Rifa rifa = rifaActiva(rifaId);
        Boleta boleta = boletas.bloquear(rifaId, numero).orElseThrow(() -> numeroInvalido(rifa));
        if (boleta.getVenta() == null) {
            throw ApiException.invalido("El " + rifa.formatear(numero) + " está libre; primero hay que apartarlo");
        }
        nuevoPago(rifa, boleta.getVenta(), req, quien);
        boleta.tocar();
        return armar(rifa, boleta);
    }

    /** Efectivo cuenta de una vez. Una transferencia registrada por un vendedor queda por confirmar. */
    private void nuevoPago(Rifa rifa, Venta venta, PagoRequest req, Usuario quien) {
        long saldo = rifa.getPrecio() - venta.totalConfirmado() - venta.totalPorConfirmar();
        if (req.valor() > saldo) {
            throw ApiException.invalido("El pago supera lo que falta ($" + saldo + ")");
        }
        boolean confirmado = req.metodo() == MetodoPago.EFECTIVO || quien.esAdmin();
        Pago pago = pagos.save(new Pago(venta, req.valor(), req.metodo(),
                confirmado ? EstadoPago.CONFIRMADO : EstadoPago.POR_CONFIRMAR, limpio(req.nota()), quien));
        venta.getPagos().add(pago);
    }

    @Transactional
    public BoletaDto confirmarPago(Long pagoId, Usuario admin) {
        Pago pago = pago(pagoId);
        if (pago.getEstado() != EstadoPago.POR_CONFIRMAR) {
            throw ApiException.conflicto("Este pago ya fue revisado");
        }
        pago.revisar(EstadoPago.CONFIRMADO, admin);
        return refrescar(pago);
    }

    /** Rechaza una transferencia que no llegó, o anula un pago que se registró por error. */
    @Transactional
    public BoletaDto rechazarPago(Long pagoId, Usuario admin) {
        Pago pago = pago(pagoId);
        if (pago.getEstado() == EstadoPago.RECHAZADO) {
            throw ApiException.conflicto("Este pago ya estaba rechazado");
        }
        pago.revisar(EstadoPago.RECHAZADO, admin);
        return refrescar(pago);
    }

    private BoletaDto refrescar(Pago pago) {
        Boleta boleta = pago.getVenta().getBoleta();
        boleta.tocar();
        return armar(rifa(boleta.getRifaId()), boleta);
    }

    /** El número vuelve a quedar libre. La venta y sus abonos quedan en el historial de la boleta. */
    @Transactional
    public BoletaDto liberar(Long rifaId, int numero, String motivo, Usuario admin) {
        Rifa rifa = rifaActiva(rifaId);
        Boleta boleta = boletas.bloquear(rifaId, numero).orElseThrow(() -> numeroInvalido(rifa));
        if (boleta.getVenta() == null) {
            throw ApiException.conflicto("El " + rifa.formatear(numero) + " ya está libre");
        }
        boleta.getVenta().liberar(admin, limpio(motivo));
        boleta.asignar(null);
        return armar(rifa, boleta);
    }

    @Transactional(readOnly = true)
    public List<CompradorConBoletas> buscarCompradores(String q, Long rifaId) {
        String texto = q == null ? "" : q.trim();
        if (texto.length() < 2) {
            return List.of();
        }
        Rifa rifa = rifa(rifaId);
        List<Comprador> encontrados = compradores.buscar(texto).stream().limit(30).toList();
        if (encontrados.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Venta>> porComprador = ventas.vigentesDe(encontrados.stream().map(Comprador::getId).toList(), rifaId)
                .stream().collect(Collectors.groupingBy(v -> v.getComprador().getId()));
        return encontrados.stream().map(c -> new CompradorConBoletas(CompradorDto.de(c),
                porComprador.getOrDefault(c.getId(), List.of()).stream().map(v -> {
                    long pagado = v.totalConfirmado();
                    int n = v.getBoleta().getNumero();
                    return new BoletaDeComprador(n, rifa.formatear(n), EstadoBoleta.segun(pagado, rifa.getPrecio()),
                            Math.max(0, rifa.getPrecio() - pagado));
                }).toList())).toList();
    }

    @Transactional
    public CompradorDto editarComprador(Long id, EditarComprador req) {
        Comprador c = compradores.findById(id).orElseThrow(() -> ApiException.noEncontrado("Comprador no encontrado"));
        c.setNombre(req.nombre().trim());
        c.setCelular(celular(req.celular()));
        c.setUbicacion(limpio(req.ubicacion()));
        return CompradorDto.de(c);
    }

    private Comprador comprador(ApartarRequest req) {
        if (req.compradorId() != null) {
            return compradores.findById(req.compradorId())
                    .orElseThrow(() -> ApiException.noEncontrado("Comprador no encontrado"));
        }
        if (!StringUtils.hasText(req.nombre()) || !StringUtils.hasText(req.celular())) {
            throw ApiException.invalido("Falta el nombre o el celular del comprador");
        }
        String celular = celular(req.celular());
        // Mismo celular = misma persona; así no se duplica quien compra varios números
        return compradores.findFirstByCelularOrderByIdAsc(celular)
                .orElseGet(() -> compradores.save(new Comprador(req.nombre().trim(), celular, limpio(req.ubicacion()))));
    }

    private BoletaDto armar(Rifa rifa, Boleta boleta) {
        List<LiberadaDto> historial = ventas.liberadas(boleta.getId()).stream()
                .map(v -> new LiberadaDto(v.getComprador().getNombre(), v.getComprador().getCelular(), v.totalConfirmado(),
                        v.getLiberadaEn(), v.getLiberadaPor() == null ? null : v.getLiberadaPor().getNombre(),
                        v.getMotivoLiberacion()))
                .toList();
        Venta v = boleta.getVenta();
        if (v == null) {
            return new BoletaDto(boleta.getNumero(), rifa.formatear(boleta.getNumero()), EstadoBoleta.LIBRE,
                    rifa.getPrecio(), null, historial);
        }
        long pagado = v.totalConfirmado();
        long porConfirmar = v.totalPorConfirmar();
        VentaDto venta = new VentaDto(v.getId(), CompradorDto.de(v.getComprador()), v.getVendedor().getId(),
                v.getVendedor().getNombre(), v.getCreadaEn(), pagado, porConfirmar,
                Math.max(0, rifa.getPrecio() - pagado), v.getPagos().stream().map(PagoDto::de).toList());
        return new BoletaDto(boleta.getNumero(), rifa.formatear(boleta.getNumero()),
                EstadoBoleta.segun(pagado, rifa.getPrecio()), rifa.getPrecio(), venta, historial);
    }

    private Rifa rifa(Long id) {
        return rifas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Rifa no encontrada"));
    }

    private Rifa rifaActiva(Long id) {
        Rifa rifa = rifa(id);
        if (!rifa.isActiva()) {
            throw ApiException.conflicto("Esta rifa ya está archivada; no se puede modificar");
        }
        return rifa;
    }

    private Pago pago(Long id) {
        Pago pago = pagos.findById(id).orElseThrow(() -> ApiException.noEncontrado("Pago no encontrado"));
        rifaActiva(pago.getVenta().getBoleta().getRifaId());
        return pago;
    }

    private static ApiException numeroInvalido(Rifa rifa) {
        return ApiException.invalido("El número debe estar entre " + rifa.formatear(0) + " y "
                + rifa.formatear(rifa.getCantidadNumeros() - 1));
    }

    /** Guarda solo los dígitos, para poder buscar y armar el link de WhatsApp. */
    static String celular(String texto) {
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() < 7) {
            throw ApiException.invalido("El celular no parece válido");
        }
        return digitos;
    }

    private static String limpio(String texto) {
        return StringUtils.hasText(texto) ? texto.trim() : null;
    }
}
