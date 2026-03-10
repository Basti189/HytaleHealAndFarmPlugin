package de.wolfsline.hytale.command;

import com.hypixel.hytale.builtin.adventure.farming.states.FarmingBlock;
import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

public class InspectorCommand extends AbstractPlayerCommand {

    public InspectorCommand(@Nonnull String name, @Nonnull String description, boolean requiresConfirmation) {
        super(name, description, requiresConfirmation);
        this.setPermissionGroup(GameMode.Adventure);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Vector3i targetPos = TargetUtil.getTargetBlock(ref, 5, store);
        if (targetPos == null) {
            player.sendMessage(Message.raw("Kein Target-Block."));
            return;
        }

        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

        // 1) Erst Target selbst prüfen
        Ref<ChunkStore> targetRef = getBlockRef(world, chunkStore, targetPos);
        TilledSoilBlock soil = targetRef == null ? null : chunkStore.getComponent(targetRef, TilledSoilBlock.getComponentType());
        Vector3i soilPos = targetPos;

        // 2) Falls Target kein Soil ist: 1 Block tiefer prüfen
        if (soil == null) {
            Vector3i belowPos = new Vector3i(targetPos.x, targetPos.y - 1, targetPos.z);
            Ref<ChunkStore> belowRef = getBlockRef(world, chunkStore, belowPos);
            TilledSoilBlock belowSoil = belowRef == null ? null : chunkStore.getComponent(belowRef, TilledSoilBlock.getComponentType());

            if (belowSoil != null) {
                soil = belowSoil;
                soilPos = belowPos;
            }
        }

        if (soil == null) {
            player.sendMessage(Message.raw("Weder Target-Block noch Block darunter ist TilledSoil."));
            return;
        }

        WorldTimeResource time = world.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
        Instant now = time.getGameTime();

        Instant wateredUntil = soil.getWateredUntil();
        Instant decayTime = soil.getDecayTime();

        player.sendMessage(Message.raw("[DEBUG] Soil gefunden"));
        player.sendMessage(Message.raw("Target Pos: " + targetPos.x + " " + targetPos.y + " " + targetPos.z));
        player.sendMessage(Message.raw("Soil Pos: " + soilPos.x + " " + soilPos.y + " " + soilPos.z));
        player.sendMessage(Message.raw("WateredUntil: " + wateredUntil));
        player.sendMessage(Message.raw("DecayTime: " + decayTime));
        player.sendMessage(Message.raw("Now: " + now));

        if (wateredUntil != null) {
            long seconds = ChronoUnit.SECONDS.between(now, wateredUntil);
            player.sendMessage(Message.raw("Water remaining: " + formatDuration(seconds)));
        }

        if (decayTime != null) {
            long seconds = ChronoUnit.SECONDS.between(now, decayTime);
            player.sendMessage(Message.raw("Decay remaining: " + formatDuration(seconds)));
        }

        // 3) Crop direkt über dem Soil prüfen
        Vector3i cropPos = new Vector3i(soilPos.x, soilPos.y + 1, soilPos.z);
        Ref<ChunkStore> cropRef = getBlockRef(world, chunkStore, cropPos);

        if (cropRef == null) {
            player.sendMessage(Message.raw("Über dem Soil existiert keine Block-Ref."));
            return;
        }

        BlockType cropType = getBlockType(world, cropPos);
        player.sendMessage(Message.raw("Crop Pos: " + cropPos.x + " " + cropPos.y + " " + cropPos.z));
        player.sendMessage(Message.raw("Crop Type: " + cropType));

        FarmingBlock farming = chunkStore.getComponent(cropRef, FarmingBlock.getComponentType());
        if (farming == null) {
            player.sendMessage(Message.raw("Soil ist aktuell nicht bepflanzt."));
            return;
        }

        player.sendMessage(Message.raw("Soil ist bepflanzt."));
        player.sendMessage(Message.raw("FarmingBlock Class: " + farming.getClass().getName()));

        // Häufige Getter direkt probieren
        printIfPresent(player, farming, "getCurrentStage", "CurrentStage");
        printIfPresent(player, farming, "getStage", "Stage");
        printIfPresent(player, farming, "getGrowthProgress", "GrowthProgress");
        printIfPresent(player, farming, "getProgress", "Progress");
        printIfPresent(player, farming, "getGrowUntil", "GrowUntil");
        printIfPresent(player, farming, "getFinishTime", "FinishTime");
        printIfPresent(player, farming, "getNextStageTime", "NextStageTime");
        printIfPresent(player, farming, "getPlantedAt", "PlantedAt");

        // Falls irgendein Zeit-Getter existiert, Restzeit direkt berechnen
        printRemainingIfInstant(player, farming, now, "getGrowUntil", "Grow remaining");
        printRemainingIfInstant(player, farming, now, "getFinishTime", "Finish remaining");
        printRemainingIfInstant(player, farming, now, "getNextStageTime", "Next stage remaining");

        // Bonus: alle parameterlosen Getter einmal dumpen, damit du sofort siehst, was die API wirklich hat
        dumpZeroArgGetters(player, farming);
    }

