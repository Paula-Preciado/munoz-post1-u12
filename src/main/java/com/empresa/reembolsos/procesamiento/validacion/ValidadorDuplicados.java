package com.empresa.reembolsos.procesamiento.validacion;

import com.empresa.reembolsos.dominio.SolicitudReembolso;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

/**
 * Rechaza la solicitud si el empleado ya tiene otra PENDIENTE de la misma categoria
 * en el mismo periodo (mes y anio del gasto).
 *
 * <p>No consulta la base de datos: recibe las pendientes ya cargadas por el servicio de
 * aplicacion, asi el paquete de procesamiento queda libre de infraestructura.</p>
 */
@Component
public class ValidadorDuplicados extends ValidadorSolicitud {

    @Override
    protected ResultadoValidacion validarPropio(SolicitudReembolso solicitud, List<SolicitudReembolso> pendientes) {
        YearMonth periodo = YearMonth.from(solicitud.getFechaGasto());
        boolean existeDuplicado = pendientes.stream()
                .anyMatch(p -> p.getCategoria() == solicitud.getCategoria()
                        && YearMonth.from(p.getFechaGasto()).equals(periodo));
        return existeDuplicado
                ? ResultadoValidacion.rechazar("Ya existe una solicitud pendiente de la misma categoria en el periodo")
                : ResultadoValidacion.ok();
    }
}
