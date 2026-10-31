package co.elmulardeoro.boletas.rifa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RifaRepository extends JpaRepository<Rifa, Long> {

    Optional<Rifa> findByActivaTrue();

    List<Rifa> findAllByOrderByCreadaEnDesc();

    @Modifying
    @Query("update Rifa r set r.activa = false where r.activa = true")
    int archivarActiva();

    @Modifying
    @Query(value = "INSERT INTO boleta (rifa_id, numero) SELECT :rifaId, generate_series(0, :cantidad - 1)", nativeQuery = true)
    int crearBoletas(Long rifaId, int cantidad);
}
