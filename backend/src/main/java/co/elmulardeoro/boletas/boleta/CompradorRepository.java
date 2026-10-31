package co.elmulardeoro.boletas.boleta;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompradorRepository extends JpaRepository<Comprador, Long> {

    Optional<Comprador> findFirstByCelularOrderByIdAsc(String celular);

    @Query("select c from Comprador c where lower(c.nombre) like lower(concat('%', :q, '%'))"
            + " or c.celular like concat('%', :q, '%') order by c.nombre")
    List<Comprador> buscar(String q);
}
