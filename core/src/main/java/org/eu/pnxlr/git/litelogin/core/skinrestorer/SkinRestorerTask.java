package org.eu.pnxlr.git.litelogin.core.skinrestorer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.profile.Property;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.configuration.SkinRestorerConfig;
import org.eu.pnxlr.git.litelogin.core.configuration.service.BaseServiceConfig;
import org.eu.pnxlr.git.litelogin.core.main.Core;
import okhttp3.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Skin restore task.
 */
public class SkinRestorerTask implements Callable<SkinRestorerResultImpl> {
    private final Core core;
    private final BaseServiceConfig config;
    private final OkHttpClient okHttpClient;
    private final String skinUrl;
    private final String skinModel;
    private final GameProfile profile;

    protected SkinRestorerTask(Core core, BaseServiceConfig config, OkHttpClient okHttpClient, String skinUrl, String skinModel, GameProfile profile) {
        this.core = core;
        this.config = config;
        this.okHttpClient = okHttpClient;
        this.skinUrl = skinUrl;
        this.skinModel = skinModel;
        this.profile = profile;
    }

    /**
     * Performs skin restoration.
     */
    public SkinRestorerResultImpl call() throws Exception {
        byte[] bytes;
        try {
            bytes = requireValidSkin(skinUrl);
        } catch (Exception e) {
            return SkinRestorerResultImpl.ofBadSkin(e);
        }
        Request request;

        if (config.getSkinRestorer().getMethod() == SkinRestorerConfig.Method.UPLOAD) {
            request = new Request.Builder()
                    .url("https://api.mineskin.org/generate/upload")
                    .header("User-Agent", core.getHttpRequestHeaderUserAgent())
                    .post(new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("name", UUID.randomUUID().toString().substring(0, 6))
                            .addFormDataPart("variant", skinModel)
                            .addFormDataPart("visibility", "0")
                            .addFormDataPart("file", "upload.png",
                                    RequestBody.create(bytes, MediaType.parse("multipart/form-data"))
                            )
                            .build())
                    .build();
        } else {
            JsonObject jo = new JsonObject();
            jo.addProperty("name", UUID.randomUUID().toString().substring(0, 6));
            jo.addProperty("variant", skinModel);
            jo.addProperty("visibility", 0);
            jo.addProperty("url", skinUrl);

            request = new Request.Builder()
                    .url("https://api.mineskin.org/generate/url")
                    .header("User-Agent", core.getHttpRequestHeaderUserAgent())
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(core.getGson().toJson(jo), MediaType.parse("application/json; charset=utf-8")))
                    .build();
        }

        Response execute = okHttpClient.newCall(request).execute();
        JsonObject jo = JsonParser.parseString(Objects.requireNonNull(execute.body()).string()).getAsJsonObject()
                .getAsJsonObject("data")
                .getAsJsonObject("texture");
        String value = jo.getAsJsonPrimitive("value").getAsString();
        String signature = jo.getAsJsonPrimitive("signature").getAsString();
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

    private byte[] requireValidSkin(String skinUrl) throws IOException {
        Request request = new Request.Builder()
                .get()
                .header("User-Agent", core.getHttpRequestHeaderUserAgent())
                .url(skinUrl)
                .build();
        // 下载皮肤原件
        byte[] bytes = Objects.requireNonNull(okHttpClient.newCall(request).execute().body()).bytes();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bais);

            if (image.getWidth() != 64) {
                throw new SkinRestorerException("Skin width is not 64.");
            }
            if (image.getHeight() != 32 && image.getHeight() != 64) {
                throw new SkinRestorerException("Skin height is not 64 or 32.");
            }
            return bytes;
        }
    }
}
