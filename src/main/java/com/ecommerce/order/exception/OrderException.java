package com.ecommerce.order.exception;

import org.springframework.http.HttpStatus;


public class OrderException extends RuntimeException {

    private final HttpStatus status;

    public OrderException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // --- Eccezioni specifiche ---

    /** Email già registrata nel sistema. → 409 Conflict */
    public static class EmailAlreadyExistsException extends OrderException {
        public EmailAlreadyExistsException(String message) {
            super(message, HttpStatus.CONFLICT);
        }
    }

    /** Credenziali errate (email o password). → 401 Unauthorized
     *  NOTA: messaggio volutamente generico per non rivelare se l'email esiste. */
    public static class InvalidCredentialsException extends OrderException {
        public InvalidCredentialsException() {
            super("Credenziali non valide", HttpStatus.UNAUTHORIZED);
        }
    }

    /** Account disabilitato dall'admin. → 401 Unauthorized */
    public static class AccountDisabledException extends OrderException {
        public AccountDisabledException() {
            super("Account disabilitato", HttpStatus.UNAUTHORIZED);
        }
    }


    /** Token JWT non valido o scaduto. → 401 */
    public static class InvalidTokenException extends OrderException {
        public InvalidTokenException(String message) {
            super(message, HttpStatus.UNAUTHORIZED);
        }
    }

    /** Risorsa non trovata. → 404 */
    public static class NotFoundException extends OrderException {
        public NotFoundException(String message) {
            super(message, HttpStatus.NOT_FOUND);
        }
    }

    public static class DatabaseException extends OrderException {
        public DatabaseException(String message) {super(message, HttpStatus.INTERNAL_SERVER_ERROR);}
    }

    /** Operazione non consentita per l'utente autenticato. → 403 Forbidden */
    public static class ForbiddenException extends OrderException {
        public ForbiddenException(String message) {
            super(message, HttpStatus.FORBIDDEN);
        }
    }
}
