package co.elmulardeoro.boletas.boleta;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    /** Bloquea la fila para que dos vendedores no aparten ni paguen el mismo número al mismo tiempo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Boleta b where b.rifaId = :rifaId and b.numero = :numero")
    Optional<Boleta> bloquear(Long rifaId, int numero);

    Optional<Boleta> findByRifaIdAndNumero(Long rifaId, int numero);
}