    private Ref<ChunkStore> getBlockRef(World world, Store<ChunkStore> chunkStore, Vector3i pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return null;
        }

        int localX = Math.floorMod(pos.x, 32);
        int localZ = Math.floorMod(pos.z, 32);
        int y = pos.y;

        BlockComponentChunk blockComponentChunk =
                chunkStore.getComponent(chunk.getReference(), BlockComponentChunk.getComponentType());

        if (blockComponentChunk == null) {
            return null;
        }

        return blockComponentChunk.getEntityReference(ChunkUtil.indexBlockInColumn(localX, y, localZ));
    }

    private BlockType getBlockType(World world, Vector3i pos) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(pos.x, pos.z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockType(pos.x, pos.y, pos.z);
    }

    private void printIfPresent(Player player, Object instance, String methodName, String label) {
        try {
            Method method = instance.getClass().getMethod(methodName);
            Object value = method.invoke(instance);
            player.sendMessage(Message.raw(label + ": " + value));
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            player.sendMessage(Message.raw(label + ": <Fehler beim Lesen: " + e.getClass().getSimpleName() + ">"));
        }
    }

    private void printRemainingIfInstant(Player player, Object instance, Instant now, String methodName, String label) {
        try {
            Method method = instance.getClass().getMethod(methodName);
            Object value = method.invoke(instance);
            if (value instanceof Instant instant) {
                long seconds = ChronoUnit.SECONDS.between(now, instant);
                player.sendMessage(Message.raw(label + ": " + formatDuration(seconds)));
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            player.sendMessage(Message.raw(label + ": <Fehler beim Berechnen: " + e.getClass().getSimpleName() + ">"));
        }
    }

    private void dumpZeroArgGetters(Player player, Object instance) {
        String methods = Arrays.stream(instance.getClass().getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> m.getName().startsWith("get") || m.getName().startsWith("is"))
                .sorted(Comparator.comparing(Method::getName))
                .map(Method::getName)
                .collect(Collectors.joining(", "));

        player.sendMessage(Message.raw("Verfügbare Getter: " + methods));

        for (Method method : instance.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            if (!method.getName().startsWith("get") && !method.getName().startsWith("is")) {
                continue;
            }

            try {
                Object value = method.invoke(instance);
                player.sendMessage(Message.raw(method.getName() + " = " + value));
            } catch (Exception e) {
                player.sendMessage(Message.raw(method.getName() + " = <" + e.getClass().getSimpleName() + ">"));
            }
        }
    }

    private static String formatDuration(long seconds) {
        if (seconds < 0) {
            seconds = 0;
        }

        long years = seconds / (365L * 86400);
        seconds %= (365L * 86400);

        long days = seconds / 86400;
        seconds %= 86400;

        long hours = seconds / 3600;
        seconds %= 3600;

        long minutes = seconds / 60;
        seconds %= 60;

        return years + "y " + days + "d " + hours + "h " + minutes + "m " + seconds + "s";
    }
}
