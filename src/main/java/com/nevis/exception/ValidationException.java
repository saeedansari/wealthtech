package com.nevis.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import java.net.URI;
import java.util.List;

/**
 * Base class for validation-type errors (type: /errors/types/validation).
 * Extend this for any new error that falls under client-side validation issues:
 * bad input, missing parameters, constraint violations, resource not found, etc.
 */
public class ValidationException extends ApiException {

    private static final URI TYPE = URI.create("/errors/types/validation");

    private final List<String> errors;

    public ValidationException(HttpStatus status, String title, String detail) {
        super(status, title, detail);
        this.errors = List.of();
    }

    public ValidationException(HttpStatus status, String title, String detail, List<String> errors) {
        super(status, title, detail);
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    @Override
    public URI getType() {
        return TYPE;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public ProblemDetail toProblemDetail() {
        ProblemDetail problem = super.toProblemDetail();
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}
