package io.github.shaojunying.onlineexecutor.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.shaojunying.onlineexecutor.service.ExecutorBusyException;
import io.github.shaojunying.onlineexecutor.service.InvalidSourceException;

/**
 * Turns service exceptions into RFC 9457 problem responses.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(InvalidSourceException.class)
    ProblemDetail invalidSource(InvalidSourceException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid request");
        return problem;
    }

    @ExceptionHandler(ExecutorBusyException.class)
    ProblemDetail busy(ExecutorBusyException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        problem.setTitle("Server busy");
        return problem;
    }
}
