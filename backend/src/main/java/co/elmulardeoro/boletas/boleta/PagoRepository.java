package co.elmulardeoro.boletas.boleta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    @Query("select p from Pago p join fetch p.venta v join fetch v.boleta b join fetch v.comprador"
            + " join fetch p.registradoPor where b.rifaId = :rifaId and p.estado = :estado order by p.registradoEn")
    List<Pago> deRifaEnEstado(Long rifaId, EstadoPago estado);
}
