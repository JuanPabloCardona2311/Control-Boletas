package co.elmulardeoro.boletas.rifa;

import co.elmulardeoro.boletas.comun.ApiException;
import co.elmulardeoro.boletas.rifa.RifaDtos.CrearRifa;
import co.elmulardeoro.boletas.rifa.RifaDtos.EditarRifa;
import co.elmulardeoro.boletas.rifa.RifaDtos.PremioDto;
import co.elmulardeoro.boletas.rifa.RifaDtos.RifaDto;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RifaService {

    private static final Set<Integer> CANTIDADES = Set.of(100, 1000, 10000);

    private final RifaRepository rifas;

    public RifaService(RifaRepository rifas) {
        this.rifas = rifas;
    }

    @Transactional(readOnly = true)
    public RifaDto activa() {
        return RifaDto.de(rifas.findByActivaTrue().orElseThrow(() -> ApiException.noEncontrado("No hay una rifa activa")));
    }

    @Transactional(readOnly = true)
    public List<RifaDto> listar() {
        return rifas.findAllByOrderByCreadaEnDesc().stream().map(RifaDto::de).toList();
    }

    @Transactional(readOnly = true)
    public RifaDto obtener(Long id) {
        return RifaDto.de(buscar(id));
    }

    /** Archiva la rifa activa (con todo su historial) y deja la nueva con todas sus boletas libres. */
    @Transactional
    public RifaDto crear(CrearRifa req) {
        if (!CANTIDADES.contains(req.cantidadNumeros())) {
            throw ApiException.invalido("La cantidad de números debe ser 100, 1000 o 10000");
        }
        rifas.archivarActiva();
        rifas.flush();
        Rifa rifa = new Rifa(req.nombre().trim(), req.cantidadNumeros(), req.precio(), req.loteria().trim(), req.fechaSorteo());
        aplicar(rifa, req.diasRecordatorio(), req.diasLiberacion(), req.responsables(), req.premios());
        rifa = rifas.saveAndFlush(rifa);
        rifas.crearBoletas(rifa.getId(), rifa.getCantidadNumeros());
        return RifaDto.de(rifa);
    }

    @Transactional
    public RifaDto editar(Long id, EditarRifa req) {
        Rifa rifa = buscar(id);
        rifa.setNombre(req.nombre().trim());
        rifa.setPrecio(req.precio());
        rifa.setLoteria(req.loteria().trim());
        rifa.setFechaSorteo(req.fechaSorteo());
        aplicar(rifa, req.diasRecordatorio(), req.diasLiberacion(), req.responsables(), req.premios());
        return RifaDto.de(rifa);
    }

    private void aplicar(Rifa rifa, int diasRecordatorio, int diasLiberacion, String responsables, List<PremioDto> premios) {
        if (diasLiberacion > diasRecordatorio) {
            throw ApiException.invalido("La liberación debe ser después del recordatorio (menos días antes del sorteo)");
        }
        rifa.setDiasRecordatorio(diasRecordatorio);
        rifa.setDiasLiberacion(diasLiberacion);
        rifa.setResponsables(responsables);
        rifa.getPremios().clear();
        for (int i = 0; i < premios.size(); i++) {
            PremioDto p = premios.get(i);
            if (p.cifras() > rifa.cifras()) {
                throw ApiException.invalido("Un premio no puede jugar con más cifras que las de la boleta (" + rifa.cifras() + ")");
            }
            rifa.getPremios().add(new Premio(rifa, p.descripcion().trim(), p.posicion(), p.cifras(), p.valorEfectivo(), i));
        }
    }

    Rifa buscar(Long id) {
        return rifas.findById(id).orElseThrow(() -> ApiException.noEncontrado("Rifa no encontrada"));
    }
}
