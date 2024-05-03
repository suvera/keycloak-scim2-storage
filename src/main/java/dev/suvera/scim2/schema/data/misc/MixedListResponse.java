package dev.suvera.scim2.schema.data.misc;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.suvera.scim2.schema.data.ExtendedRecord;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Set;

/**
 * author: suvera
 * date: 10/17/2020 12:39 PM
 */
@Data
public class MixedListResponse {

    /**
     * This must be ScimConstant.URN_LIST_RESPONSE
     */
    private Set<String> schemas;

    @NotNull(message = "totalResults cannot be null")
    @PositiveOrZero(message = "totalResults must be 0 or greater")
    private Integer totalResults;
    private Integer itemsPerPage;
    private Integer startIndex;

    @JsonProperty("Resources")
    private List<ExtendedRecord> resources;
}
