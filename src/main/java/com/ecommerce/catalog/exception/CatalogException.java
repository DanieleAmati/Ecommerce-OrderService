package com.ecommerce.catalog.exception;

import org.springframework.http.HttpStatus;


public class CatalogException extends RuntimeException {

    private final HttpStatus status;

    public CatalogException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // --- Eccezioni specifiche ---

    /** Email già registrata nel sistema. → 409 Conflict */
    public static class EmailAlreadyExistsException extends CatalogException {
        public EmailAlreadyExistsException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    /** Credenziali errate (email o password). → 401 Unauthorized
     *  NOTA: messaggio volutamente generico per non rivelare se l'email esiste. */
    public static class InvalidCredentialsException extends CatalogException {
        public InvalidCredentialsException() {
            super("Credenziali non valide", HttpStatus.UNAUTHORIZED);
        }
    }

    /** Account disabilitato dall'admin. → 401 Unauthorized */
    public static class AccountDisabledException extends CatalogException {
        public AccountDisabledException() {
            super("Account disabilitato", HttpStatus.UNAUTHORIZED);
        }
    }


    /** Token JWT non valido o scaduto. → 401 */
    public static class InvalidTokenException extends CatalogException {
        public InvalidTokenException(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }

    /** Risorsa non trovata. → 404 */
    public static class NotFoundException extends CatalogException {
        public NotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    public static class DatabaseException extends CatalogException {
        public DatabaseException(String message) {super(message, HttpStatus.INTERNAL_SERVER_ERROR);}
    }
}
