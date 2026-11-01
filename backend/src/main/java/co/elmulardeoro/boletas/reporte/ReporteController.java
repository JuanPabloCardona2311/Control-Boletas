package co.elmulardeoro.boletas.reporte;

import co.elmulardeoro.boletas.reporte.ReporteDtos.Deudores;
import co.elmulardeoro.boletas.reporte.ReporteDtos.Resumen;
import co.elmulardeoro.boletas.reporte.ReporteDtos.ResultadoSorteo;
import co.elmulardeoro.boletas.reporte.ReporteDtos.TransferenciaPendiente;
import co.elmulardeoro.boletas.rifa.RifaDtos.Resultado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rifas/{rifaId}")
public class ReporteController {

    private final ReporteService servicio;

    public ReporteController(ReporteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/resumen")
    Resumen resumen(@PathVariable Long rifaId) {
        return servicio.resumen(rifaId);
    }

    @GetMapping("/deudores")
    Deudores deudores(@PathVariable Long rifaId) {
        return servicio.deudores(rifaId);
    }

    @GetMapping("/transferencias-pendientes")
    @PreAuthorize("hasRole('ADMIN')")
    List<TransferenciaPendiente> transferencias(@PathVariable Long rifaId) {
        return servicio.transferenciasPendientes(rifaId);
    }

    @GetMapping("/ganadores")
    ResultadoSorteo ganadores(@PathVariable Long rifaId) {
        return servicio.ganadores(rifaId);
    }

    @PostMapping("/resultado")
    @PreAuthorize("hasRole('ADMIN')")
    ResultadoSorteo resultado(@PathVariable Long rifaId, @Valid @RequestBody Resultado req) {
        return servicio.registrarResultado(rifaId, req.resultado());
    }
}
