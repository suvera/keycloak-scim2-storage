package dev.suvera.keycloak.scim2.storage.storage;

import java.util.List;
import java.util.function.Function;

import dev.suvera.scim2.schema.data.misc.PatchRequest;
import dev.suvera.scim2.schema.data.user.UserRecord;
import dev.suvera.scim2.schema.enums.PatchOp;

public class UserRecordPatchBuilder {
    private UserRecordPatchBuilder() { /* static class */ }

    public static PatchRequest<UserRecord> buildPatchRequest(UserRecord modifiedRecord, UserRecord originalRecord) {
        PatchRequest<UserRecord> patchRequest = new PatchRequest<>(UserRecord.class);

        addOperation(patchRequest, originalRecord.getName(), modifiedRecord.getName(), "name");
        addOperation(patchRequest, originalRecord.getDisplayName(), modifiedRecord.getDisplayName(), "displayName");
        addOperation(patchRequest, originalRecord.getNickName(), modifiedRecord.getNickName(), "nickName");
        addOperation(patchRequest, originalRecord.getProfileUrl(), modifiedRecord.getProfileUrl(), "profileUrl");
        addOperation(patchRequest, originalRecord.getTitle(), modifiedRecord.getTitle(), "title");
        addOperation(patchRequest, originalRecord.getUserType(), modifiedRecord.getUserType(), "userType");
        addOperation(patchRequest, originalRecord.getPreferredLanguage(), modifiedRecord.getPreferredLanguage(), "preferredLanguage");
        addOperation(patchRequest, originalRecord.getLocale(), modifiedRecord.getLocale(), "locale");
        addOperation(patchRequest, originalRecord.getTimezone(), modifiedRecord.getTimezone(), "timezone");
        addOperation(patchRequest, originalRecord.getPassword(), modifiedRecord.getPassword(), "password");

        if (originalRecord.isActive() != modifiedRecord.isActive()) {
            patchRequest.addOperation(PatchOp.REPLACE, "active", modifiedRecord.isActive());
        }

        addListValueOperations(patchRequest, originalRecord.getEmails(), modifiedRecord.getEmails(), t -> t.getType(), v -> v.getValue(), "emails[type eq %s].value");
        addListValueOperations(patchRequest, originalRecord.getPhoneNumbers(), modifiedRecord.getPhoneNumbers(), t -> t.getType(), v -> v.getValue(), "phoneNumbers[type eq %s].value");
        addListValueOperations(patchRequest, originalRecord.getAddresses(), modifiedRecord.getAddresses(), t -> t, v -> v, "addreses[type eq %s]");

        return patchRequest;
    }

    private static <T> void addListValueOperations(PatchRequest<UserRecord> patch, List<T> values1, List<T> values2,
            Function<T, Object> typeProp, Function<T, Object> valueProp, String path) {
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
                        valueProp.apply(val2), String.format(path, typeProp.apply(val2)));
            }
        }

        if (values1 != null) {
            values1.forEach(v -> patch.addOperation(PatchOp.REMOVE, String.format(path, typeProp.apply(v)), ""));
        }
    }

    private static void addOperation(PatchRequest<UserRecord> patch, Object val1, Object val2, String path) {
        if (isNullOrEmpty(val1)) {
            if (!isNullOrEmpty(val2)) {
                patch.addOperation(PatchOp.ADD, path, val2);
            }
        } else {
            if (isNullOrEmpty(val2)) {
                patch.addOperation(PatchOp.REMOVE, path, val2);
            } else if (!val1.equals(val2)) {
                patch.addOperation(PatchOp.REPLACE, path, val2);
            }
        }

    }

    private static boolean isNullOrEmpty(Object val) {
        return val == null || (val instanceof String && ((String) val).trim().isEmpty());
    }
}
