package co.elmulardeoro.boletas.boleta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query("select v from Venta v join fetch v.comprador left join fetch v.liberadaPor"
            + " where v.boleta.id = :boletaId and v.liberadaEn is not null order by v.liberadaEn desc")
    List<Venta> liberadas(Long boletaId);

    @Query("select distinct v from Venta v join fetch v.boleta b join fetch v.comprador join fetch v.vendedor"
            + " left join fetch v.pagos where b.rifaId = :rifaId and v.liberadaEn is null")
    List<Venta> vigentes(Long rifaId);

    @Query("select v from Venta v join fetch v.boleta b where v.comprador.id in :compradores"
            + " and b.rifaId = :rifaId and v.liberadaEn is null order by b.numero")
    List<Venta> vigentesDe(List<Long> compradores, Long rifaId);
}
