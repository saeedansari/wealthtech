package com.nevis.exception;

import org.springframework.http.HttpStatus;
import java.net.URI;

/**
 * Base class for server-type errors (type: /errors/types/server).
 * Extend this for any new error that represents an internal server failure:
 * unexpected exceptions, integration failures, infrastructure issues, etc.
 */
public class ServerException extends ApiException {

    private static final URI TYPE = URI.create("/errors/types/server");

    public ServerException(String detail) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", detail);
    }

    public ServerException(String detail, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", detail, cause);
    }

    public ServerException(HttpStatus status, String title, String detail) {
        super(status, title, detail);
    }

    public ServerException(HttpStatus status, String title, String detail, Throwable cause) {
        super(status, title, detail, cause);
    }

    @Override
    public URI getType() {
        return TYPE;
    }
}
