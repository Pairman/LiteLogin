package org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil.serialize;

import com.google.gson.*;
import lombok.NoArgsConstructor;
import org.eu.pnxlr.git.litelogin.api.profile.Property;
import org.eu.pnxlr.git.litelogin.core.auth.service.yggdrasil.UnmodifiableGameProfile;

import java.lang.reflect.Type;

/**
 * Gson serializer for Property.
 */
@NoArgsConstructor
public class PropertySerializer implements JsonSerializer<Property>, JsonDeserializer<Property> {

    @Override
    public Property deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Property ret = new Property();
        if (json.isJsonObject()) {
            JsonObject root = json.getAsJsonObject();
            ret.setName(root.get("name").getAsString());
            ret.setValue(root.get("value").getAsString());
            if (root.has("signature")) ret.setSignature(root.get("signature").getAsString());
        }
        return UnmodifiableGameProfile.UnmodifiableProperty.unmodifiable(ret);
    }

    @Override
    public JsonElement serialize(Property src, Type typeOfSrc, JsonSerializationContext context) {
        var ret = new JsonObject();
        ret.addProperty("name", src.getName());
        ret.addProperty("value", src.getValue());
        if (src.getSignature() != null) ret.addProperty("signature", src.getSignature());
        return ret;
    }
}
