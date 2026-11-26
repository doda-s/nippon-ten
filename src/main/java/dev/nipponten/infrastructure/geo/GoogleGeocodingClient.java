package dev.nipponten.infrastructure.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Google Geocoding API. A URL base vem de quarkus.rest-client.google-geocoding. */
@RegisterRestClient(configKey = "google-geocoding")
@Path("/maps/api/geocode/json")
@Produces(MediaType.APPLICATION_JSON)
public interface GoogleGeocodingClient {

    @GET
    Response geocode(
            @QueryParam("address") String address,
            @QueryParam("components") String components,
            @QueryParam("language") String language,
            @QueryParam("region") String region,
            @QueryParam("key") String key);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(
            String status,
            @JsonProperty("error_message") String errorMessage,
            List<Result> results) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(
            @JsonProperty("address_components") List<Component> addressComponents,
            Geometry geometry,
            @JsonProperty("partial_match") boolean partialMatch) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Component(
            @JsonProperty("long_name") String longName,
            @JsonProperty("short_name") String shortName,
            List<String> types) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Geometry(Location location, @JsonProperty("location_type") String locationType) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Location(double lat, double lng) {}
}
