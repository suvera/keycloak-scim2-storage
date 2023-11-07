package dev.suvera.keycloak.scim2.storage.storage;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import dev.suvera.scim2.schema.data.misc.PatchRequest;
import dev.suvera.scim2.schema.data.user.UserRecord;
import dev.suvera.scim2.schema.data.user.UserRecord.UserEmail;
import dev.suvera.scim2.schema.data.user.UserRecord.UserPhoneNumber;
import dev.suvera.scim2.schema.enums.PatchOp;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.ExtensionRecord;

public class UserRecordPatchBuilder {
    private UserRecordPatchBuilder() { /* static class */ }

    public static PatchRequest<UserRecord> buildPatchRequest(UserRecord modifiedRecord, UserRecord originalRecord) {
        PatchRequest<UserRecord> patchRequest = new PatchRequest<>(UserRecord.class);

        addOperation(patchRequest, originalRecord.getDisplayName(), modifiedRecord.getDisplayName(), "displayName");
        addOperation(patchRequest, originalRecord.getNickName(), modifiedRecord.getNickName(), "nickName");
        addOperation(patchRequest, originalRecord.getProfileUrl(), modifiedRecord.getProfileUrl(), "profileUrl");
        addOperation(patchRequest, originalRecord.getTitle(), modifiedRecord.getTitle(), "title");
        addOperation(patchRequest, originalRecord.getUserType(), modifiedRecord.getUserType(), "userType");
        addOperation(patchRequest, originalRecord.getPreferredLanguage(), modifiedRecord.getPreferredLanguage(),
                "preferredLanguage");
        addOperation(patchRequest, originalRecord.getLocale(), modifiedRecord.getLocale(), "locale");
        addOperation(patchRequest, originalRecord.getTimezone(), modifiedRecord.getTimezone(), "timezone");

        if (originalRecord.isActive() != modifiedRecord.isActive()) {
            patchRequest.addOperation(PatchOp.REPLACE, "active", modifiedRecord.isActive());
        }

        ExtensionRecord originalUserExtension = originalRecord.getExtensions().get(ScimConstant.URN_ADINSURE_USER);
        ExtensionRecord modifiedUserExtension = modifiedRecord.getExtensions().get(ScimConstant.URN_ADINSURE_USER);

        String originalPartyCode = extractStringNodeValueFromExtension(originalUserExtension, "partyCode");
        String modifiedPartyCode = extractStringNodeValueFromExtension(modifiedUserExtension, "partyCode");

        addOperation(patchRequest, originalPartyCode, modifiedPartyCode,
                ScimConstant.URN_ADINSURE_USER + ":partyCode");

        String extractedOriginalBlocked = extractStringNodeValueFromExtension(originalUserExtension, "blocked");
        String extractedModifiedBlocked = extractStringNodeValueFromExtension(modifiedUserExtension, "blocked");

        boolean originalBlocked = Boolean.parseBoolean(extractedOriginalBlocked);
        boolean modifiedBlocked;

        if (isNullOrEmpty(extractedModifiedBlocked)) {
            modifiedBlocked = originalBlocked;
        } else {
            modifiedBlocked = Boolean.parseBoolean(extractedModifiedBlocked);
        }

        addOperation(patchRequest, originalBlocked, modifiedBlocked,
                ScimConstant.URN_ADINSURE_USER + ":blocked");
        
        List<Map.Entry<String, String>> originalClaims = extractObjectAsKeyValueListFromExtension(originalUserExtension, "claims");
        List<Map.Entry<String, String>> modifiedClaims = extractObjectAsKeyValueListFromExtension(modifiedUserExtension, "claims");
        
        addListValueOperations(patchRequest, originalClaims, modifiedClaims,
                Entry::getKey, Entry::getValue,
                ScimConstant.URN_ADINSURE_USER + ":claims[type eq %s].value", false);
        
        addListValueOperations(patchRequest, originalRecord.getEmails(), modifiedRecord.getEmails(), UserEmail::getType,
                UserEmail::getValue, "emails[type eq %s].value");
        addListValueOperations(patchRequest, originalRecord.getPhoneNumbers(), modifiedRecord.getPhoneNumbers(),
                UserPhoneNumber::getType, UserPhoneNumber::getValue, "phoneNumbers[type eq %s].value");
        addListValueOperations(patchRequest, originalRecord.getAddresses(), modifiedRecord.getAddresses(), t -> t,
                v -> v, "addreses[type eq %s]");

        return patchRequest;
    }

