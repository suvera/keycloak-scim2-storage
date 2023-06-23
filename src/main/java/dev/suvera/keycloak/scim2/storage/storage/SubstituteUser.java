package dev.suvera.keycloak.scim2.storage.storage;

public class SubstituteUser {
    private String userId;
    private String substituteUserId;

    public void setUser(String id) {
        userId = id;
    }

    public String getUser() {
        return userId;
    }

    public void setSubstituteUser(String id) {
        substituteUserId = id;
    }

    public String getSubstituteUser() {
        return substituteUserId;
    }
}
