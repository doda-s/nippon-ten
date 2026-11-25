package dev.nipponten.infrastructure.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** ViaCEP (viacep.com.br). A URL base vem de quarkus.rest-client.viacep. */
@RegisterRestClient(configKey = "viacep")
@Path("/ws")
@Produces(MediaType.APPLICATION_JSON)
public interface ViaCepClient {

    @GET
    @Path("/{cep}/json/")
    Response get(@PathParam("cep") String cep);

    // CEP inexistente responde 200 com {"erro": "true"} (ou true, nas versões antigas).
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(
            String cep,
            String logradouro,
            String bairro,
            String localidade,
            String uf,
            String erro) {

        boolean notFound() {
            return erro != null && !erro.equalsIgnoreCase("false");
        }
    }
}
