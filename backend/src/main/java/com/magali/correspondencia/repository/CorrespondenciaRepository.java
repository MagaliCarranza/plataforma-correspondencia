package com.magali.correspondencia.repository;

import com.magali.correspondencia.model.Correspondencia;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CorrespondenciaRepository
        extends JpaRepository<Correspondencia, Long>, JpaSpecificationExecutor<Correspondencia> {

    /**
     * Override de {@link JpaSpecificationExecutor#findAll(Specification, Sort)} con fetch join
     * de las relaciones LAZY que usa el reporte PDF (area destino, mensajero y recepcionador).
     * Evita N+1 al generar el reporte sobre cientos de correspondencias.
     *
     * <p>Las paginaciones y filtros interactivos usan otras firmas ({@code findAll(spec, Pageable)}
     * y {@code findAll(spec)}) y no se ven afectadas.
     */
    @Override
    @EntityGraph(attributePaths = {"areaDestino", "mensajero", "recepcionadoPor"})
    List<Correspondencia> findAll(Specification<Correspondencia> spec, Sort sort);
}
