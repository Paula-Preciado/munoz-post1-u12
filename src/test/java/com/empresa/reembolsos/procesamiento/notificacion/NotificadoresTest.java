package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;

import static com.empresa.reembolsos.soporte.Solicitudes.valida;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class NotificadoresTest {

    private SolicitudReembolso conEstado(EstadoSolicitud estado) {
        SolicitudReembolso s = valida(CategoriaGasto.REPRESENTACION, "500000");
        s.setId(7L);
        s.setMontoAprobado(new BigDecimal("500000"));
        s.setEstado(estado);
        return s;
    }

    @Test
    void empleado_seEnteraDeCualquierCambio(CapturedOutput salida) {
        new NotificadorEmpleado().onCambioEstado(conEstado(EstadoSolicitud.RECHAZADA), EstadoSolicitud.PENDIENTE);
        assertThat(salida).contains("Notificando a E-001: su solicitud paso de PENDIENTE a RECHAZADA");
    }

    @Test
    void contabilidad_soloActuaConAprobadas(CapturedOutput salida) {
        var contabilidad = new NotificadorContabilidad();
        contabilidad.onCambioEstado(conEstado(EstadoSolicitud.RECHAZADA), EstadoSolicitud.PENDIENTE);
        contabilidad.onCambioEstado(conEstado(EstadoSolicitud.REQUIERE_CORRECCION), EstadoSolicitud.PENDIENTE);
        assertThat(salida).doesNotContain("Contabilidad");

        contabilidad.onCambioEstado(conEstado(EstadoSolicitud.APROBADA), EstadoSolicitud.PENDIENTE);
        assertThat(salida).contains("Contabilidad: programar pago de 500000 para E-001");
    }

    @Test
    void auditoria_registraTodoCambio(CapturedOutput salida) {
        new NotificadorAuditoria().onCambioEstado(conEstado(EstadoSolicitud.REQUIERE_CORRECCION), EstadoSolicitud.PENDIENTE);
        assertThat(salida).contains("Auditoria: solicitud 7 cambio de PENDIENTE a REQUIERE_CORRECCION");
    }
}
