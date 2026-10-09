package es.bytescolab.msmaintenance.exception;

public class PlanAreadyExists extends RuntimeException {
    public PlanAreadyExists(String message) {
        super(message);
    }
}
