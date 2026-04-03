package org.eu.pnxlr.git.litelogin.core.skinrestorer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.eu.pnxlr.git.litelogin.api.profile.GameProfile;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.result.SkinRestorerResult;

/**
 * Skin restoration result.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class SkinRestorerResultImpl implements SkinRestorerResult {
    private final Reason reason;
    private final GameProfile response;
    private final Throwable throwable;

    public static SkinRestorerResultImpl ofNoSkin() {
        return new SkinRestorerResultImpl(Reason.NO_SKIN, null, null);
    }

    public static SkinRestorerResultImpl ofNoRestorer() {
        return new SkinRestorerResultImpl(Reason.NO_RESTORER, null, null);
    }

    public static SkinRestorerResultImpl ofSignatureValid() {
        return new SkinRestorerResultImpl(Reason.SIGNATURE_VALID, null, null);
    }

    public static SkinRestorerResultImpl ofRestorerAsync() {
        return new SkinRestorerResultImpl(Reason.RESTORER_ASYNC, null, null);
    }

    public static SkinRestorerResultImpl ofUseCache(GameProfile profile) {
        return new SkinRestorerResultImpl(Reason.USE_CACHE, profile, null);
    }

    public static SkinRestorerResultImpl ofRestorerSucceed(GameProfile profile) {
        return new SkinRestorerResultImpl(Reason.RESTORER_SUCCEED, profile, null);
    }

    public static SkinRestorerResultImpl ofBadSkin(Throwable throwable) {
        return new SkinRestorerResultImpl(Reason.BAD_SKIN, null, throwable);
    }

    public static void handleSkinRestoreResult(Throwable throwable) {
        LoggerProvider.getLogger().error("An exception occurred while processing the skin repair.", throwable);
    }

    public static void handleSkinRestoreResult(SkinRestorerResultImpl result) {
        if (result.getThrowable() != null) {
            handleSkinRestoreResult(result.getThrowable());
        }
    }
}
