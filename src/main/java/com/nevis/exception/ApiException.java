package com.nevis.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import java.net.URI;

/**
 * Base class for all API exceptions that produce RFC-9457 Problem Detail responses.
 * Subclasses define the problem type URI and default title. Concrete exceptions
 * set the HTTP status and detail message.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String title;

    protected ApiException(HttpStatus status, String title, String detail) {
        super(detail);
        this.status = status;
        this.title = title;
    }

    protected ApiException(HttpStatus status, String title, String detail, Throwable cause) {
        super(detail, cause);
        this.status = status;
        this.title = title;
    }

    /**
     * The RFC-9457 problem type URI (e.g. "/errors/types/validation").
     */
    public abstract URI getType();

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Builds the ProblemDetail. Subclasses can override to add extension properties.
     */
    public ProblemDetail toProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, getMessage());
        problem.setTitle(title);
        problem.setType(getType());
        return problem;
    }
}
