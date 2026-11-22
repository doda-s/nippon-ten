package dev.nipponten.resource;

import dev.nipponten.application.services.PaymentService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Chamado pelo Mercado Pago. Pública: a origem é provada pela assinatura, conferida no service.
@Path("/payments/webhooks/mercadopago")
public class PaymentWebhookResource {

    @Inject PaymentService service;

    // O corpo é ignorado: o estado é buscado no provedor.
    @POST
    @PermitAll
    @Consumes(MediaType.WILDCARD)
    public Response notify(
            @QueryParam("type") String type,
            @QueryParam("data.id") String resourceId,
            @HeaderParam("x-request-id") String requestId,
            @HeaderParam("x-signature") String signature) {
        service.handleNotification(type, resourceId, requestId, signature);
        return Response.ok().build();
    }
}
