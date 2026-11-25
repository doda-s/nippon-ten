package dev.nipponten.resource;

import dev.nipponten.application.services.JobTriggerService;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

// Chamado pelo Cloud Scheduler (docs/deploy.md, Jobs agendados). Pública: a origem é provada pelo
// token, conferido no service. Fora do OpenAPI (e do client npm) porque o front não chama.
@Path("/jobs")
public class JobResource {

    static final String TOKEN_HEADER = "X-Job-Token";

    @Inject JobTriggerService service;

    @POST
    @Path("/payment-reconciliation")
    @PermitAll
    @Operation(hidden = true)
    public Response reconcilePayments(@HeaderParam(TOKEN_HEADER) String token) {
        service.reconcilePayments(token);
        return Response.noContent().build();
    }
}
