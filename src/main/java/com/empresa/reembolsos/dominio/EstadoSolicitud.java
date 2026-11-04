package com.empresa.reembolsos.dominio;

/** Estados por los que pasa una solicitud. Cada cambio se notifica a los interesados (RF3). */
public enum EstadoSolicitud { PENDIENTE, APROBADA, RECHAZADA, REQUIERE_CORRECCION }
