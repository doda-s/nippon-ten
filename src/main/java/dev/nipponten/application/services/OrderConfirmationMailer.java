package dev.nipponten.application.services;

import dev.nipponten.domain.gateways.MailSender;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * E-mail com o código do pedido e o link de acompanhamento (docs/orders.md). O link abre a página
 * do pedido no front, que mostra o status em tempo real pelo SSE.
 */
@ApplicationScoped
public class OrderConfirmationMailer {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    @Inject MailSender sender;

    @ConfigProperty(name = "nippon.front.base-url")
    String frontBaseUrl;

    // Só depois do commit: ninguém recebe o link de um pedido que voltou atrás.
    void send(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderConfirmation confirmation) {
        String link = trackingLink(confirmation);
        String subject =
                confirmation.resent()
                        ? "Link para acompanhar o pedido " + confirmation.code()
                        : "Pedido " + confirmation.code() + " recebido";
        String next =
                confirmation.awaitingPayment()
                        ? "Assim que o pagamento for confirmado, o pedido vai para a cozinha."
                        : "Você pode acompanhar o preparo pelo link abaixo.";
        String total = NumberFormat.getCurrencyInstance(PT_BR).format(total(confirmation));
        String intro =
                confirmation.resent()
                        ? "Aqui está o novo link para acompanhar o seu pedido. O link anterior deixou"
                                + " de funcionar."
                        : "Recebemos o seu pedido.";

        String text =
                "Olá, %s!\n\n%s\n\nPedido: %s\nTotal: %s\n\n%s\n%s\n"
                        .formatted(
                                confirmation.name(), intro, confirmation.code(), total, next, link);
        String html =
                ("<p>Olá, %s!</p><p>%s</p><p>Pedido: <strong>%s</strong><br>Total: %s</p>"
                                + "<p>%s</p><p><a href=\"%s\">Acompanhar o pedido</a></p>")
                        .formatted(
                                escape(confirmation.name()),
                                intro,
                                confirmation.code(),
                                total,
                                next,
                                escape(link));
        sender.send(new MailSender.Mail(confirmation.email(), subject, text, html));
    }

    // O token vai no fragmento (#), que o navegador não envia ao servidor: não aparece em log de
    // acesso nem no Referer. O cliente com conta recebe o link sem token e entra logado.
    private String trackingLink(OrderConfirmation confirmation) {
        String base = frontBaseUrl.endsWith("/") ? frontBaseUrl : frontBaseUrl + "/";
        String link = base + "pedido/" + confirmation.code();
        return confirmation.trackingToken() == null
                ? link
                : link + "#t=" + confirmation.trackingToken();
    }

    private static BigDecimal total(OrderConfirmation confirmation) {
        return confirmation.total() == null ? BigDecimal.ZERO : confirmation.total();
    }

    // O nome vem de quem fez o pedido e entra no html.
    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
