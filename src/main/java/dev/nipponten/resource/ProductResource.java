package dev.nipponten.resource;

import dev.nipponten.application.requests.AdditionalIngredientRequest;
import dev.nipponten.application.requests.AdditionalIngredientRequestMapper;
import dev.nipponten.application.requests.ProductIngredientRequest;
import dev.nipponten.application.requests.ProductIngredientRequestMapper;
import dev.nipponten.application.requests.ProductRequest;
import dev.nipponten.application.requests.ProductRequestMapper;
import dev.nipponten.application.requests.ProductSizeRequest;
import dev.nipponten.application.requests.ProductSizeRequestMapper;
import dev.nipponten.application.responses.AdditionalIngredientResponse;
import dev.nipponten.application.responses.AdditionalIngredientResponseMapper;
import dev.nipponten.application.responses.ProductDetailResponse;
import dev.nipponten.application.responses.ProductIngredientResponse;
import dev.nipponten.application.responses.ProductIngredientResponseMapper;
import dev.nipponten.application.responses.ProductResponse;
import dev.nipponten.application.responses.ProductResponseMapper;
import dev.nipponten.application.responses.ProductSizeResponse;
import dev.nipponten.application.responses.ProductSizeResponseMapper;
import dev.nipponten.application.services.AdditionalIngredientService;
import dev.nipponten.application.services.ProductIngredientService;
import dev.nipponten.application.services.ProductService;
import dev.nipponten.application.services.ProductSizeService;
import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.domain.models.Product;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.domain.models.ProductSize;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/products")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject ProductService service;

    @Inject ProductIngredientService productIngredientService;

    @Inject ProductSizeService productSizeService;

    @Inject AdditionalIngredientService additionalIngredientService;

    @Inject ProductResponseMapper productMapper;

    @Inject ProductIngredientResponseMapper productIngredientMapper;

    @Inject ProductSizeResponseMapper productSizeMapper;

    @Inject AdditionalIngredientResponseMapper additionalIngredientMapper;

    @Inject ProductRequestMapper productRequestMapper;

    @Inject ProductIngredientRequestMapper productIngredientRequestMapper;

    @Inject ProductSizeRequestMapper productSizeRequestMapper;

    @Inject AdditionalIngredientRequestMapper additionalIngredientRequestMapper;

    @POST
    public Response create(@Valid ProductRequest request) {
        Product saved = service.create(productRequestMapper.toModel(null, request));
        return Response.status(Response.Status.CREATED)
                .entity(productMapper.toResponse(saved))
                .build();
    }

    @GET
    @Path("/{id}")
    public ProductDetailResponse getById(@PathParam("id") Long id) {
        return productMapper.toDetailResponse(
                service.getById(id),
                productIngredientService.getByProduct(id),
                productSizeService.getByProduct(id),
                additionalIngredientService.getByProduct(id));
    }

    @GET
    public List<ProductResponse> getAll() {
        return service.getAll().stream().map(productMapper::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public ProductResponse update(@PathParam("id") Long id, @Valid ProductRequest request) {
        Product updated = service.update(id, productRequestMapper.toModel(id, request));
        return productMapper.toResponse(updated);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{productId}/ingredients")
    public List<ProductIngredientResponse> getIngredients(@PathParam("productId") Long productId) {
        return productIngredientService.getByProduct(productId).stream()
                .map(productIngredientMapper::toResponse)
                .toList();
    }

    @POST
    @Path("/{productId}/ingredients")
    public Response addIngredient(
            @PathParam("productId") Long productId, @Valid ProductIngredientRequest request) {
        ProductIngredient saved =
                productIngredientService.create(
                        productIngredientRequestMapper.toModel(null, productId, request));
        return Response.status(Response.Status.CREATED)
                .entity(productIngredientMapper.toResponse(saved))
                .build();
    }

    @DELETE
    @Path("/{productId}/ingredients/{productIngredientId}")
    public Response removeIngredient(
            @PathParam("productId") Long productId,
            @PathParam("productIngredientId") Long productIngredientId) {
        productIngredientService.delete(productId, productIngredientId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{productId}/sizes")
    public List<ProductSizeResponse> getSizes(@PathParam("productId") Long productId) {
        return productSizeService.getByProduct(productId).stream()
                .map(productSizeMapper::toResponse)
                .toList();
    }

    @POST
    @Path("/{productId}/sizes")
    public Response addSize(
            @PathParam("productId") Long productId, @Valid ProductSizeRequest request) {
        ProductSize saved =
                productSizeService.create(
                        productSizeRequestMapper.toModel(null, productId, request));
        return Response.status(Response.Status.CREATED)
                .entity(productSizeMapper.toResponse(saved))
                .build();
    }

    @PUT
    @Path("/{productId}/sizes/{sizeId}")
    public ProductSizeResponse updateSize(
            @PathParam("productId") Long productId,
            @PathParam("sizeId") Long sizeId,
            @Valid ProductSizeRequest request) {
        ProductSize updated =
                productSizeService.update(
                        productId,
                        sizeId,
                        productSizeRequestMapper.toModel(sizeId, productId, request));
        return productSizeMapper.toResponse(updated);
    }

    @DELETE
    @Path("/{productId}/sizes/{sizeId}")
    public Response removeSize(
            @PathParam("productId") Long productId, @PathParam("sizeId") Long sizeId) {
        productSizeService.delete(productId, sizeId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{productId}/additional-ingredients")
    public List<AdditionalIngredientResponse> getAdditionalIngredients(
            @PathParam("productId") Long productId) {
        return additionalIngredientService.getByProduct(productId).stream()
                .map(additionalIngredientMapper::toResponse)
                .toList();
    }

    @POST
    @Path("/{productId}/additional-ingredients")
    public Response addAdditionalIngredient(
            @PathParam("productId") Long productId, @Valid AdditionalIngredientRequest request) {
        AdditionalIngredient saved =
                additionalIngredientService.create(
                        additionalIngredientRequestMapper.toModel(null, productId, request));
        return Response.status(Response.Status.CREATED)
                .entity(additionalIngredientMapper.toResponse(saved))
                .build();
    }

    @PUT
    @Path("/{productId}/additional-ingredients/{additionalIngredientId}")
    public AdditionalIngredientResponse updateAdditionalIngredient(
            @PathParam("productId") Long productId,
            @PathParam("additionalIngredientId") Long additionalIngredientId,
            @Valid AdditionalIngredientRequest request) {
        AdditionalIngredient updated =
                additionalIngredientService.update(
                        productId,
                        additionalIngredientId,
                        additionalIngredientRequestMapper.toModel(
                                additionalIngredientId, productId, request));
        return additionalIngredientMapper.toResponse(updated);
    }

    @DELETE
    @Path("/{productId}/additional-ingredients/{additionalIngredientId}")
    public Response removeAdditionalIngredient(
            @PathParam("productId") Long productId,
            @PathParam("additionalIngredientId") Long additionalIngredientId) {
        additionalIngredientService.delete(productId, additionalIngredientId);
        return Response.noContent().build();
    }
}
