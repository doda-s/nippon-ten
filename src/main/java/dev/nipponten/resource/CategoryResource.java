package dev.nipponten.resource;

import dev.nipponten.application.requests.CategoryRequest;
import dev.nipponten.application.requests.CategoryRequestMapper;
import dev.nipponten.application.responses.CategoryResponse;
import dev.nipponten.application.responses.CategoryResponseMapper;
import dev.nipponten.application.responses.PageResponse;
import dev.nipponten.application.services.CategoryService;
import dev.nipponten.domain.models.Category;
import dev.nipponten.resource.security.Permissions;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Categorias do cardápio: leitura pública (a vitrine agrupa por elas), escrita com a mesma
// permissão dos produtos.
@Path("/categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

    @Inject CategoryService service;

    @Inject CategoryResponseMapper mapper;

    @Inject CategoryRequestMapper requestMapper;

    @POST
    @RolesAllowed(Permissions.MANAGE_PRODUCTS)
    public Response create(@Valid CategoryRequest request) {
        Category saved = service.create(requestMapper.toModel(null, request));
        return Response.status(Response.Status.CREATED).entity(mapper.toResponse(saved)).build();
    }

    @GET
    @PermitAll
    @Path("/{id}")
    public CategoryResponse getById(@PathParam("id") Long id) {
        return mapper.toResponse(service.getById(id));
    }

    @GET
    @PermitAll
    public PageResponse<CategoryResponse> getAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageParams params = PageParams.of(page, size);
        return PageResponse.of(
                service.getPage(params.page(), params.size()),
                params.page(),
                params.size(),
                mapper::toResponse);
    }

    @PUT
    @RolesAllowed(Permissions.MANAGE_PRODUCTS)
    @Path("/{id}")
    public CategoryResponse update(@PathParam("id") Long id, @Valid CategoryRequest request) {
        return mapper.toResponse(service.update(id, requestMapper.toModel(id, request)));
    }

    @DELETE
    @RolesAllowed(Permissions.MANAGE_PRODUCTS)
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
