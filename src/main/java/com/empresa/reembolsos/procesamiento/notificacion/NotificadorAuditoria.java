package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Auditoria interna registra todo cambio de estado, sin excepcion, para el reporte trimestral. */
@Component
public class NotificadorAuditoria implements ObservadorCambioEstado {

    private static final Logger log = LoggerFactory.getLogger(NotificadorAuditoria.class);

    @Override
    public void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior) {
        log.info("Auditoria: solicitud {} cambio de {} a {}", solicitud.getId(), estadoAnterior, solicitud.getEstado());
    }
}
