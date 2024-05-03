package dev.suvera.keycloak.scim2.storage.ldap;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.naming.AuthenticationException;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.user.SynchronizationResult;

import dev.suvera.keycloak.scim2.storage.storage.JobEnqueuer;
import dev.suvera.keycloak.scim2.storage.storage.JobEnqueuerFactory;

public class LDAPEventMapper implements LDAPStorageMapper {
    private static final Logger log = Logger.getLogger(LDAPEventMapper.class);
    private KeycloakSession session;

    public LDAPEventMapper(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() { }

    @Override
    public void beforeLDAPQuery(LDAPQuery query) { }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        log.infof("We are in getGroupMembers. For what it counts :).");
        return Collections.emptyList();
    }

    @Override
    public LDAPStorageProvider getLdapProvider() {
        return null;
    }

    @Override
    public List<UserModel> getRoleMembers(RealmModel realm, RoleModel role, int firstResult, int maxResults) {
        return Collections.emptyList();
    }

    @Override
    public boolean onAuthenticationFailure(LDAPObject ldapUser, UserModel user, AuthenticationException ldapException, RealmModel realm) {
        return false;
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {
        log.infof("Import user from LDAP event user is %s, is created %s", user.getUsername(), isCreate);

        JobEnqueuer job = JobEnqueuerFactory.create(session);
        job.enqueueExternalUserCreateJob(realm, user.getId());
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel localUser, RealmModel realm) {
        log.infof("Register user to LDAP event user is %s", localUser.getUsername());

        JobEnqueuer job = JobEnqueuerFactory.create(session);
        job.enqueueExternalUserCreateJob(realm, localUser);
    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
        return delegate;
    }

    @Override
    public SynchronizationResult syncDataFromFederationProviderToKeycloak(RealmModel realm) {
        return new SynchronizationResult();
    }

    @Override
    public SynchronizationResult syncDataFromKeycloakToFederationProvider(RealmModel realm) {
        return new SynchronizationResult();
    }

    @Override
    public Set<String> mandatoryAttributeNames() {
        throw new UnsupportedOperationException("Unimplemented method 'mandatoryAttributeNames'");
    }

    @Override
    public Set<String> getUserAttributes() {
        throw new UnsupportedOperationException("Unimplemented method 'getUserAttributes'");
    }
    
}
