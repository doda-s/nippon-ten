package dev.nipponten.infrastructure.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** BrasilAPI (brasilapi.com.br), reserva do ViaCEP. CEP inexistente responde 404. */
@RegisterRestClient(configKey = "brasilapi")
@Path("/api/cep/v1")
@Produces(MediaType.APPLICATION_JSON)
public interface BrasilApiCepClient {

    @GET
    @Path("/{cep}")
    Response get(@PathParam("cep") String cep);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(String cep, String state, String city, String neighborhood, String street) {}
}
