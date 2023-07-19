package dev.suvera.scim2.schema.data.group;

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
public class GroupRecord extends BaseRecord {
    private String displayName;
    private List<GroupMember> members;

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
    public static class GroupMember {
        private String type;
        private String display;
        private String value;
        @JsonProperty("$ref")
        private String ref;
    }
}
