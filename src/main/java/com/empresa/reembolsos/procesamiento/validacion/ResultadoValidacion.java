package com.empresa.reembolsos.procesamiento.validacion;

/**
 * Resultado de pasar una solicitud por la cadena de validacion.
 *
 * @param valida        {@code true} si la solicitud supero la verificacion
 * @param motivoRechazo motivo legible para el empleado cuando {@code valida} es {@code false}
 */
public record ResultadoValidacion(boolean valida, String motivoRechazo) {

    public static ResultadoValidacion ok() {
        return new ResultadoValidacion(true, null);
    }

    public static ResultadoValidacion rechazar(String motivo) {
        return new ResultadoValidacion(false, motivo);
    }
}
