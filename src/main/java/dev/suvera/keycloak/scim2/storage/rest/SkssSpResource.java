package dev.suvera.keycloak.scim2.storage.rest;

import dev.suvera.keycloak.scim2.storage.SkssSpRecord;
import dev.suvera.keycloak.scim2.storage.ex.DuplicateSpException;
import dev.suvera.keycloak.scim2.storage.spi.SkssService;
import dev.suvera.keycloak.scim2.storage.spi.impl.SkssServiceImpl;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * author: suvera
 * date: 10/14/2020 12:40 PM
 */
public class SkssSpResource {
    private final KeycloakSession session;
    private final SkssService skssService;

    public SkssSpResource(KeycloakSession session) {
        this.session = session;
        skssService = new SkssServiceImpl(session);
    }

    @GET
    @Path("")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<SkssSpRecord> getServiceProviders() {
        return skssService.listSp();
    }

    @POST
    @Path("")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createServiceProvider(SkssSpRecord rep) {
        try {
            skssService.addSp(rep);
        } catch (DuplicateSpException e) {
            return Response
                    .status(Response.Status.CONFLICT.getStatusCode(), e.getMessage())
                    .build();
        }
        return Response
                .created(session.getContext().getUri().getAbsolutePathBuilder().path(rep.getId()).build())
                .build();
    }

    @GET
    @NoCache
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public SkssSpRecord getServiceProvider(@PathParam("id") final String id) {
        return skssService.findSp(id);
    }

    @DELETE
    @NoCache
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteServiceProvider(@PathParam("id") final String id) {
        skssService.delete(id);
        
        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }
}
