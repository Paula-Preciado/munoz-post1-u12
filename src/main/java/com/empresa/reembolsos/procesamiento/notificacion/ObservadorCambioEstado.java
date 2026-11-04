package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

/**
 * Interesado en los cambios de estado de una solicitud (patron Observer, RF3).
 *
 * <p>El servicio de aplicacion notifica a todos los observadores registrados como
 * {@code @Component}, sin conocer que hace cada uno. Cada observador decide por si mismo
 * si el cambio le interesa. Agregar un interesado nuevo (por ejemplo, el panel de metricas)
 * es crear una clase nueva que implemente esta interfaz.</p>
 */
public interface ObservadorCambioEstado {

    /**
     * Se invoca despues de que la solicitud cambio de estado y fue guardada.
     *
     * @param solicitud      solicitud ya guardada, con su nuevo estado
     * @param estadoAnterior estado que tenia antes del cambio
     */
    void onCambioEstado(SolicitudReembolso solicitud, EstadoSolicitud estadoAnterior);
}
