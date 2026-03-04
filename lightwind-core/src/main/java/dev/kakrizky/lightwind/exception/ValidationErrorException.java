package dev.kakrizky.lightwind.exception;

import java.util.List;

public class ValidationErrorException extends RuntimeException {

    private final List<ValidationError> errors;

    public ValidationErrorException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
