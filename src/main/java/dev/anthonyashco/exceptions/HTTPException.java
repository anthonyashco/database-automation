package dev.anthonyashco.exceptions;


/**
 * HTTPException should be thrown in cases where an HTTP request returns an unexpected value.
 */
public class HTTPException extends Exception {
    public HTTPException(String errorMessage) {
        super(errorMessage);
    }
}
