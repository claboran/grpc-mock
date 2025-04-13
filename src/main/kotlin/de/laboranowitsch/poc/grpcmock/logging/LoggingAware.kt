package de.laboranowitsch.poc.grpcmock.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Marker interface for classes that need logging capabilities.
 * Classes implementing this interface can use the logger() extension function.
 */
interface LoggingAware

/**
 * Extension function that provides a logger for any class implementing LoggingAware.
 * Uses reified type parameter to get the correct logger for the implementing class.
 *
 * @return SLF4J Logger instance for the class
 */
inline fun <reified T : LoggingAware> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

/**
 * Extension functions for logging with different levels that accept lambda functions
 * to avoid unnecessary string concatenation when logging is disabled.
 */
inline fun Logger.debug(crossinline messageSupplier: () -> String) {
    if (isDebugEnabled) {
        debug(messageSupplier())
    }
}

inline fun Logger.info(crossinline messageSupplier: () -> String) {
    if (isInfoEnabled) {
        info(messageSupplier())
    }
}

inline fun Logger.warn(crossinline messageSupplier: () -> String) {
    if (isWarnEnabled) {
        warn(messageSupplier())
    }
}

inline fun Logger.error(crossinline messageSupplier: () -> String) {
    if (isErrorEnabled) {
        error(messageSupplier())
    }
}

inline fun Logger.error(throwable: Throwable, crossinline messageSupplier: () -> String) {
    if (isErrorEnabled) {
        error(messageSupplier(), throwable)
    }
}
