package co.elmulardeoro.boletas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlujoBoletasTest {

    private static final String RIFA = """
            {"nombre": "Gran Rifa diciembre 2026", "cantidadNumeros": 1000, "precio": 60000,
             "loteria": "Lotería de Medellín", "fechaSorteo": "2026-12-18",
             "diasRecordatorio": 14, "diasLiberacion": 7, "responsables": "Julián y Adriana",
             "premios": [
               {"descripcion": "Moto Honda XR 190", "posicion": "PRIMERAS", "cifras": 3, "valorEfectivo": 15000000},
               {"descripcion": "Muleta de 42 meses aperada", "posicion": "ULTIMAS", "cifras": 3, "valorEfectivo": 15000000}
             ]}
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    String admin;
    String vendedor;
    long rifaId;

    @BeforeEach
    void preparar() throws Exception {
        jdbc.sql("TRUNCATE pago, venta, boleta, premio, rifa, comprador RESTART IDENTITY CASCADE").update();
        jdbc.sql("DELETE FROM usuario WHERE usuario <> 'admin'").update();
        admin = login("admin", "admin123");
        enviar(post("/api/usuarios"), admin, """
                {"nombre": "Juan Pablo", "usuario": "juanpa", "clave": "secreta1", "rol": "VENDEDOR", "celular": "3001234567"}
                """).andExpect(status().isCreated());
        vendedor = login("juanpa", "secreta1");
        String rifa = enviar(post("/api/rifas"), admin, RIFA).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        rifaId = ((Number) JsonPath.read(rifa, "$.id")).longValue();
    }

    @Test
    void loginIncorrectoNoEntra() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"admin\", \"clave\": \"mala\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/rifas/activa")).andExpect(status().isUnauthorized());
    }

    @Test
    void elFrontendPuedeLlamarDesdeOtroDominio() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header()
                        .string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void apartarAbonarYConfirmarTransferencia() throws Exception {
        apartar(777, """
                {"nombre": "Pedro Pérez", "celular": "311 555 1234", "ubicacion": "Santo Domingo",
                 "abono": {"valor": 20000, "metodo": "EFECTIVO"}}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABONADA"))
                .andExpect(jsonPath("$.venta.saldo").value(40000))
                .andExpect(jsonPath("$.venta.comprador.celular").value("3115551234"));

        // Otro vendedor no puede quedarse con el mismo número
        apartar(777, "{\"nombre\": \"Otra\", \"celular\": \"3000000000\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El 777 ya lo tiene Pedro Pérez"));

        String boleta = enviar(post(url("/boletas/777/pagos")), vendedor, "{\"valor\": 40000, \"metodo\": \"TRANSFERENCIA\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABONADA"))
                .andExpect(jsonPath("$.venta.porConfirmar").value(40000))
                .andReturn().getResponse().getContentAsString();
        long pagoId = ((Number) JsonPath.read(boleta, "$.venta.pagos[1].id")).longValue();

        // No se puede pagar más de lo que falta, contando lo que está por confirmar
        enviar(post(url("/boletas/777/pagos")), vendedor, "{\"valor\": 1000, \"metodo\": \"EFECTIVO\"}")
                .andExpect(status().isBadRequest());

        mvc.perform(conToken(get(url("/tablero")), vendedor))
                .andExpect(jsonPath("$.conteo.abonadas").value(1))
                .andExpect(jsonPath("$.conteo.libres").value(999))
                .andExpect(jsonPath("$.ocupadas[0].porConfirmar").value(true));

        mvc.perform(conToken(post("/api/pagos/" + pagoId + "/confirmar"), vendedor)).andExpect(status().isForbidden());
        mvc.perform(conToken(get(url("/transferencias-pendientes")), admin)).andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(conToken(post("/api/pagos/" + pagoId + "/confirmar"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.venta.saldo").value(0));

        mvc.perform(conToken(get(url("/resumen")), admin))
                .andExpect(jsonPath("$.recaudado").value(60000))
                .andExpect(jsonPath("$.porCobrar").value(0))
                .andExpect(jsonPath("$.efectivoPorVendedor[0].total").value(20000));
    }

    @Test
    void elMismoCelularEsElMismoComprador() throws Exception {
        apartar(1, "{\"nombre\": \"Pedro\", \"celular\": \"3115551234\"}").andExpect(status().isOk());
        apartar(2, "{\"nombre\": \"Pedro P.\", \"celular\": \"311-555-1234\"}").andExpect(status().isOk());

        mvc.perform(conToken(get(url("/compradores")).param("q", "pedro"), vendedor))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].boletas", hasSize(2)))
                .andExpect(jsonPath("$[0].boletas[0].numeroTexto").value("001"));
    }

    @Test
    void liberarDejaElNumeroLibreYGuardaElHistorial() throws Exception {
        apartar(5, """
                {"nombre": "María", "celular": "3120000000", "abono": {"valor": 10000, "metodo": "EFECTIVO"}}
                """).andExpect(status().isOk());
        apartar(6, "{\"nombre\": \"Luis\", \"celular\": \"3130000000\"}").andExpect(status().isOk());

        mvc.perform(conToken(get(url("/deudores")), vendedor))
                .andExpect(jsonPath("$.fechaRecordatorio").value("2026-12-04"))
                .andExpect(jsonPath("$.fechaLiberacion").value("2026-12-11"))
                .andExpect(jsonPath("$.deudores", hasSize(2)));

        enviar(post(url("/boletas/5/liberar")), vendedor, "{}").andExpect(status().isForbidden());
        enviar(post(url("/boletas/5/liberar")), admin, "{\"motivo\": \"No pagó a tiempo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LIBRE"))
                .andExpect(jsonPath("$.historial[0].comprador").value("María"))
                .andExpect(jsonPath("$.historial[0].abonado").value(10000));

        // El abono de María no le cuenta al siguiente comprador
        apartar(5, "{\"nombre\": \"Ana\", \"celular\": \"3140000000\"}")
                .andExpect(jsonPath("$.estado").value("APARTADA"))
                .andExpect(jsonPath("$.venta.pagado").value(0));

        mvc.perform(conToken(get(url("/resumen")), admin))
                .andExpect(jsonPath("$.abonosPerdidos").value(10000))
                .andExpect(jsonPath("$.vendidas").value(2));
    }

    @Test
    void nuevaRifaArchivaLaAnterior() throws Exception {
        apartar(10, "{\"nombre\": \"Pedro\", \"celular\": \"3115551234\"}").andExpect(status().isOk());
        String nueva = enviar(post("/api/rifas"), admin, RIFA.replace("\"cantidadNumeros\": 1000", "\"cantidadNumeros\": 10000")
                .replace("60000", "80000")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.cifras").value(4))
                .andReturn().getResponse().getContentAsString();
        long nuevaId = ((Number) JsonPath.read(nueva, "$.id")).longValue();

        mvc.perform(conToken(get("/api/rifas/activa"), vendedor)).andExpect(jsonPath("$.id").value(nuevaId));
        mvc.perform(conToken(get("/api/rifas/" + nuevaId + "/tablero"), vendedor))
                .andExpect(jsonPath("$.conteo.libres").value(10000));
        // La vieja se puede consultar pero ya no modificar
        mvc.perform(conToken(get(url("/boletas/10")), vendedor)).andExpect(jsonPath("$.estado").value("APARTADA"));
        apartar(11, "{\"nombre\": \"Pedro\", \"celular\": \"3115551234\"}").andExpect(status().isConflict());
    }

    @Test
    void resultadoDelSorteoMuestraGanadores() throws Exception {
        apartar(123, "{\"nombre\": \"Pedro\", \"celular\": \"3115551234\", \"abono\": {\"valor\": 60000, \"metodo\": \"EFECTIVO\"}}");
        apartar(234, "{\"nombre\": \"María\", \"celular\": \"3120000000\"}");

        enviar(post(url("/resultado")), admin, "{\"resultado\": \"1234\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ganadores[0].numeroTexto").value("123"))
                .andExpect(jsonPath("$.ganadores[0].comprador").value("Pedro"))
                .andExpect(jsonPath("$.ganadores[0].juega").value(true))
                .andExpect(jsonPath("$.ganadores[1].numeroTexto").value("234"))
                .andExpect(jsonPath("$.ganadores[1].juega").value(false));
    }

    @Test
    void dosVendedoresAlMismoTiempoSoloUnoSeQuedaConElNumero() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<Integer>> intentos = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String celular = "31000000" + (10 + i);
            intentos.add(() -> apartar(500, "{\"nombre\": \"C\", \"celular\": \"" + celular + "\"}")
                    .andReturn().getResponse().getStatus());
        }
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> f : pool.invokeAll(intentos)) {
            estados.add(f.get());
        }
        pool.shutdown();
        assertThat(estados).filteredOn(s -> s == 200).hasSize(1);
        assertThat(estados).filteredOn(s -> s == 409).hasSize(7);
    }

    private ResultActions apartar(int numero, String cuerpo) throws Exception {
        return enviar(post(url("/boletas/" + numero + "/apartar")), vendedor, cuerpo);
    }

    private String url(String ruta) {
        return "/api/rifas/" + rifaId + ruta;
    }

    private String login(String usuario, String clave) throws Exception {
        String respuesta = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"" + usuario + "\", \"clave\": \"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private ResultActions enviar(MockHttpServletRequestBuilder req, String token, String cuerpo) throws Exception {
        return mvc.perform(conToken(req, token).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }
}
