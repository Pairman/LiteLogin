package org.eu.pnxlr.git.litelogin.core.skinrestorer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eu.pnxlr.git.litelogin.api.internal.main.LiteLoginConstants;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.profile.Property;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.http.HttpClientHelper;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Skin restore task.
 */
public class SkinRestorerTask implements Callable<SkinRestorerResultImpl> {
    private static final String MINESKIN_GENERATE_URL = "https://api.mineskin.org/generate/url";

    private final Core core;
    private final long timeoutMillis;
    private final int retryCount;
    private final long retryDelayMillis;
    private final String skinUrl;
    private final String skinModel;
    private final GameProfile profile;

    protected SkinRestorerTask(Core core, long timeoutMillis, int retryCount, long retryDelayMillis, String skinUrl, String skinModel, GameProfile profile) {
        this.core = core;
        this.timeoutMillis = timeoutMillis;
        this.retryCount = retryCount;
        this.retryDelayMillis = retryDelayMillis;
        this.skinUrl = skinUrl;
        this.skinModel = skinModel;
        this.profile = profile;
    }

    /**
     * Performs skin restoration.
     */
    public SkinRestorerResultImpl call() throws Exception {
        try {
            requireValidSkin(skinUrl);
        } catch (Exception e) {
            return SkinRestorerResultImpl.ofBadSkin(e);
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("name", UUID.randomUUID().toString().substring(0, 6));
        payload.addProperty("variant", skinModel);
        payload.addProperty("visibility", 0);
        payload.addProperty("url", skinUrl);

        String response = HttpClientHelper.postJson(
                MINESKIN_GENERATE_URL,
                core.getGson().toJson(payload),
                timeoutMillis,
                retryCount,
                retryDelayMillis
        );
        JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject()
                .getAsJsonObject("data")
                .getAsJsonObject("texture");
        String value = responseJson.getAsJsonPrimitive("value").getAsString();
        String signature = responseJson.getAsJsonPrimitive("signature").getAsString();
        try {
            core.getSqlManager().getSkinRestoredCacheTable().insertNew(ValueUtil.sha256(skinUrl), skinModel, value, signature);
        } catch (Exception e) {
            LoggerProvider.getLogger().warn("An exception occurred while saving restored skin data.", e);
        }
        Property restoredProperty = new Property();
        restoredProperty.setName("textures");
        restoredProperty.setValue(value);
        restoredProperty.setSignature(signature);
        profile.getPropertyMap().remove("textures");
        profile.getPropertyMap().put("textures", restoredProperty);
        return SkinRestorerResultImpl.ofRestorerSucceed(profile);
    }

    private void requireValidSkin(String skinUrl) throws IOException {
        byte[] bytes = HttpClientHelper.getBytes(skinUrl, timeoutMillis, retryCount, retryDelayMillis);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bais);

            if (image.getWidth() != 64) {
                throw new IOException("Skin width is not 64.");
            }
            if (image.getHeight() != 32 && image.getHeight() != 64) {
                throw new IOException("Skin height is not 64 or 32.");
            }
        }
    }
}
