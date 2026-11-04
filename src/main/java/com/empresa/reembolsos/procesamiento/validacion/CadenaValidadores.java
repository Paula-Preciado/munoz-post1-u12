package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Arma la cadena de validacion en un unico lugar.
 *
 * <p>Orden: documentos -> monto -> duplicados. Los eslabones baratos (un booleano y una
 * comparacion contra un mapa) van primero; el que recorre la lista de pendientes va de
 * ultimo, porque solo se ejecuta si los anteriores aprobaron (ver Decision de diseno 1).</p>
 */
@Component
public class CadenaValidadores {

    private final ValidadorSolicitud primero;

    public CadenaValidadores(ValidadorDocumentosSoporte documentos,
                             ValidadorMontoMaximo monto,
                             ValidadorDuplicados duplicados) {
        documentos.siguiente(monto).siguiente(duplicados);
        this.primero = documentos;
    }

    public ResultadoValidacion validar(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        return primero.validar(solicitud, pendientes);
    }
}
