package com.magali.correspondencia.config;

import com.magali.correspondencia.model.Area;
import com.magali.correspondencia.model.Correspondencia;
import com.magali.correspondencia.model.EstadoCorrespondencia;
import com.magali.correspondencia.model.Prioridad;
import com.magali.correspondencia.model.Rol;
import com.magali.correspondencia.model.SecuenciaFolio;
import com.magali.correspondencia.model.TipoCorrespondencia;
import com.magali.correspondencia.model.TipoRemitente;
import com.magali.correspondencia.model.Usuario;
import com.magali.correspondencia.repository.AreaRepository;
import com.magali.correspondencia.repository.CorrespondenciaRepository;
import com.magali.correspondencia.repository.SecuenciaFolioRepository;
import com.magali.correspondencia.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra los usuarios demo y aproximadamente 150 correspondencias realistas de los
 * ultimos 60 dias. Es idempotente: si ya existe {@link #USERNAME_ADMIN_DEMO} no hace
 * nada, asi que puede correr al arrancar y volver a llamarse desde {@link DemoResetService}
 * despues del wipe.
 */
@Component
@Order(DemoSeeder.ORDEN)
public class DemoSeeder implements CommandLineRunner {

    public static final int ORDEN = InicializadorAdmin.ORDEN + 10;
    public static final String USERNAME_ADMIN_DEMO = "demo.admin";
    public static final String USERNAME_RECEP_DEMO = "demo.recep";
    public static final String USERNAME_SUPER_DEMO = "demo.super";
    public static final String USERNAME_MENSA_1_DEMO = "demo.mensa1";
    public static final String USERNAME_MENSA_2_DEMO = "demo.mensa2";

    public static final List<String> AREAS_BASE = List.of("Sistemas", "Recursos Humanos", "Direccion");
    public static final List<String> AREAS_DEMO_EXTRA = List.of("Finanzas", "Operaciones", "Juridico");

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);
    private static final long SEMILLA_RANDOM = 42L;
    private static final int TOTAL_CORRESPONDENCIAS = 150;
    private static final int DIAS_ATRAS = 60;

    // Distribucion de estados (suma 150).
    private static final int CANT_ARCHIVADA = 82;   // 55%
    private static final int CANT_ENTREGADA = 38;   // 25%
    private static final int CANT_EN_TRAMITE = 15;  // 10%
    private static final int CANT_RECIBIDA = 11;    // 7%
    private static final int CANT_ANULADA = 4;      // 3%

    private static final String[] REMITENTES_EXTERNOS = {
            "Juzgado Primero Civil CDMX",
            "SAT Delegacion Centro",
            "Infonavit Zona Oriente",
            "FedEx Mexico",
            "DHL Express",
            "Estafeta Paqueteria",
            "Comex Industrial S.A.",
            "Costco Mayoreo",
            "Dra. Ana Martinez Paredes",
            "Lic. Carlos Ramirez Soto",
            "Notaria Publica 42",
            "Secretaria de Economia",
            "Banco Nacional de Comercio",
            "Despacho Contable Fuentes",
            "Universidad Autonoma del Estado"
    };

    private static final String[] REMITENTES_INTERNOS = {
            "Direccion General",
            "Jefatura de Finanzas",
            "Subdireccion de Sistemas",
            "Coordinacion de Recursos Humanos",
            "Area de Operaciones",
            "Oficina Juridica",
            "Gerencia Administrativa"
    };

    // Asuntos por tipo.
    private static final Map<TipoCorrespondencia, String[]> ASUNTOS_POR_TIPO = Map.of(
            TipoCorrespondencia.OFICIO, new String[] {
                    "Respuesta a oficio 445/2026",
                    "Solicitud de informacion presupuestal",
                    "Notificacion judicial expediente 1203",
                    "Citatorio para audiencia preliminar",
                    "Confirmacion de recepcion de documentos"
            },
            TipoCorrespondencia.CIRCULAR, new String[] {
                    "Circular interna 03/2026 vacaciones",
                    "Comunicado de cambios de politica de seguridad",
                    "Aviso de mantenimiento programado",
                    "Circular de capacitacion anual"
            },
            TipoCorrespondencia.MEMO, new String[] {
                    "Memorandum reunion de direccion",
                    "Instrucciones del proyecto Q4",
                    "Autorizacion de viaticos",
                    "Seguimiento de acuerdos ultima reunion"
            },
            TipoCorrespondencia.INVITACION, new String[] {
                    "Invitacion a ceremonia de reconocimiento",
                    "Invitacion capacitacion ISO 9001",
                    "Invitacion evento anual de clientes",
                    "Invitacion rueda de prensa"
            },
            TipoCorrespondencia.OTRO, new String[] {
                    "Paqueteria entrega de documentos",
                    "Factura servicios profesionales 1234",
                    "Estado de cuenta corporativo octubre",
                    "Material publicitario proveedores",
                    "Entrega de muestras para revision"
            }
    );

    private static final String[] RECEPTORES_ENTREGA = {
            "Lic. Rodriguez Hernandez",
            "Secretaria del area",
            "Dra. Maria Perez Lopez",
            "Ing. Ramirez Castillo",
            "Encargado de turno",
            "Recepcion principal",
            "Asistente de direccion"
    };

    private static final String[] OBSERVACIONES = {
            "Documentos sellados y firmados.",
            "Entrega sin observaciones.",
            "Pendiente de confirmar acuse.",
            "El destinatario solicito copia adicional.",
            "Recibido en horario de oficina.",
            null, null, null // probabilidad de no tener observaciones
    };

    private static final String[] MOTIVOS_ANULACION = {
            "Folio duplicado detectado al momento del registro.",
            "Documento recibido por error, se devuelve al remitente.",
            "Datos del remitente ilegibles, se solicita nuevo envio.",
            "Correspondencia dirigida a otra organizacion."
    };

    private static final String[] MOTIVOS_ARCHIVO = {
            "Expediente concluido.",
            "Tramite completado y acusado.",
            "Archivado como respaldo documental.",
            "Caso cerrado sin pendientes."
    };

    private final DemoProperties demo;
    private final AreaRepository areaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorrespondenciaRepository correspondenciaRepository;
    private final SecuenciaFolioRepository secuenciaRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoSeeder(DemoProperties demo,
                      AreaRepository areaRepository,
                      UsuarioRepository usuarioRepository,
                      CorrespondenciaRepository correspondenciaRepository,
                      SecuenciaFolioRepository secuenciaRepository,
                      PasswordEncoder passwordEncoder) {
        this.demo = demo;
        this.areaRepository = areaRepository;
        this.usuarioRepository = usuarioRepository;
        this.correspondenciaRepository = correspondenciaRepository;
        this.secuenciaRepository = secuenciaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!demo.habilitado()) {
            return;
        }
        ejecutar();
    }

    @Transactional
    public void ejecutar() {
        if (usuarioRepository.existsByUsername(USERNAME_ADMIN_DEMO)) {
            return;
        }
        log.info("Modo demo: sembrando usuarios demo y {} correspondencias", TOTAL_CORRESPONDENCIAS);

        Map<String, Area> areas = sembrarAreas();
        UsuariosDemo usuariosDemo = sembrarUsuariosDemo(areas);
        sembrarCorrespondencias(areas, usuariosDemo);
    }

    private Map<String, Area> sembrarAreas() {
        Map<String, Area> mapa = new LinkedHashMap<>();
        List<String> todas = new ArrayList<>(AREAS_BASE);
        todas.addAll(AREAS_DEMO_EXTRA);
        for (String nombre : todas) {
            Area area = areaRepository.findByNombreIgnoreCase(nombre)
                    .orElseGet(() -> areaRepository.save(
                            Area.builder().nombre(nombre).activo(true).build()));
            if (!area.isActivo()) {
                area.setActivo(true);
                area = areaRepository.save(area);
            }
            mapa.put(nombre, area);
        }
        return mapa;
    }

    private UsuariosDemo sembrarUsuariosDemo(Map<String, Area> areas) {
        String passwordHash = passwordEncoder.encode(demo.password());
        Usuario admin = crearUsuarioDemo(USERNAME_ADMIN_DEMO, "Demo Administrador",
                "demo.admin@correspondencia.local", Rol.ADMIN, areas.get("Sistemas"), passwordHash);
        Usuario recep = crearUsuarioDemo(USERNAME_RECEP_DEMO, "Demo Recepcionista",
                "demo.recep@correspondencia.local", Rol.RECEPCIONISTA, areas.get("Recursos Humanos"), passwordHash);
        Usuario supervisor = crearUsuarioDemo(USERNAME_SUPER_DEMO, "Demo Supervisor",
                "demo.super@correspondencia.local", Rol.SUPERVISOR, areas.get("Direccion"), passwordHash);
        Usuario mensa1 = crearUsuarioDemo(USERNAME_MENSA_1_DEMO, "Demo Mensajero Uno",
                "demo.mensa1@correspondencia.local", Rol.MENSAJERO, areas.get("Operaciones"), passwordHash);
        Usuario mensa2 = crearUsuarioDemo(USERNAME_MENSA_2_DEMO, "Demo Mensajero Dos",
                "demo.mensa2@correspondencia.local", Rol.MENSAJERO, areas.get("Operaciones"), passwordHash);
        return new UsuariosDemo(admin, recep, supervisor, List.of(mensa1, mensa2));
    }

    private Usuario crearUsuarioDemo(String username, String nombreCompleto, String email,
                                     Rol rol, Area area, String passwordHash) {
        Usuario usuario = Usuario.builder()
                .username(username)
                .nombreCompleto(nombreCompleto)
                .email(email)
                .passwordHash(passwordHash)
                .rol(rol)
                .area(area)
                .debeCambiarPassword(false)
                .esDemo(true)
                .build();
        return usuarioRepository.save(usuario);
    }

    private void sembrarCorrespondencias(Map<String, Area> areas, UsuariosDemo usuarios) {
        Random rnd = new Random(SEMILLA_RANDOM);
        Instant ahora = Instant.now();
        Instant inicio = ahora.minus(Duration.ofDays(DIAS_ATRAS));

        // 1) Generar primero las fechas y estados para cada correspondencia, ordenarlas por
        //    fechaRecepcion y recien despues generar folios: asi el folio N+1 siempre es
        //    posterior al N (con el anio correcto).
        List<PlanCorrespondencia> plan = new ArrayList<>(TOTAL_CORRESPONDENCIAS);
        agregarPlanes(plan, CANT_ARCHIVADA, EstadoCorrespondencia.ARCHIVADA, rnd, inicio, ahora);
        agregarPlanes(plan, CANT_ENTREGADA, EstadoCorrespondencia.ENTREGADA, rnd, inicio, ahora);
        agregarPlanes(plan, CANT_EN_TRAMITE, EstadoCorrespondencia.EN_TRAMITE, rnd, inicio, ahora);
        agregarPlanes(plan, CANT_RECIBIDA, EstadoCorrespondencia.RECIBIDA, rnd, inicio, ahora);
        agregarPlanes(plan, CANT_ANULADA, EstadoCorrespondencia.ANULADA, rnd, inicio, ahora);
        plan.sort(Comparator.comparing(p -> p.recepcion));

        List<Area> areasList = new ArrayList<>(areas.values());
        TipoCorrespondencia[] tipos = TipoCorrespondencia.values();
        Prioridad[] prioridades = Prioridad.values();
        TipoRemitente[] tiposRem = TipoRemitente.values();
        Map<Integer, SecuenciaFolio> secuencias = new HashMap<>();

        for (PlanCorrespondencia p : plan) {
            TipoRemitente tipoRem = tiposRem[rnd.nextInt(tiposRem.length)];
            String remitente = tipoRem == TipoRemitente.EXTERNO
                    ? REMITENTES_EXTERNOS[rnd.nextInt(REMITENTES_EXTERNOS.length)]
                    : REMITENTES_INTERNOS[rnd.nextInt(REMITENTES_INTERNOS.length)];
            TipoCorrespondencia tipo = tipos[rnd.nextInt(tipos.length)];
            String[] asuntos = ASUNTOS_POR_TIPO.get(tipo);
            String asunto = asuntos[rnd.nextInt(asuntos.length)];
            Prioridad prioridad = rnd.nextInt(10) < 2 ? Prioridad.URGENTE : Prioridad.NORMAL;
            Area area = areasList.get(rnd.nextInt(areasList.size()));
            String observaciones = OBSERVACIONES[rnd.nextInt(OBSERVACIONES.length)];
            String folio = siguienteFolio(p.recepcion, secuencias);

            Correspondencia c = Correspondencia.builder()
                    .folio(folio)
                    .fechaRecepcion(p.recepcion)
                    .remitente(remitente)
                    .tipoRemitente(tipoRem)
                    .asunto(asunto)
                    .tipo(tipo)
                    .prioridad(prioridad)
                    .areaDestino(area)
                    .observaciones(observaciones)
                    .estado(p.estado)
                    .recepcionadoPor(usuarios.recep)
                    .build();

            // Rellenar transiciones en orden cronologico segun el estado.
            if (p.estado == EstadoCorrespondencia.EN_TRAMITE
                    || p.estado == EstadoCorrespondencia.ENTREGADA
                    || p.estado == EstadoCorrespondencia.ARCHIVADA) {
                c.setMensajero(usuarios.mensajeros.get(rnd.nextInt(usuarios.mensajeros.size())));
                c.setFechaAsignacion(p.asignacion);
            }
            if (p.estado == EstadoCorrespondencia.ENTREGADA
                    || p.estado == EstadoCorrespondencia.ARCHIVADA) {
                c.setFechaEntrega(p.entrega);
                c.setNombreRecibe(RECEPTORES_ENTREGA[rnd.nextInt(RECEPTORES_ENTREGA.length)]);
                c.setObservacionesEntrega("Entregado en el domicilio del destinatario.");
            }
            if (p.estado == EstadoCorrespondencia.ARCHIVADA) {
                c.setArchivadoEn(p.archivo);
                c.setArchivadoPor(usuarios.supervisor);
                c.setMotivoArchivo(MOTIVOS_ARCHIVO[rnd.nextInt(MOTIVOS_ARCHIVO.length)]);
            }
            if (p.estado == EstadoCorrespondencia.ANULADA) {
                c.setAnuladoEn(p.anulacion);
                c.setAnuladoPor(usuarios.recep);
                c.setMotivoAnulacion(MOTIVOS_ANULACION[rnd.nextInt(MOTIVOS_ANULACION.length)]);
            }

            correspondenciaRepository.save(c);
        }
        // Persistir contadores de folio actualizados.
        secuencias.values().forEach(secuenciaRepository::save);
    }

    private static void agregarPlanes(List<PlanCorrespondencia> acumulador, int cantidad,
                                      EstadoCorrespondencia estado, Random rnd,
                                      Instant inicio, Instant fin) {
        long rangoSegundos = Duration.between(inicio, fin).toSeconds();
        for (int i = 0; i < cantidad; i++) {
            long offset = (long) (rnd.nextDouble() * rangoSegundos);
            Instant recepcion = inicio.plusSeconds(offset);
            Instant asignacion = null;
            Instant entrega = null;
            Instant archivo = null;
            Instant anulacion = null;
            switch (estado) {
                case RECIBIDA:
                    break;
                case EN_TRAMITE:
                    asignacion = recepcion.plus(Duration.ofHours(1 + rnd.nextInt(24)));
                    break;
                case ENTREGADA:
                    asignacion = recepcion.plus(Duration.ofHours(1 + rnd.nextInt(24)));
                    entrega = asignacion.plus(Duration.ofHours(1 + rnd.nextInt(48)));
                    break;
                case ARCHIVADA:
                    asignacion = recepcion.plus(Duration.ofHours(1 + rnd.nextInt(24)));
                    entrega = asignacion.plus(Duration.ofHours(1 + rnd.nextInt(48)));
                    archivo = entrega.plus(Duration.ofHours(1 + rnd.nextInt(48)));
                    if (archivo.isAfter(fin)) {
                        archivo = fin;
                    }
                    break;
                case ANULADA:
                    anulacion = recepcion.plus(Duration.ofHours(1 + rnd.nextInt(12)));
                    break;
            }
            // Cualquier evento posterior al "ahora" se recorta.
            if (asignacion != null && asignacion.isAfter(fin)) {
                asignacion = fin;
            }
            if (entrega != null && entrega.isAfter(fin)) {
                entrega = fin;
            }
            if (anulacion != null && anulacion.isAfter(fin)) {
                anulacion = fin;
            }
            acumulador.add(new PlanCorrespondencia(estado, recepcion, asignacion, entrega, archivo, anulacion));
        }
    }

    private String siguienteFolio(Instant fechaRecepcion, Map<Integer, SecuenciaFolio> cache) {
        int anio = fechaRecepcion.atZone(ZoneOffset.UTC).getYear();
        SecuenciaFolio sec = cache.computeIfAbsent(anio, a ->
                secuenciaRepository.findById(a).orElse(new SecuenciaFolio(a, 0)));
        int siguiente = sec.getUltimoNumero() + 1;
        sec.setUltimoNumero(siguiente);
        return String.format("CORR-%d-%04d", anio, siguiente);
    }

    public record UsuariosDemo(Usuario admin, Usuario recep, Usuario supervisor, List<Usuario> mensajeros) {

        public UsuariosDemo {
            mensajeros = Collections.unmodifiableList(new ArrayList<>(mensajeros));
        }
    }

    private record PlanCorrespondencia(EstadoCorrespondencia estado, Instant recepcion,
                                       Instant asignacion, Instant entrega,
                                       Instant archivo, Instant anulacion) {}
}