    private static List<Map.Entry<String, String>> extractObjectAsKeyValueListFromExtension(ExtensionRecord userExtension, String fieldName) {
        List<Map.Entry<String, String>> keyValueList = new ArrayList<>();
        if (userExtension == null) {
            return keyValueList;
        }
        
        JsonNode userExtensionJsonNode = userExtension.asJsonNode();

        JsonNode objectJsonNode = userExtensionJsonNode.get(fieldName);
        if (objectJsonNode != null && objectJsonNode.isObject()) {
            objectJsonNode.fields().forEachRemaining(field -> {
                String value = extractStringFromValueNode(field.getValue());
                if (value != null) {
                    keyValueList.add(new AbstractMap.SimpleEntry<>(field.getKey(), value));
                }
            });
        }

        return keyValueList;
    }

    private static String extractStringNodeValueFromExtension(ExtensionRecord userExtension, String fieldName) {
        if (userExtension == null) {
            return null;
        }

        JsonNode userExtensionJsonNode = userExtension.asJsonNode();
        return extractStringFromValueNode(userExtensionJsonNode.get(fieldName));
    }

    private static String extractStringFromValueNode(JsonNode jsonNode) {
        if (jsonNode != null && jsonNode.isValueNode()) {
            return jsonNode.asText();
        }

        return null;
    }

    private static <T> void addListValueOperations(PatchRequest<UserRecord> patch, List<T> values1, List<T> values2,
            Function<T, Object> typeProp, Function<T, Object> valueProp, String path) {
        addListValueOperations(patch, values1, values2, typeProp, valueProp, path, true);
    }

    private static <T> void addListValueOperations(PatchRequest<UserRecord> patch, List<T> values1, List<T> values2,
            Function<T, Object> typeProp, Function<T, Object> valueProp, String path, boolean treatEmptyAsNull) {
        if (values2 != null) {
            for (T val2 : values2) {
                T existingEntry = null;
                if (values1 != null) {
                    existingEntry = values1
                            .stream()
                            .filter(v -> typeProp.apply(v).equals(typeProp.apply(val2)))
                            .findFirst().orElse(null);

                    if (existingEntry != null) {
                        values1.remove(existingEntry);
                    }
                }

                addOperation(patch, existingEntry == null ? null : valueProp.apply(existingEntry),
                        valueProp.apply(val2), String.format(path, typeProp.apply(val2)), treatEmptyAsNull);
            }
        }

        if (values1 != null) {
            values1.forEach(v -> patch.addOperation(PatchOp.REMOVE, String.format(path, typeProp.apply(v)),
                    valueProp.apply(v)));
        }
    }

    private static void addOperation(PatchRequest<UserRecord> patch, Object val1, Object val2, String path) {
        addOperation(patch, val1, val2, path, true);
    }

    private static void addOperation(PatchRequest<UserRecord> patch, Object val1, Object val2, String path,
            boolean treatEmptyAsNull) {
        if (isEmptyValue(val1, treatEmptyAsNull)) {
            if (!isEmptyValue(val2, treatEmptyAsNull)) {
                patch.addOperation(PatchOp.ADD, path, val2);
            }
        } else {
            if (isEmptyValue(val2, treatEmptyAsNull)) {
                patch.addOperation(PatchOp.REMOVE, path, val2);
            } else if (!val1.equals(val2)) {
                patch.addOperation(PatchOp.REPLACE, path, val2);
            }
        }
    }

    private static boolean isEmptyValue(Object val, boolean treatEmptyAsNull) {
        return treatEmptyAsNull
                ? isNullOrEmpty(val)
                : (val == null);
    }

    private static boolean isNullOrEmpty(Object val) {
        return val == null || (val instanceof String && ((String) val).trim().isEmpty());
    }
}
