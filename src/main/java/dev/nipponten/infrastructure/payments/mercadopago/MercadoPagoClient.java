package dev.nipponten.infrastructure.payments.mercadopago;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** API Orders do Mercado Pago (/v1/orders). A URL base vem de quarkus.rest-client.mercadopago. */
@RegisterRestClient(configKey = "mercadopago")
@Path("/v1/orders")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface MercadoPagoClient {

    @POST
    MercadoPagoOrder create(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("X-Idempotency-Key") String idempotencyKey,
            MercadoPagoOrder.Create body);

    @GET
    @Path("/{id}")
    MercadoPagoOrder get(
            @HeaderParam("Authorization") String authorization, @PathParam("id") String id);
}
