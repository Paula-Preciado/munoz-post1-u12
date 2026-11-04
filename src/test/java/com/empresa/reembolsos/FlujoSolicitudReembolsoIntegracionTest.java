package com.empresa.reembolsos;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorAuditoria;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorContabilidad;
import com.empresa.reembolsos.procesamiento.notificacion.NotificadorEmpleado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion del flujo completo con el contexto de Spring, H2 y la API REST:
 * crear -> validar (cadena) -> calcular (estrategia) -> cambiar estado -> notificar (observadores).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(OutputCaptureExtension.class)
class FlujoSolicitudReembolsoIntegracionTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @SpyBean private NotificadorEmpleado empleado;
    @SpyBean private NotificadorContabilidad contabilidad;
    @SpyBean private NotificadorAuditoria auditoria;

    private String cuerpo(String empleadoId, String categoria, String monto, int dias, boolean docs) {
        return """
               {"empleadoId":"%s","categoria":"%s","montoSolicitado":%s,
                "fechaGasto":"2026-10-15","diasViaje":%d,"documentosSoporte":%s}
               """.formatted(empleadoId, categoria, monto, dias, docs);
    }

    private SolicitudReembolso crear(String cuerpo) throws Exception {
        String respuesta = mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readValue(respuesta, SolicitudReembolso.class);
    }

    @Test
    void creaSolicitudesDeLasTresCategoriasConSuMontoAprobado() throws Exception {
        assertThat(crear(cuerpo("E-100", "VIAJE", "140000", 1, true)).getMontoAprobado()).isEqualByComparingTo("140000");
        assertThat(crear(cuerpo("E-100", "REPRESENTACION", "1800000", 0, true)).getMontoAprobado()).isEqualByComparingTo("1800000");
        SolicitudReembolso capacitacion = crear(cuerpo("E-100", "CAPACITACION", "2500000", 0, true));
        assertThat(capacitacion.getMontoAprobado()).isEqualByComparingTo("2000000"); // 80%
        assertThat(capacitacion.getEstado()).isEqualTo(EstadoSolicitud.PENDIENTE);
        assertThat(capacitacion.getId()).isNotNull();
    }

    @Test
    void rechazaSinDocumentosMontoExcesivoYDuplicadoConMensajeClaro() throws Exception {
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("E-200", "VIAJE", "100000", 1, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Faltan documentos soporte"));

        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("E-200", "REPRESENTACION", "2500000", 0, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Monto excede el tope de la categoria REPRESENTACION"));

        crear(cuerpo("E-200", "CAPACITACION", "1000000", 0, true));
        mvc.perform(post("/api/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo("E-200", "CAPACITACION", "500000", 0, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ya existe una solicitud pendiente de la misma categoria en el periodo"));
    }

    @Test
    void cambiarEstadoDisparaLosTresObservadoresYContabilidadSoloConAprobada(CapturedOutput salida) throws Exception {
        SolicitudReembolso rechazada = crear(cuerpo("E-300", "VIAJE", "90000", 1, true));
        mvc.perform(put("/api/solicitudes/{id}/estado", rechazada.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("\"RECHAZADA\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
        assertThat(salida).doesNotContain("Contabilidad: programar pago");

        SolicitudReembolso aprobada = crear(cuerpo("E-300", "CAPACITACION", "1000000", 0, true));
        mvc.perform(put("/api/solicitudes/{id}/estado", aprobada.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("\"APROBADA\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"));

        verify(empleado, times(2)).onCambioEstado(any(), eq(EstadoSolicitud.PENDIENTE));
        verify(contabilidad, times(2)).onCambioEstado(any(), any());
        verify(auditoria, times(2)).onCambioEstado(any(), any());
        assertThat(salida)
                .contains("Notificando a E-300: su solicitud paso de PENDIENTE a RECHAZADA")
                .contains("Notificando a E-300: su solicitud paso de PENDIENTE a APROBADA")
                .contains("Contabilidad: programar pago de 800000")
                .contains("Auditoria: solicitud " + aprobada.getId() + " cambio de PENDIENTE a APROBADA");
    }

    @Test
    void cambiarEstadoDeSolicitudInexistente_devuelve404() throws Exception {
        mvc.perform(put("/api/solicitudes/{id}/estado", 999)
                        .contentType(MediaType.APPLICATION_JSON).content("\"APROBADA\""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Solicitud no encontrada: 999"));
    }
}
