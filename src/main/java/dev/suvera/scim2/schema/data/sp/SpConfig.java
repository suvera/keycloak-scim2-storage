package dev.suvera.scim2.schema.data.sp;

import dev.suvera.scim2.schema.data.BaseRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * author: suvera
 * date: 10/17/2020 12:42 AM
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpConfig extends BaseRecord {
    @NotBlank(message = "documentationUri cannot be empty")
    private String documentationUri;
    @NotNull(message = "patch cannot be null")
    private Supported patch;
    @NotNull(message = "bulk cannot be null")
    private Bulk bulk;
    @NotNull(message = "filter cannot be null")
    private Filter filter;
    @NotNull(message = "etag cannot be null")
    private Supported etag;
    @NotNull(message = "changePassword cannot be null")
    private Supported changePassword;
    @NotNull(message = "sort cannot be null")
    private Supported sort;
    private List<AuthenticationScheme> authenticationSchemes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Supported {
        @NotNull(message = "supported cannot be null")
        private Boolean supported;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bulk {
        @NotNull(message = "supported cannot be null")
        private Boolean supported;
        @NotNull(message = "maxOperations cannot be null")
        private Integer maxOperations;
        @NotNull(message = "maxPayloadSize cannot be null")
        private Integer maxPayloadSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Filter {
        @NotNull(message = "supported cannot be null")
        private Boolean supported;
        @NotNull(message = "maxResults cannot be null")
        private Integer maxResults;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticationScheme {
        @NotBlank(message = "type cannot be null")
        private String type;
        @NotBlank(message = "name cannot be null")
        private String name;
        private String description;
        private String specUri;
        private String documentationUri;
        private Boolean primary;
    }
}
