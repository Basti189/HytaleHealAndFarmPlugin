package de.wolfsline.hytale.listener;

import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSleep;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WakeUpHealSystem extends RefChangeSystem<EntityStore, PlayerSomnolence> {

    @NonNullDecl
    @Override
    public ComponentType<EntityStore, PlayerSomnolence> componentType() {
        return PlayerSomnolence.getComponentType();
    }

    @Override
    public Query<EntityStore> getQuery() {
        // nur Entities mit UUID
        return Query.and(PlayerSomnolence.getComponentType(), UUIDComponent.getComponentType());
    }

    // Component wurde NEU hinzugefügt
    @Override
    public void onComponentAdded(Ref<EntityStore> ref,
                                 PlayerSomnolence newC,
                                 Store<EntityStore> store,
                                 CommandBuffer<EntityStore> cb) {
       // sendDebug(ref, store, "ADDED", newC);
    }

    // Component wurde ERSETZT / neu gesetzt
    @Override
    public void onComponentSet(Ref<EntityStore> ref,
                               PlayerSomnolence oldC,
                               PlayerSomnolence newC,
                               Store<EntityStore> store,
                               CommandBuffer<EntityStore> cb) {
        //sendDebug(ref, store, "SET", newC);
        if (!(newC.getSleepState() instanceof PlayerSleep.MorningWakeUp)) return;

        EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
        assert stats != null;

        EntityStatValue healthValue = stats.get(DefaultEntityStatTypes.getHealth());
        float currentHealth = 0.0F;
        if (healthValue != null) {
            currentHealth = healthValue.get();
        }

        if (currentHealth < 100.0F) {
            stats.maximizeStatValue(DefaultEntityStatTypes.getHealth());
            UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuid != null) {
                PlayerRef player = Universe.get().getPlayer(uuid.getUuid());
                if (player != null) {
                    NotificationUtil.sendNotification(player.getPacketHandler(), "Leben vollständig regeneriert!", NotificationStyle.Success);
                }
            }
        }
    }

    // Component wurde ENTFERNT
    @Override
    public void onComponentRemoved(Ref<EntityStore> ref,
                                   PlayerSomnolence oldC,
                                   Store<EntityStore> store,
                                   CommandBuffer<EntityStore> cb) {
       // sendDebug(ref, store, "REMOVED", oldC);
    }

    private void sendDebug(Ref<EntityStore> ref,
                           Store<EntityStore> store,
                           String type,
                           PlayerSomnolence c) {

        UUIDComponent uuidComp = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComp == null) return;

        PlayerRef player = Universe.get().getPlayer(uuidComp.getUuid());
        if (player == null) return;

        String state = (c == null || c.getSleepState() == null)
                ? "null"
                : c.getSleepState().getClass().getSimpleName();

        player.sendMessage(Message.raw("§e[Somnolence " + type + "] State=" + state));
    }
}
