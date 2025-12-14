package com.barterhouse.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilidad centralizada para logging del mod BarterHouse.
 * Proporciona métodos convenientes para registrar información de diferentes niveles.
 */
public class LoggerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("BarterHouse");

    /**
     * Registra un mensaje informativo.
     *
     * @param message Mensaje a registrar
     */
    public static void info(String message) {
        LOGGER.info(message);
    }

    /**
     * Registra un mensaje de depuración.
     *
     * @param message Mensaje a registrar
     */
    public static void debug(String message) {
        LOGGER.debug(message);
    }

    /**
     * Registra un mensaje de advertencia.
     *
     * @param message Mensaje a registrar
     */
    public static void warn(String message) {
        LOGGER.warn(message);
    }

    /**
     * Registra un mensaje de error.
     *
     * @param message Mensaje a registrar
     */
    public static void error(String message) {
        LOGGER.error(message);
    }

    /**
     * Registra un mensaje de error con excepción.
     *
     * @param message Mensaje a registrar
     * @param throwable Excepción a registrar
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    /**
     * Registra un error fatal.
     *
     * @param message Mensaje a registrar
     */
    public static void fatal(String message) {
        LOGGER.error("[FATAL] " + message);
    }
}
