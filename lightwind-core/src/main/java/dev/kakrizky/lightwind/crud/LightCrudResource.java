package dev.kakrizky.lightwind.crud;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kakrizky.lightwind.auth.LightUser;
import dev.kakrizky.lightwind.entity.LightEntity;
import dev.kakrizky.lightwind.exception.BadRequestException;
import dev.kakrizky.lightwind.response.BulkDeleteResult;
import dev.kakrizky.lightwind.response.LightResponse;
import dev.kakrizky.lightwind.response.PagedResult;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.security.Principal;
import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class LightCrudResource<E extends LightEntity<E, D>, D> {

    @Inject
    ObjectMapper objectMapper;

    protected abstract LightCrudService<E, D> getService();

    protected abstract Class<D> getDtoClass();

    @GET
    public LightResponse<PagedResult<D>> getAll(
            @Context UriInfo uriInfo,
            @Context SecurityContext securityContext
    ) {
        var params = uriInfo.getQueryParameters();

        String search = params.getFirst("search");
        if (search == null || search.isBlank()) {
            search = params.getFirst("q");
        }

        int page = parseIntOrDefault(params.getFirst("page"), 1);
        int size = Math.min(parseIntOrDefault(params.getFirst("size"), 10), 100);
        String sort = params.getFirst("sort");
        String sortDir = "desc";

        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            sort = parts[0];
            sortDir = parts.length > 1 ? parts[1] : "desc";
        }

        Set<String> reserved = Set.of("search", "q", "page", "size", "sort", "is_paginate");
        Map<String, List<String>> filters = new HashMap<>();
        params.forEach((key, values) -> {
            if (!reserved.contains(key)) {
                List<String> expanded = new ArrayList<>();
                for (String v : values) {
                    for (String part : v.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) expanded.add(trimmed);
                    }
                }
                if (!expanded.isEmpty()) {
                    filters.put(key, expanded);
                }
            }
        });

        LightUser user = extractUser(securityContext);
        return LightResponse.ok(getService().getAll(filters, search, page, size, sort, sortDir, user));
    }

    @GET
    @Path("deleted")
    public LightResponse<PagedResult<D>> getDeleted(
            @Context UriInfo uriInfo,
            @Context SecurityContext securityContext
    ) {
        var params = uriInfo.getQueryParameters();

        String search = params.getFirst("search");
        if (search == null || search.isBlank()) {
            search = params.getFirst("q");
        }

        int page = parseIntOrDefault(params.getFirst("page"), 1);
        int size = Math.min(parseIntOrDefault(params.getFirst("size"), 10), 100);
        String sort = params.getFirst("sort");
        String sortDir = "desc";

        if (sort != null && sort.contains(",")) {
            String[] parts = sort.split(",");
            sort = parts[0];
            sortDir = parts.length > 1 ? parts[1] : "desc";
        }

        Set<String> reserved = Set.of("search", "q", "page", "size", "sort", "is_paginate");
        Map<String, List<String>> filters = new HashMap<>();
        params.forEach((key, values) -> {
            if (!reserved.contains(key)) {
                List<String> expanded = new ArrayList<>();
                for (String v : values) {
                    for (String part : v.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) expanded.add(trimmed);
                    }
                }
                if (!expanded.isEmpty()) {
                    filters.put(key, expanded);
                }
            }
        });

        return LightResponse.ok(getService().getAllIncludingDeleted(filters, search, page, size, sort, sortDir));
    }

    @GET
    @Path("{id}")
    public LightResponse<D> getOne(
            @PathParam("id") UUID id,
            @Context SecurityContext securityContext
    ) {
        return LightResponse.ok(getService().getOne(id, extractUser(securityContext)));
    }

    @POST
    public LightResponse<D> create(D dto, @Context SecurityContext securityContext) {
        return LightResponse.ok(getService().create(dto, extractUser(securityContext)));
    }

    @PUT
    @Path("{id}")
    public LightResponse<D> update(
            @PathParam("id") UUID id,
            D dto,
            @Context SecurityContext securityContext
    ) {
        return LightResponse.ok(getService().update(id, dto, extractUser(securityContext)));
    }

    @PATCH
    @Path("{id}")
    public LightResponse<D> patch(
            @PathParam("id") UUID id,
            D dto,
            @Context SecurityContext securityContext
    ) {
        return LightResponse.ok(getService().patch(id, dto, extractUser(securityContext)));
    }

    @POST
    @Path("bulk")
    public LightResponse<List<D>> bulkCreate(
            String body,
            @Context SecurityContext securityContext
    ) {
        List<D> dtos = deserializeList(body);
        return LightResponse.ok(getService().bulkCreate(dtos, extractUser(securityContext)));
    }

    @DELETE
    @Path("bulk")
    public LightResponse<BulkDeleteResult> bulkDelete(
            String body,
            @Context SecurityContext securityContext
    ) {
        List<UUID> ids = deserializeUuidList(body);
        int count = getService().bulkDelete(ids, extractUser(securityContext));
        return LightResponse.ok(new BulkDeleteResult(count));
    }

    @DELETE
    @Path("{id}")
    public LightResponse<D> remove(
            @PathParam("id") UUID id,
            @Context SecurityContext securityContext
    ) {
        return LightResponse.ok(getService().delete(id, extractUser(securityContext)));
    }

    @POST
    @Path("{id}/restore")
    public LightResponse<D> restore(
            @PathParam("id") UUID id,
            @Context SecurityContext securityContext
    ) {
        return LightResponse.ok(getService().restore(id, extractUser(securityContext)));
    }

    protected LightUser extractUser(SecurityContext ctx) {
        Principal principal = ctx.getUserPrincipal();
        if (principal == null) return null;

        if (principal instanceof JsonWebToken jwt) {
            String userId = jwt.getClaim("user_id");
            String userName = jwt.getClaim("user_name");
            if (userId != null) {
                return new LightUser(UUID.fromString(userId), userName);
            }
        }
        return null;
    }

    private List<D> deserializeList(String body) {
        try {
            return objectMapper.readValue(body,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, getDtoClass()));
        } catch (Exception e) {
            throw new BadRequestException("Invalid request body: " + e.getMessage());
        }
    }

    private List<UUID> deserializeUuidList(String body) {
        try {
            return objectMapper.readValue(body, new TypeReference<List<UUID>>() {});
        } catch (Exception e) {
            throw new BadRequestException("Invalid request body: expected JSON array of UUIDs");
        }
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
