package co.elmulardeoro.boletas.reporte;

import co.elmulardeoro.boletas.boleta.BoletaRepository;
import co.elmulardeoro.boletas.boleta.EstadoBoleta;
import co.elmulardeoro.boletas.boleta.EstadoPago;
import co.elmulardeoro.boletas.boleta.PagoRepository;
import co.elmulardeoro.boletas.boleta.Venta;
import co.elmulardeoro.boletas.boleta.VentaRepository;
import co.elmulardeoro.boletas.comun.ApiException;
import co.elmulardeoro.boletas.config.AppProperties;
import co.elmulardeoro.boletas.reporte.ReporteDtos.Deudor;
import co.elmulardeoro.boletas.reporte.ReporteDtos.Deudores;
import co.elmulardeoro.boletas.reporte.ReporteDtos.EfectivoVendedor;
import co.elmulardeoro.boletas.reporte.ReporteDtos.Ganador;
import co.elmulardeoro.boletas.reporte.ReporteDtos.Resumen;
import co.elmulardeoro.boletas.reporte.ReporteDtos.ResultadoSorteo;
import co.elmulardeoro.boletas.reporte.ReporteDtos.TransferenciaPendiente;
import co.elmulardeoro.boletas.rifa.Premio;
import co.elmulardeoro.boletas.rifa.Rifa;
import co.elmulardeoro.boletas.rifa.RifaRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReporteService {

    private final RifaRepository rifas;
    private final BoletaRepository boletas;
    private final VentaRepository ventas;
    private final PagoRepository pagos;
    private final JdbcClient jdbc;
    private final AppProperties props;

    public ReporteService(RifaRepository rifas, BoletaRepository boletas, VentaRepository ventas,
                          PagoRepository pagos, JdbcClient jdbc, AppProperties props) {
        this.rifas = rifas;
        this.boletas = boletas;
        this.ventas = ventas;
        this.pagos = pagos;
        this.jdbc = jdbc;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public Resumen resumen(Long rifaId) {
        Rifa rifa = rifa(rifaId);
        List<Venta> vigentes = ventas.vigentes(rifaId);
        long cobradoVigentes = vigentes.stream().mapToLong(v -> Math.min(v.totalConfirmado(), rifa.getPrecio())).sum();
        long porConfirmar = vigentes.stream().mapToLong(Venta::totalPorConfirmar).sum();
        long recaudado = jdbc.sql("""
                        SELECT COALESCE(SUM(p.valor), 0) FROM pago p
                        JOIN venta v ON v.id = p.venta_id JOIN boleta b ON b.id = v.boleta_id
                        WHERE b.rifa_id = :rifaId AND p.estado = 'CONFIRMADO'
                        """)
                .param("rifaId", rifaId).query(Long.class).single();
        long perdidos = jdbc.sql("""
                        SELECT COALESCE(SUM(p.valor), 0) FROM pago p
                        JOIN venta v ON v.id = p.venta_id JOIN boleta b ON b.id = v.boleta_id
                        WHERE b.rifa_id = :rifaId AND p.estado = 'CONFIRMADO' AND v.liberada_en IS NOT NULL
                        """)
                .param("rifaId", rifaId).query(Long.class).single();
        List<EfectivoVendedor> efectivo = jdbc.sql("""
                        SELECT u.id, u.nombre, SUM(p.valor) AS total FROM pago p
                        JOIN venta v ON v.id = p.venta_id JOIN boleta b ON b.id = v.boleta_id
                        JOIN usuario u ON u.id = p.registrado_por
                        WHERE b.rifa_id = :rifaId AND p.estado = 'CONFIRMADO' AND p.metodo = 'EFECTIVO'
                        GROUP BY u.id, u.nombre ORDER BY total DESC
                        """)
                .param("rifaId", rifaId)
                .query((rs, i) -> new EfectivoVendedor(rs.getLong("id"), rs.getString("nombre"), rs.getLong("total")))
                .list();
        return new Resumen(vigentes.size(), rifa.getCantidadNumeros() - vigentes.size(), recaudado, porConfirmar,
                vigentes.size() * rifa.getPrecio() - cobradoVigentes, perdidos, efectivo);
    }

    @Transactional(readOnly = true)
    public Deudores deudores(Long rifaId) {
        Rifa rifa = rifa(rifaId);
        LocalDate hoy = LocalDate.now(props.zona());
        List<Deudor> deudores = ventas.vigentes(rifaId).stream()
                .filter(v -> v.totalConfirmado() < rifa.getPrecio())
                .sorted(Comparator.comparingInt(v -> v.getBoleta().getNumero()))
                .map(v -> {
                    long pagado = v.totalConfirmado();
                    int n = v.getBoleta().getNumero();
                    return new Deudor(n, rifa.formatear(n), EstadoBoleta.segun(pagado, rifa.getPrecio()),
                            v.getComprador().getId(), v.getComprador().getNombre(), v.getComprador().getCelular(),
                            v.getComprador().getUbicacion(), v.getVendedor().getNombre(), pagado,
                            v.totalPorConfirmar(), rifa.getPrecio() - pagado);
                })
                .toList();
        return new Deudores(rifa.fechaRecordatorio(), rifa.fechaLiberacion(), rifa.getFechaSorteo(),
                !hoy.isBefore(rifa.fechaRecordatorio()), !hoy.isBefore(rifa.fechaLiberacion()), deudores);
    }

    @Transactional(readOnly = true)
    public List<TransferenciaPendiente> transferenciasPendientes(Long rifaId) {
        Rifa rifa = rifa(rifaId);
        return pagos.deRifaEnEstado(rifaId, EstadoPago.POR_CONFIRMAR).stream()
                .map(p -> {
                    int n = p.getVenta().getBoleta().getNumero();
                    return new TransferenciaPendiente(p.getId(), n, rifa.formatear(n),
                            p.getVenta().getComprador().getNombre(), p.getValor(), p.getNota(),
                            p.getRegistradoPor().getNombre(), p.getRegistradoEn());
                })
                .toList();
    }

    @Transactional
    public ResultadoSorteo registrarResultado(Long rifaId, String resultado) {
        Rifa rifa = rifa(rifaId);
        int maxCifras = rifa.getPremios().stream().mapToInt(Premio::getCifras).max().orElse(0);
        if (resultado.length() < maxCifras) {
            throw ApiException.invalido("El resultado debe tener al menos " + maxCifras + " cifras");
        }
        rifa.setResultado(resultado);
        return ganadores(rifa);
    }

    @Transactional(readOnly = true)
    public ResultadoSorteo ganadores(Long rifaId) {
        return ganadores(rifa(rifaId));
    }

    /** Cada premio se compara con las primeras o últimas cifras del resultado. Una boleta sin pagar completa no juega. */
    private ResultadoSorteo ganadores(Rifa rifa) {
        if (rifa.getResultado() == null) {
            return new ResultadoSorteo(null, List.of());
        }
        List<Ganador> ganadores = rifa.getPremios().stream().map(premio -> {
            String cifras = premio.cifrasGanadoras(rifa.getResultado());
            if (premio.getCifras() != rifa.cifras()) {
                // Premios por menos cifras que la boleta (p. ej. "2 últimas") pueden tener varios dueños; se listan sin dueño
                return new Ganador(premio.getDescripcion(), cifras, null, null, null, null, null, false);
            }
            int numero = Integer.parseInt(cifras);
            return boletas.findByRifaIdAndNumero(rifa.getId(), numero)
                    .filter(b -> b.getVenta() != null)
                    .map(b -> {
                        Venta v = b.getVenta();
                        EstadoBoleta estado = EstadoBoleta.segun(v.totalConfirmado(), rifa.getPrecio());
                        return new Ganador(premio.getDescripcion(), cifras, numero, rifa.formatear(numero),
                                v.getComprador().getNombre(), v.getComprador().getCelular(), estado,
                                estado == EstadoBoleta.PAGADA);
                    })
                    .orElseGet(() -> new Ganador(premio.getDescripcion(), cifras, numero, rifa.formatear(numero),
                            null, null, EstadoBoleta.LIBRE, false));
        }).toList();
        return new ResultadoSorteo(rifa.getResultado(), ganadores);
    }

    private Rifa rifa(Long id) {
        return rifas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Rifa no encontrada"));
    }
}
