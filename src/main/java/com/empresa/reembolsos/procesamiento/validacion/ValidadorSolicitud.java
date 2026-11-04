package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;

import java.util.List;

/**
 * Eslabon de la cadena de validacion de solicitudes (patron Chain of Responsibility, RF1).
 *
 * <p>Cada eslabon ejecuta su propia verificacion en {@link #validarPropio}; si la solicitud
 * la supera, delega en el siguiente eslabon. Ningun eslabon conoce cuantos hay despues de el,
 * por lo que agregar una verificacion nueva no exige modificar las existentes.</p>
 */
public abstract class ValidadorSolicitud {

    private ValidadorSolicitud siguiente;

    /**
     * Enlaza el siguiente eslabon de la cadena.
     *
     * @return el eslabon recibido, para encadenar llamadas: {@code a.siguiente(b).siguiente(c)}
     */
    public ValidadorSolicitud siguiente(ValidadorSolicitud siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    /**
     * Ejecuta este eslabon y, si la solicitud es valida, los siguientes.
     *
     * @param solicitud             solicitud a validar
     * @param pendientesDelEmpleado solicitudes PENDIENTES del mismo empleado, ya cargadas por el servicio
     * @return el primer rechazo encontrado, o {@code ok()} si todos los eslabones aprueban
     */
    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado) {
        ResultadoValidacion propio = validarPropio(solicitud, pendientesDelEmpleado);
        if (!propio.valida() || siguiente == null) {
            return propio;
        }
        return siguiente.validar(solicitud, pendientesDelEmpleado);
    }

    /** Verificacion propia de este eslabon, sin conocer al resto de la cadena. */
    protected abstract ResultadoValidacion validarPropio(
            SolicitudReembolso solicitud, List<SolicitudReembolso> pendientesDelEmpleado);
}
