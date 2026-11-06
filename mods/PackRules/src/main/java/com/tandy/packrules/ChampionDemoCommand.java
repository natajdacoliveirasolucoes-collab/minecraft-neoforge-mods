package com.tandy.packrules;

import java.util.Random;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import mobchampions.MobChampions;
import mobchampions.config.ConfigHandler;
import mobchampions.network.MobChampion;
import mobchampions.platform.Services;

/** OP-only local diagnostics; the normal MobChampions spawn hook sends the actual alert. */
public final class ChampionDemoCommand {
    private ChampionDemoCommand() { }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("packrules")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("champion-status").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "MobChampions em memoria: avisos=" + ConfigHandler.Common.enableSpawnMessage()
                    + ", rank minimo=" + ConfigHandler.Common.getSpawnMessageMinimumRank()
                    + ", alcance=" + ConfigHandler.Common.getSpawnMessageRange()), false);
                return 1;
            }))
            .then(Commands.literal("epic").then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> spawn(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int spawn(CommandSourceStack source, ServerPlayer player) {
        if (!ConfigHandler.Common.enableSpawnMessage()
                || ConfigHandler.Common.getSpawnMessageMinimumRank() > MobChampion.Rank.EPIC.ordinal()) {
            source.sendFailure(Component.literal("Aviso de epico nao esta ativo em memoria. Nenhum mob gerado."));
            return 0;
        }
        int total = ConfigHandler.Common.getTotalWeight();
        if (total <= 0) return 0;
        long chosenSeed = -1;
        for (long seed = 0; seed < 100000; seed++) {
            if (ConfigHandler.Common.getChampionByWeight(new Random(seed).nextInt(total)) == MobChampion.Rank.EPIC) {
                chosenSeed = seed;
                break;
            }
        }
        if (chosenSeed < 0) {
            source.sendFailure(Component.literal("O peso de epicos esta zerado. Nenhum mob gerado."));
            return 0;
        }
        var level = player.serverLevel();
        Zombie zombie = new Zombie(EntityType.ZOMBIE, level);
        zombie.setPos(player.getX() + 5, player.getY(), player.getZ());
        zombie.setPersistenceRequired();
        zombie.setCustomName(Component.literal("EPICO NATURAL - teste unico").withStyle(ChatFormatting.LIGHT_PURPLE));
        zombie.setCustomNameVisible(true);
        zombie.addTag("native_epic_verified");
        // A deterministic single roll selects EPIC without changing the configured population weights.
        // Finalization runs the mod's own rank builder AND its own notification path.
        synchronized (MobChampions.RANDOM) {
            MobChampions.RANDOM.setSeed(chosenSeed);
            EventHooks.finalizeMobSpawn(zombie, level, level.getCurrentDifficultyAt(zombie.blockPosition()),
                MobSpawnType.COMMAND, null);
        }
        var champion = Services.PLATFORM.getMobChampionData(zombie).orElseThrow();
        if (champion.getRank() != MobChampion.Rank.EPIC) {
            zombie.discard();
            source.sendFailure(Component.literal("Rank inesperado; o mob nao foi adicionado ao mundo."));
            return 0;
        }
        if (!level.addFreshEntity(zombie)) return 0;
        source.sendSuccess(() -> Component.literal("Um unico epico gerado pelo hook nativo: vida="
            + zombie.getHealth() + ", rank=" + champion.getRank()
            + ", distancia=" + zombie.distanceTo(player)), false);
        return 1;
    }
}
