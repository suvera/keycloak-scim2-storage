package dev.suvera.keycloak.scim2.storage.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.suvera.scim2.schema.data.misc.PatchRequest;
import dev.suvera.scim2.schema.data.user.UserRecord;
import dev.suvera.scim2.schema.data.user.UserRecord.UserClaim;
import dev.suvera.scim2.schema.enums.PatchOp;
import dev.suvera.scim2.schema.ScimConstant;
import dev.suvera.scim2.schema.data.ExtensionRecord;

public class UserRecordPatchBuilder {
    private UserRecordPatchBuilder() {
        /* static class */ }

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

        UserClaim modifiedPartyCodeClaim = modifiedRecord.getClaims().stream()
                .filter(claim -> "PartyCode".equalsIgnoreCase(claim.getAttributeKey()))
                .findFirst()
                .orElse(null);
        String modifiedPartyCode = modifiedPartyCodeClaim != null
                ? modifiedPartyCodeClaim.getAttributeValue()
                : null;

        List<UserClaim> modifiedClaims = modifiedRecord.getClaims().stream()
                .filter(uc -> !uc.getAttributeKey().equalsIgnoreCase("PartyCode"))
                .collect(Collectors.toList());

        List<UserClaim> originalClaims = new ArrayList<UserClaim>();
        String originalPartyCode = null;
        ExtensionRecord originalUserExtension = originalRecord.getExtensions().get(ScimConstant.URN_ADINSURE_USER);
        if (originalUserExtension != null) {
            JsonNode originalUserExtensionJsonNode = originalUserExtension.asJsonNode();

            JsonNode originalPartyCodeJsonNode = originalUserExtensionJsonNode.get("partyCode");
            if (originalPartyCodeJsonNode != null && originalPartyCodeJsonNode.isValueNode()) {
                originalPartyCode = originalPartyCodeJsonNode.asText();
            }

            JsonNode originalClaimsJsonNode = originalUserExtensionJsonNode.get("claims");
            if (originalClaimsJsonNode != null && originalClaimsJsonNode.isObject()) {
                Iterator<Entry<String, JsonNode>> fields = originalClaimsJsonNode.fields();
                while (fields.hasNext()) {
                    Entry<String, JsonNode> field = fields.next();
                    UserClaim userClaim = new UserClaim();
                    userClaim.setAttributeKey(field.getKey());
                    userClaim.setAttributeValue(field.getValue().asText());
                    originalClaims.add(userClaim);
                }
            }
        }

        addOperation(patchRequest, originalPartyCode, modifiedPartyCode,
                ScimConstant.URN_ADINSURE_USER + ":partyCode");
        addListValueOperations(patchRequest, originalClaims, modifiedClaims,
                t -> t.getAttributeKey(), v -> v.getAttributeValue(),
                ScimConstant.URN_ADINSURE_USER + ":claims[type eq %s].value", false);

        addListValueOperations(patchRequest, originalRecord.getEmails(), modifiedRecord.getEmails(), t -> t.getType(),
                v -> v.getValue(), "emails[type eq %s].value");
        addListValueOperations(patchRequest, originalRecord.getPhoneNumbers(), modifiedRecord.getPhoneNumbers(),
                t -> t.getType(), v -> v.getValue(), "phoneNumbers[type eq %s].value");
        addListValueOperations(patchRequest, originalRecord.getAddresses(), modifiedRecord.getAddresses(), t -> t,
                v -> v, "addreses[type eq %s]");

        return patchRequest;
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
