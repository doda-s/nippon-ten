package dev.nipponten.resource;

import dev.nipponten.application.exceptions.AdditionalIngredientNotFoundException;
import dev.nipponten.application.exceptions.ProductIngredientNotFoundException;
import dev.nipponten.application.exceptions.ProductSizeNotFoundException;
import dev.nipponten.application.requests.AdditionalIngredientRequest;
import dev.nipponten.application.requests.ProductIngredientRequest;
import dev.nipponten.application.requests.ProductRequest;
import dev.nipponten.application.requests.ProductSizeRequest;
import dev.nipponten.application.responses.AdditionalIngredientResponse;
import dev.nipponten.application.responses.ProductDetailResponse;
import dev.nipponten.application.responses.ProductIngredientResponse;
import dev.nipponten.application.responses.ProductResponse;
import dev.nipponten.application.responses.ProductSizeResponse;
import dev.nipponten.application.services.AdditionalIngredientService;
import dev.nipponten.application.services.ProductIngredientService;
import dev.nipponten.application.services.ProductService;
import dev.nipponten.application.services.ProductSizeService;
import dev.nipponten.domain.models.AdditionalIngredient;
import dev.nipponten.domain.models.Product;
import dev.nipponten.domain.models.ProductIngredient;
import dev.nipponten.domain.models.ProductSize;
import jakarta.inject.Inject;
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

    @POST
    public Response create(ProductRequest request) {
        Product saved =
                service.create(
                        new Product(
                                null,
                                request.name(),
                                request.imageUrl(),
                                request.description(),
                                request.status()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @GET
    @Path("/{id}")
    public ProductDetailResponse getById(@PathParam("id") Long id) {
        Product product = service.getById(id);
        return new ProductDetailResponse(
                product.id(),
                product.name(),
                product.imageUrl(),
                product.description(),
                product.status(),
                productIngredientService.getByProduct(id).stream().map(this::toResponse).toList(),
                productSizeService.getByProduct(id).stream().map(this::toResponse).toList(),
                additionalIngredientService.getByProduct(id).stream()
                        .map(this::toResponse)
                        .toList());
    }

    @GET
    public List<ProductResponse> getAll() {
        return service.getAll().stream().map(this::toResponse).toList();
    }

    @PUT
    @Path("/{id}")
    public ProductResponse update(@PathParam("id") Long id, ProductRequest request) {
        Product updated =
                service.update(
                        id,
                        new Product(
                                id,
                                request.name(),
                                request.imageUrl(),
                                request.description(),
                                request.status()));
        return toResponse(updated);
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
        service.getById(productId);
        return productIngredientService.getByProduct(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @POST
    @Path("/{productId}/ingredients")
    public Response addIngredient(
            @PathParam("productId") Long productId, ProductIngredientRequest request) {
        service.getById(productId);
        ProductIngredient saved =
                productIngredientService.create(
                        new ProductIngredient(null, productId, request.ingredientId()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @DELETE
    @Path("/{productId}/ingredients/{productIngredientId}")
    public Response removeIngredient(
            @PathParam("productId") Long productId,
            @PathParam("productIngredientId") Long productIngredientId) {
        requireIngredientOf(productId, productIngredientId);
        productIngredientService.delete(productIngredientId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{productId}/sizes")
    public List<ProductSizeResponse> getSizes(@PathParam("productId") Long productId) {
        service.getById(productId);
        return productSizeService.getByProduct(productId).stream().map(this::toResponse).toList();
    }

    @POST
    @Path("/{productId}/sizes")
    public Response addSize(@PathParam("productId") Long productId, ProductSizeRequest request) {
        service.getById(productId);
        ProductSize saved =
                productSizeService.create(
                        new ProductSize(null, productId, request.price(), request.status()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @PUT
    @Path("/{productId}/sizes/{sizeId}")
    public ProductSizeResponse updateSize(
            @PathParam("productId") Long productId,
            @PathParam("sizeId") Long sizeId,
            ProductSizeRequest request) {
        requireSizeOf(productId, sizeId);
        ProductSize updated =
                productSizeService.update(
                        sizeId,
                        new ProductSize(sizeId, productId, request.price(), request.status()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{productId}/sizes/{sizeId}")
    public Response removeSize(
            @PathParam("productId") Long productId, @PathParam("sizeId") Long sizeId) {
        requireSizeOf(productId, sizeId);
        productSizeService.delete(sizeId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{productId}/additional-ingredients")
    public List<AdditionalIngredientResponse> getAdditionalIngredients(
            @PathParam("productId") Long productId) {
        service.getById(productId);
        return additionalIngredientService.getByProduct(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @POST
    @Path("/{productId}/additional-ingredients")
    public Response addAdditionalIngredient(
            @PathParam("productId") Long productId, AdditionalIngredientRequest request) {
        service.getById(productId);
        AdditionalIngredient saved =
                additionalIngredientService.create(
                        new AdditionalIngredient(
                                null,
                                productId,
                                request.ingredientId(),
                                request.maximumQuantity(),
                                request.status()));
        return Response.status(Response.Status.CREATED).entity(toResponse(saved)).build();
    }

    @PUT
    @Path("/{productId}/additional-ingredients/{additionalIngredientId}")
    public AdditionalIngredientResponse updateAdditionalIngredient(
            @PathParam("productId") Long productId,
            @PathParam("additionalIngredientId") Long additionalIngredientId,
            AdditionalIngredientRequest request) {
        requireAdditionalIngredientOf(productId, additionalIngredientId);
        AdditionalIngredient updated =
                additionalIngredientService.update(
                        additionalIngredientId,
                        new AdditionalIngredient(
                                additionalIngredientId,
                                productId,
                                request.ingredientId(),
                                request.maximumQuantity(),
                                request.status()));
        return toResponse(updated);
    }

    @DELETE
    @Path("/{productId}/additional-ingredients/{additionalIngredientId}")
    public Response removeAdditionalIngredient(
            @PathParam("productId") Long productId,
            @PathParam("additionalIngredientId") Long additionalIngredientId) {
        requireAdditionalIngredientOf(productId, additionalIngredientId);
        additionalIngredientService.delete(additionalIngredientId);
        return Response.noContent().build();
    }

    private ProductIngredient requireIngredientOf(Long productId, Long productIngredientId) {
        service.getById(productId);
        ProductIngredient productIngredient = productIngredientService.getById(productIngredientId);
        if (!productId.equals(productIngredient.productId())) {
            throw new ProductIngredientNotFoundException(productIngredientId);
        }
        return productIngredient;
    }

    private ProductSize requireSizeOf(Long productId, Long sizeId) {
        service.getById(productId);
        ProductSize productSize = productSizeService.getById(sizeId);
        if (!productId.equals(productSize.productId())) {
            throw new ProductSizeNotFoundException(sizeId);
        }
        return productSize;
    }

    private AdditionalIngredient requireAdditionalIngredientOf(
            Long productId, Long additionalIngredientId) {
        service.getById(productId);
        AdditionalIngredient additionalIngredient =
                additionalIngredientService.getById(additionalIngredientId);
        if (!productId.equals(additionalIngredient.productId())) {
            throw new AdditionalIngredientNotFoundException(additionalIngredientId);
        }
        return additionalIngredient;
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.imageUrl(),
                product.description(),
                product.status());
    }

    private ProductIngredientResponse toResponse(ProductIngredient productIngredient) {
        return new ProductIngredientResponse(
                productIngredient.id(),
                productIngredient.productId(),
                productIngredient.ingredientId());
    }

    private ProductSizeResponse toResponse(ProductSize productSize) {
        return new ProductSizeResponse(
                productSize.id(),
                productSize.productId(),
                productSize.price(),
                productSize.status());
    }

    private AdditionalIngredientResponse toResponse(AdditionalIngredient additionalIngredient) {
        return new AdditionalIngredientResponse(
                additionalIngredient.id(),
                additionalIngredient.productId(),
                additionalIngredient.ingredientId(),
                additionalIngredient.maximumQuantity(),
                additionalIngredient.status());
    }
}
