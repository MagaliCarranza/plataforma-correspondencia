package com.magali.correspondencia.controller;

import com.magali.correspondencia.config.DemoProperties;
import com.magali.correspondencia.config.DemoSeeder;
import com.magali.correspondencia.dto.DemoInfoResponse;
import com.magali.correspondencia.dto.DemoInfoResponse.UsuarioDemoInfo;
import com.magali.correspondencia.service.DemoResetService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del modo demo. {@code GET /demo/info} es publico (se consulta desde
 * la pantalla de login para mostrar los botones "Entrar como ..."). {@code POST /demo/reset}
 * dispara el reset manual y solo lo puede invocar un ADMIN.
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final List<UsuarioDemoInfo> USUARIOS_DEMO = List.of(
            new UsuarioDemoInfo(DemoSeeder.USERNAME_ADMIN_DEMO, "ADMIN",
                    "Gestiona usuarios, areas y toda la correspondencia."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_RECEP_DEMO, "RECEPCIONISTA",
                    "Registra correspondencia entrante y asigna mensajeros."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_SUPER_DEMO, "SUPERVISOR",
                    "Consulta, asigna mensajeros, archiva y genera reportes PDF."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_MENSA_1_DEMO, "MENSAJERO",
                    "Registra la entrega de la correspondencia asignada (mensajero 1)."),
            new UsuarioDemoInfo(DemoSeeder.USERNAME_MENSA_2_DEMO, "MENSAJERO",
                    "Registra la entrega de la correspondencia asignada (mensajero 2).")
    );

    private final DemoProperties demo;
    private final DemoResetService demoResetService;

    public DemoController(DemoProperties demo, DemoResetService demoResetService) {
        this.demo = demo;
        this.demoResetService = demoResetService;
    }

    @GetMapping("/info")
    public DemoInfoResponse info() {
        if (!demo.habilitado()) {
            return new DemoInfoResponse(false, null, List.of());
        }
        return new DemoInfoResponse(true, demo.password(), USUARIOS_DEMO);
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        demoResetService.resetear();
    }
}
