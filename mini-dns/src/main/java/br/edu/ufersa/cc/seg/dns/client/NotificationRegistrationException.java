package br.edu.ufersa.cc.seg.dns.client;

/**
 * Exceção lançada quando o registro para receber notificações falha
 */
public class NotificationRegistrationException extends Exception {
    public NotificationRegistrationException(String message) {
        super(message);
    }

    public NotificationRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}