package com.frontierinstallations;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.tags.TagKey;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Standalone mod: structures are supplied as data-driven worldgen resources. */
@Mod(FrontierInstallations.MOD_ID)
public final class FrontierInstallations {
    public static final String MOD_ID = "frontierinstallations";
    private static final int SEARCH_RADIUS_CHUNKS = 100;

    public FrontierInstallations() {}

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class CommandRegistration {
        @SubscribeEvent
        public static void registerCommands(RegisterCommandsEvent event) {
            event.getDispatcher().register(buildCommand("nearestbuilding"));
            event.getDispatcher().register(buildCommand("nearest_building"));
        }

        private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>
                buildCommand(String literal) {
            var command = Commands.literal(literal)
                .executes(ctx -> find(ctx.getSource(), "all", "любая постройка"));
            command.then(Commands.literal("field_dugout")
                .executes(ctx -> find(ctx.getSource(), "field_dugout", "землянка")));
            command.then(Commands.literal("observation_post")
                .executes(ctx -> find(ctx.getSource(), "observation_post", "наблюдательный пост")));
            command.then(Commands.literal("small_camp")
                .executes(ctx -> find(ctx.getSource(), "small_camp", "маленький лагерь")));
            command.then(Commands.literal("reinforced_bunker")
                .executes(ctx -> find(ctx.getSource(), "reinforced_bunker", "бункер")));
            command.then(Commands.literal("fortified_camp")
                .executes(ctx -> find(ctx.getSource(), "fortified_camp", "укреплённый лагерь")));
            command.then(Commands.literal("large_base")
                .executes(ctx -> find(ctx.getSource(), "large_base", "большая база")));
            return command;
        }

        private static int find(CommandSourceStack source, String tagName, String displayName) {
            ServerLevel level = source.getLevel();
            TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE,
                new ResourceLocation(MOD_ID, tagName));
            BlockPos origin = BlockPos.containing(source.getPosition());
            HolderSet.Named<Structure> structures = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE).getTag(tag).orElse(null);
            if (structures == null || structures.size() == 0) {
                source.sendFailure(Component.literal("Тег структур «" + tagName
                    + "» не найден или пуст. Проверьте установку мода и загрузку данных мира."));
                return 0;
            }
            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, structures, origin, SEARCH_RADIUS_CHUNKS, false);
            if (result == null) {
                source.sendFailure(Component.literal("Постройка «" + displayName
                    + "» не найдена в радиусе поиска. Попробуйте в новых чанках."));
                return 0;
            }
            BlockPos pos = result.getFirst();
            double distance = Math.sqrt(origin.distSqr(pos));
            source.sendSuccess(() -> Component.literal("Найдена: " + displayName + " — X: "
                + pos.getX() + " Y: " + pos.getY() + " Z: " + pos.getZ()
                + " (примерно " + Math.round(distance) + " блоков)"), false);
            return 1;
        }
    }
}
