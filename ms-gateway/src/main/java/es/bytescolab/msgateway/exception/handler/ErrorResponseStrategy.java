package es.bytescolab.msgateway.exception.handler;

@FunctionalInterface
public interface ErrorResponseStrategy {
    ErrorContext map(Throwable ex);
}
