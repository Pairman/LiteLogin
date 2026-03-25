package org.eu.pnxlr.git.litelogin.core.auth.validate.entry;

import lombok.SneakyThrows;
import org.eu.pnxlr.git.litelogin.api.internal.logger.LoggerProvider;
import org.eu.pnxlr.git.litelogin.api.internal.plugin.IPlayer;
import org.eu.pnxlr.git.litelogin.api.internal.util.Pair;
import org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil;
import org.eu.pnxlr.git.litelogin.core.auth.validate.ValidateContext;
import org.eu.pnxlr.git.litelogin.core.main.Core;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.UUID;

/**
 * 玩家初始 UUID 生成程序
 */
public class AssignInGameStep {
    private final Core core;

    public AssignInGameStep(Core core) {
        this.core = core;
    }

    @SneakyThrows
    public boolean run(ValidateContext validateContext) {

        // 从数据库里面读登录档案的游戏内 UUID
        UUID inGameUUID = core.getSqlManager().getUserDataTable().getInGameUUID(
                validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId()
        );

        // 如果这个 UUID 不存在，表示是个预新玩家或是档案被清理的新玩家。这时需要分配个全新的身份卡给它。
        String loginName = validateContext.getBaseServiceAuthenticationResult().getResponse().getName();
        if (inGameUUID == null) {

            inGameUUID = validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getInitUUID()
                    .generateUUID(validateContext.getBaseServiceAuthenticationResult().getResponse().getId(), loginName);

            // 需要线程安全
            synchronized (AssignInGameStep.class) {
                // 取没有被占用的 UUID
                while (core.getSqlManager().getInGameProfileTable().dataExists(inGameUUID)) {
                    LoggerProvider.getLogger().warn(String.format("UUID %s has been used and will take a random UUID instead.", inGameUUID));
                    inGameUUID = UUID.randomUUID();
                }
                // 身份卡UUID数据被确定
                // 更新数据
                core.getSqlManager().getUserDataTable().setInGameUUID(
                        validateContext.getBaseServiceAuthenticationResult().getResponse().getId(),
                        validateContext.getBaseServiceAuthenticationResult().getServiceConfig().getId(),
                        inGameUUID);
            }
        }
        if (core.getPluginConfig().isAutoNameChange() && validateContext.isOnlineNameUpdated()) {
            String username = core.getSqlManager().getInGameProfileTable().getUsername(inGameUUID);
            if (!ValueUtil.isEmpty(username)) {
                core.getSqlManager().getInGameProfileTable().eraseUsername(username);
            }
        }

        // 身份卡UUID数据存在，看看数据库中有没有对应的记录
        boolean exist = core.getSqlManager().getInGameProfileTable().dataExists(inGameUUID);
        if (exist) {
            String username = core.getSqlManager().getInGameProfileTable().getUsername(inGameUUID);
            if (!ValueUtil.isEmpty(username)) {
                validateContext.getInGameProfile().setId(inGameUUID);
                validateContext.getInGameProfile().setName(username);
                return true;
            }
        }

        String fixName = validateContext.getBaseServiceAuthenticationResult().getServiceConfig().generateName(loginName);
        if(fixName.isEmpty()) fixName = "1";

        String initFixName = fixName;
        if (core.getPluginConfig().isNameCorrect()) {
            boolean modified = false;
            UUID ownerUUID;
            while ((ownerUUID = core.getSqlManager().getInGameProfileTable().getInGameUUIDIgnoreCase(fixName)) != null) {
                if(ownerUUID.equals(inGameUUID)) break;
                fixName = incrementString(fixName);
                modified = true;
            }

            if(modified){
                UUID finalInGameUUID = inGameUUID;
                String finalFixName = fixName;
                LoggerProvider.getLogger().warn(String.format("The name %s is occupied, change it to %s.", initFixName, fixName));
                core.getPlugin().getRunServer().getScheduler().runTaskAsync(() -> {
                    IPlayer player = core.getPlugin().getRunServer().getPlayerManager().getPlayer(finalInGameUUID);
                    if (player != null) {
                        player.sendMessagePL(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                                "§cYour original name §e{old_name}§c was already in use, so it was changed automatically to §e{new_name}§c.",
                                new Pair<>("old_name", initFixName),
                                new Pair<>("new_name", finalFixName)
                        ));
                    }
                }, 2000);
            }
        }

        // Username 需要更新
        if (exist) {
            try {
                core.getSqlManager().getInGameProfileTable().updateUsername(inGameUUID,
                        fixName);
                validateContext.getInGameProfile().setId(inGameUUID);
                validateContext.getInGameProfile().setName(fixName);
                return true;
            } catch (SQLIntegrityConstraintViolationException e) {
                validateContext.setDisallowMessage(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cYour username §e{name}§c is already used by another player.",
                        new Pair<>("name", validateContext.getInGameProfile().getName())
                ));
                return false;
            }
        } else {
            try {
                core.getSqlManager().getInGameProfileTable().insertNewData(inGameUUID,
                        fixName);
                validateContext.getInGameProfile().setId(inGameUUID);
                validateContext.getInGameProfile().setName(fixName);
                return true;
            } catch (SQLIntegrityConstraintViolationException e) {
                validateContext.setDisallowMessage(org.eu.pnxlr.git.litelogin.api.internal.util.ValueUtil.transPapi(
                        "§cYour username §e{name}§c is already used by another player.",
                        new Pair<>("name", validateContext.getInGameProfile().getName())
                ));
                return false;
            }
        }
    }

    private String incrementString(String source){
        if (source.isEmpty()) return "1";

        char c = source.charAt(source.length() - 1);
        if (Character.isDigit(c)) {
            int i = Character.getNumericValue(c);
            if(i == 9){
                return incrementString(source.substring(0, source.length() - 1)) + "0";
            } else {
                return source.substring(0, source.length() - 1) + (i + 1);
            }
        }

        return source + "1";
    }
}
