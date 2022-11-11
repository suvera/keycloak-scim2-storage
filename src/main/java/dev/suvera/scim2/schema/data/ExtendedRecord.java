package dev.suvera.scim2.schema.data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * author: suvera
 * date: 10/17/2020 1:02 PM
 */
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("unused")
@Data
public class ExtendedRecord extends BaseRecord {
    @JsonIgnore
    private final Map<String, Object> record = new HashMap<>();

    @JsonAnySetter
    protected void setData(String key, Object value) {
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
}
