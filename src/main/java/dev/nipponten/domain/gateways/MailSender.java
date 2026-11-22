package dev.nipponten.domain.gateways;

/**
 * Envio de e-mail. Não bloqueia quem chama e não lança erro de entrega: uma falha do servidor de
 * e-mail é registrada no log e não desfaz a operação que pediu o envio.
 */
public interface MailSender {

    void send(Mail mail);

    /** O html é a versão principal; o texto é o fallback para leitores sem html. */
    record Mail(String to, String subject, String text, String html) {}
}
