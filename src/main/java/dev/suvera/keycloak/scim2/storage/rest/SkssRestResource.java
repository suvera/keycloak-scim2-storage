package dev.suvera.keycloak.scim2.storage.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Path;

/**
 * author: suvera
 * date: 10/14/2020 12:42 PM
 */
public class SkssRestResource {
    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;

    public SkssRestResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager().authenticateBearerToken(session);
    }

    @Path("serviceProviders")
    public SkssSpResource getSkssSpResource() {
        return new SkssSpResource(session);
    }

    // Same like "companies" endpoint, but REST endpoint is authenticated with Bearer
    // token and user must be in realm role "admin"
    @Path("sp-auth")
    public SkssSpResource getSkssSpResourceAuthenticated() {
        checkRealmAdmin();
        return new SkssSpResource(session);
    }

    private void checkRealmAdmin() {
        if (auth == null) {
            throw new NotAuthorizedException("Bearer");
        } else if (auth.getToken().getRealmAccess() == null
                || !auth.getToken().getRealmAccess().isUserInRole("admin")) {
            String error = "Does not have realm admin role.";
            error += ", Token: " + auth.getToken();
            error += ", Realm: " + auth.getSession().getRealm();
            throw new ForbiddenException(error);
        }
    }
}
