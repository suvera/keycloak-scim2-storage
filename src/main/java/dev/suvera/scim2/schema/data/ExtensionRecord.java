package dev.suvera.scim2.schema.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * author: suvera
 * date: 10/17/2020 11:55 AM
 */
@SuppressWarnings("unused")
@Data
public class ExtensionRecord {
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    private final Map<String, Object> record = new HashMap<>();

    @JsonAnySetter
    public void setData(String key, Object value) {
        record.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getRecord() {
        return record;
    }

    @JsonIgnore
    public Object get(String key) {
        return record.get(key);
    }

    @JsonIgnore
    public void set(String key, Object val) {
        record.put(key, val);
    }

    @JsonIgnore
    public JsonNode asJsonNode() {
        return mapper.valueToTree(record);
    }
}
