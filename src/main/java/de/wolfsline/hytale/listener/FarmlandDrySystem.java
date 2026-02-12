package de.wolfsline.hytale.listener;

import com.hypixel.hytale.builtin.adventure.farming.states.TilledSoilBlock;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

// https://github.com/Ranork/Hytale-Server-Unpacked/blob/f33bf4c871e728e2b2edaf63e8e97f07597567ae/com/hypixel/hytale/builtin/adventure/farming/FarmingSystems%24Ticking.java#L240

public class FarmlandDrySystem extends EntityTickingSystem<ChunkStore> {

    private static final Query<ChunkStore> QUERY = (Query<ChunkStore>)Query.and(new Query[] { (Query)BlockSection.getComponentType(), (Query) ChunkSection.getComponentType() });

    private void broadcast(World world, String text) {
        world.execute(() -> {
            for (PlayerRef p : Universe.get().getPlayers()) {
                if (p != null) p.sendMessage(Message.raw(text));
            }
        });
    }

    @NullableDecl
    @Override
    public Query<ChunkStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(float dt, int index, @NonNullDecl ArchetypeChunk<ChunkStore> archetypeChunk, @NonNullDecl Store<ChunkStore> store, @NonNullDecl CommandBuffer<ChunkStore> commandBuffer) {
        BlockSection blocks = (BlockSection)archetypeChunk.getComponent(index, BlockSection.getComponentType());
        assert blocks != null;

        if (blocks.getTickingBlocksCountCopy() == 0) return;

        ChunkSection section = (ChunkSection)archetypeChunk.getComponent(index, ChunkSection.getComponentType());
        assert section != null;

        if (section.getChunkColumnReference() == null || !section.getChunkColumnReference().isValid()) return;

        BlockComponentChunk blockComponentChunk = (BlockComponentChunk)commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());
        assert blockComponentChunk != null;

        Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
        BlockChunk blockChunk = (BlockChunk)commandBuffer.getComponent(section.getChunkColumnReference(), BlockChunk.getComponentType());
        assert blockChunk != null;

        blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) -> {
            Ref<ChunkStore> blockRef = blockComponentChunk1.getEntityReference(ChunkUtil.indexBlockInColumn(localX, localY, localZ));
            if (blockRef == null) return BlockTickStrategy.IGNORED;

            TilledSoilBlock soil = (TilledSoilBlock)commandBuffer1.getComponent(blockRef, TilledSoilBlock.getComponentType());
            if (soil == null) return BlockTickStrategy.IGNORED;
            tickSoil(commandBuffer1, blockComponentChunk1, blockRef, soil);

            return BlockTickStrategy.SLEEP;
        });

    }

    private void tickSoil(CommandBuffer<ChunkStore> commandBuffer1, BlockComponentChunk blockComponentChunk1, Ref<ChunkStore> blockRef, TilledSoilBlock soil) {
        soil.setExternalWater(true);
    }
}
