package dev.suvera.scim2.schema.data.user;

import com.fasterxml.jackson.annotation.*;
import dev.suvera.scim2.schema.data.BaseRecord;
import dev.suvera.scim2.schema.data.ExtensionRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author: suvera
 * date: 10/17/2020 9:56 AM
 */
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UserRecord extends BaseRecord {
    private String userName;
    private UserName name;
    private String displayName;
    private String nickName;
    private String profileUrl;
    private String title;
    private String userType;
    private String preferredLanguage;
    private String locale;
    private String timezone;
    private boolean active;
    private String password;
    private List<UserEmail> emails;
    private List<UserPhoneNumber> phoneNumbers;
    private List<UserIm> ims;
    private List<UserPhoto> photos;
    private List<UserAddress> addresses;
    private List<UserGroup> groups;
    private List<UserEntitlement> entitlements;
    private List<UserRole> roles;
    private List<UserX509Certificate> x509Certificates;
    private List<UserClaim> claims;

    @JsonIgnore
    private Map<String, ExtensionRecord> extensions = new HashMap<>();

    @JsonAnySetter
    protected void setExtensions(String key, ExtensionRecord value) {
        extensions.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, ExtensionRecord> getExtensions() {
        return extensions;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserName {
        private String formatted;
        private String familyName;
        private String givenName;
        private String middleName;
        private String honorificPrefix;
        private String honorificSuffix;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserClaim {
        private String attributeKey;
        private String attributeValue;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserEmail {
        private String value;
        private String display;
        private String type;
        private boolean primary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserPhoneNumber {
        private String value;
        private String display;
        private String type;
        private boolean primary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserIm {
        private String value;
        private String display;
        private String type;
        private boolean primary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserPhoto {
        private String value;
        private String type;
        private boolean primary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserAddress {
        private String formatted;
        private String streetAddress;
        private String locality;
        private String region;
        private String postalCode;
        private String country;
        private String type;
        private boolean primary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserGroup {
        private String value;
        @JsonProperty("$ref")
        private String ref;
        private String display;
        private String type;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserEntitlement {
        private String value;
        private String display;
        private String type;
        private boolean primary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserRole {
        private String value;
        private String display;
        private String type;
        private boolean primary;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class UserX509Certificate {
        private String value;
        private String display;
        private String type;
        private boolean primary;
    }
}
