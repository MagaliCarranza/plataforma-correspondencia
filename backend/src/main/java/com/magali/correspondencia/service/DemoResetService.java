package com.magali.correspondencia.service;

import com.magali.correspondencia.config.DemoProperties;
import com.magali.correspondencia.config.DemoSeeder;
import com.magali.correspondencia.exception.ReglaNegocioException;
import com.magali.correspondencia.model.Area;
import com.magali.correspondencia.model.Rol;
import com.magali.correspondencia.repository.AreaRepository;
import com.magali.correspondencia.repository.CorrespondenciaRepository;
import com.magali.correspondencia.repository.SecuenciaFolioRepository;
import com.magali.correspondencia.repository.UsuarioRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reinicio del modo demo. Elimina los datos sembrados (correspondencias, secuencias de folio,
 * cuentas demo y cuentas no-ADMIN creadas por visitantes, areas extra sin uso) y vuelve a
 * ejecutar {@link DemoSeeder#ejecutar()}. Conserva unicamente los ADMIN con {@code es_demo=false}
 * (el admin real sembrado por {@code InicializadorAdmin}).
 *
 * <p>Se ejecuta programaticamente por {@code POST /demo/reset} y en forma automatica por
 * {@link #resetearProgramado()} con cron/zona configurables.
 */
@Service
public class DemoResetService {

    private static final Logger log = LoggerFactory.getLogger(DemoResetService.class);

    private final DemoProperties demo;
    private final CorrespondenciaRepository correspondenciaRepository;
    private final SecuenciaFolioRepository secuenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final DemoSeeder demoSeeder;

    public DemoResetService(DemoProperties demo,
                            CorrespondenciaRepository correspondenciaRepository,
                            SecuenciaFolioRepository secuenciaRepository,
                            UsuarioRepository usuarioRepository,
                            AreaRepository areaRepository,
                            DemoSeeder demoSeeder) {
        this.demo = demo;
        this.correspondenciaRepository = correspondenciaRepository;
        this.secuenciaRepository = secuenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
        this.demoSeeder = demoSeeder;
    }

    @Transactional
    public void resetear() {
        if (!demo.habilitado()) {
            throw new ReglaNegocioException("El modo demo no esta habilitado.");
        }
        log.info("Reset demo: iniciando wipe y re-seed");

        // Orden importado por las FK: correspondencia -> usuario/area; usuario -> area.
        correspondenciaRepository.deleteAllInBatch();
        secuenciaRepository.deleteAllInBatch();
        int usuariosEliminados = usuarioRepository.borrarExceptoAdminReal(Rol.ADMIN);
        int areasEliminadas = borrarAreasExtraSinUso();
        reactivarAreasBase();

        demoSeeder.ejecutar();

        log.info("Reset demo completado. Usuarios eliminados: {}. Areas extra eliminadas: {}",
                usuariosEliminados, areasEliminadas);
    }

    @Scheduled(cron = "${correspondencia.demo.cron}", zone = "${correspondencia.demo.zona}")
    public void resetearProgramado() {
        if (!demo.habilitado()) {
            return;
        }
        log.info("Reset demo programado: disparando");
        resetear();
    }

    private int borrarAreasExtraSinUso() {
        List<Area> extras = areaRepository.findByNombreNotIn(DemoSeeder.AREAS_BASE);
        int borradas = 0;
        for (Area a : extras) {
            if (!usuarioRepository.existsByAreaId(a.getId())) {
                areaRepository.delete(a);
                borradas++;
            }
        }
        return borradas;
    }

    private void reactivarAreasBase() {
        for (String nombre : DemoSeeder.AREAS_BASE) {
            areaRepository.findByNombreIgnoreCase(nombre).ifPresent(a -> {
                if (!a.isActivo()) {
                    a.setActivo(true);
                    areaRepository.save(a);
                }
            });
        }
    }
}
