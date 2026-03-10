package de.wolfsline.hytale.listener;

import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.time.Instant;

public class SoilAddedListener extends RefSystem<ChunkStore> {

    private static final Query<ChunkStore> QUERY = Query.and(BlockModule.BlockStateInfo.getComponentType(), TilledSoilBlock.getComponentType());

    @NullableDecl
    @Override
    public Query<ChunkStore> getQuery() {
        return QUERY;
    }


    @Override
    public void onEntityAdded(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl AddReason addReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
        TilledSoilBlock soil = commandBuffer.getComponent(ref, TilledSoilBlock.getComponentType());
        assert soil != null;

        BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        assert info != null;

        World world = commandBuffer.getExternalData().getWorld();

        // "weit genug in die Zukunft", aber nicht Instant.MAX
        WorldTimeResource time = world.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
        Instant now = time.getGameTime();
        Instant farFuture = now.plusSeconds(60L * 60 * 24 * 365 * 10); // 10 Jahre

        // nur setzen, wenn es NICHT schon weit genug ist
        boolean writeToCommandBuffer = false;
        Instant cur = soil.getWateredUntil();
        if (cur == null || cur.isBefore(farFuture.minusSeconds(60))) {
            soil.setWateredUntil(farFuture);

            writeToCommandBuffer = true;

            // broadcast(world, "§bSoil wateredUntil gesetzt: " + farFuture);
        }

        Instant curDecay = soil.getDecayTime();
        if (curDecay == null || curDecay.isBefore(farFuture.minusSeconds(60))) {
            soil.setDecayTime(farFuture);

            writeToCommandBuffer = true;

            // broadcast(world, "§bSoil decayedUntil gesetzt: " + farFuture);
        }

        if (writeToCommandBuffer) {
            // sicher persistieren
            commandBuffer.putComponent(ref, TilledSoilBlock.getComponentType(), soil);
        }
    }

    @Override
    public void onEntityRemove(@NonNullDecl Ref<ChunkStore> ref, @NonNullDecl RemoveReason removeReason, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {

    }

    private void broadcast(World world, String text) {
        world.execute(() -> {
            for (PlayerRef p : Universe.get().getPlayers()) {
                if (p != null) p.sendMessage(Message.raw(text));
            }
        });
    }


}
