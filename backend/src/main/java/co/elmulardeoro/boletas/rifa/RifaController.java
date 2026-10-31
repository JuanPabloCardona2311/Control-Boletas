package co.elmulardeoro.boletas.rifa;

import co.elmulardeoro.boletas.rifa.RifaDtos.CrearRifa;
import co.elmulardeoro.boletas.rifa.RifaDtos.EditarRifa;
import co.elmulardeoro.boletas.rifa.RifaDtos.RifaDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rifas")
public class RifaController {

    private final RifaService servicio;

    public RifaController(RifaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/activa")
    RifaDto activa() {
        return servicio.activa();
    }

    @GetMapping
    List<RifaDto> listar() {
        return servicio.listar();
    }

    @GetMapping("/{id}")
    RifaDto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    RifaDto crear(@Valid @RequestBody CrearRifa req) {
        return servicio.crear(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    RifaDto editar(@PathVariable Long id, @Valid @RequestBody EditarRifa req) {
        return servicio.editar(id, req);
    }
}
