package dev.nipponten.infrastructure.mail;

import dev.nipponten.domain.gateways.MailSender;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Envio por SMTP com o quarkus-mailer (configuração em quarkus.mailer.*). Em dev e teste o mailer
 * roda em modo mock por padrão e só registra a mensagem, sem servidor de e-mail.
 */
@ApplicationScoped
public class QuarkusMailSender implements MailSender {

    private static final Logger LOG = Logger.getLogger(QuarkusMailSender.class);

    @Inject ReactiveMailer mailer;

    // Reativo para a request não esperar o SMTP, que pode levar segundos.
    @Override
    public void send(MailSender.Mail mail) {
        mailer.send(
                        // O Mail do quarkus, não o da porta, que tem o mesmo nome.
                        io.quarkus.mailer.Mail.withHtml(mail.to(), mail.subject(), mail.html())
                                .setText(mail.text()))
                .subscribe()
                .with(
                        ignored -> {},
                        failure ->
                                LOG.warnf(failure, "Could not send e-mail \"%s\"", mail.subject()));
    }
}
