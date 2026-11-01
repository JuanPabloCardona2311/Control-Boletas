package co.elmulardeoro.boletas.boleta;

import co.elmulardeoro.boletas.boleta.BoletaDtos.ApartarRequest;
import co.elmulardeoro.boletas.boleta.BoletaDtos.BoletaDto;
import co.elmulardeoro.boletas.boleta.BoletaDtos.CompradorConBoletas;
import co.elmulardeoro.boletas.boleta.BoletaDtos.CompradorDto;
import co.elmulardeoro.boletas.boleta.BoletaDtos.EditarComprador;
import co.elmulardeoro.boletas.boleta.BoletaDtos.LiberarRequest;
import co.elmulardeoro.boletas.boleta.BoletaDtos.PagoRequest;
import co.elmulardeoro.boletas.boleta.BoletaDtos.TableroDto;
import co.elmulardeoro.boletas.usuario.Usuario;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BoletaController {

    private final BoletaService servicio;

    public BoletaController(BoletaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/api/rifas/{rifaId}/tablero")
    TableroDto tablero(@PathVariable Long rifaId) {
        return servicio.tablero(rifaId);
    }

    @GetMapping("/api/rifas/{rifaId}/boletas/{numero}")
    BoletaDto detalle(@PathVariable Long rifaId, @PathVariable int numero) {
        return servicio.detalle(rifaId, numero);
    }

    @PostMapping("/api/rifas/{rifaId}/boletas/{numero}/apartar")
    BoletaDto apartar(@PathVariable Long rifaId, @PathVariable int numero, @Valid @RequestBody ApartarRequest req,
                      @AuthenticationPrincipal Usuario usuario) {
        return servicio.apartar(rifaId, numero, req, usuario);
    }

    @PostMapping("/api/rifas/{rifaId}/boletas/{numero}/pagos")
    BoletaDto pagar(@PathVariable Long rifaId, @PathVariable int numero, @Valid @RequestBody PagoRequest req,
                    @AuthenticationPrincipal Usuario usuario) {
        return servicio.registrarPago(rifaId, numero, req, usuario);
    }

    @PostMapping("/api/rifas/{rifaId}/boletas/{numero}/liberar")
    @PreAuthorize("hasRole('ADMIN')")
    BoletaDto liberar(@PathVariable Long rifaId, @PathVariable int numero, @Valid @RequestBody LiberarRequest req,
                      @AuthenticationPrincipal Usuario usuario) {
        return servicio.liberar(rifaId, numero, req.motivo(), usuario);
    }

    @PostMapping("/api/pagos/{id}/confirmar")
    @PreAuthorize("hasRole('ADMIN')")
    BoletaDto confirmar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servicio.confirmarPago(id, usuario);
    }

    @PostMapping("/api/pagos/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    BoletaDto rechazar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return servicio.rechazarPago(id, usuario);
    }

    @GetMapping("/api/rifas/{rifaId}/compradores")
    List<CompradorConBoletas> buscarCompradores(@PathVariable Long rifaId, @RequestParam String q) {
        return servicio.buscarCompradores(q, rifaId);
    }

    @PutMapping("/api/compradores/{id}")
    CompradorDto editarComprador(@PathVariable Long id, @Valid @RequestBody EditarComprador req) {
        return servicio.editarComprador(id, req);
    }
}
