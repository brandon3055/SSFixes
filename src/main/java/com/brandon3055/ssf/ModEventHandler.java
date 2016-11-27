package com.brandon3055.ssf;

import com.brandon3055.ssf.modules.SSModuleBase;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brandon3055 on 11/11/2016.
 */
public class ModEventHandler {

    public static final Map<EventType, List<SSModuleBase>> eventListeners = new HashMap<EventType, List<SSModuleBase>>();

    static {
        for (EventType type : EventType.values()) {
            eventListeners.put(type, new ArrayList<SSModuleBase>());
        }
    }

    @SubscribeEvent
    public void onEvent(TickEvent.ServerTickEvent event) {
        for (SSModuleBase module : eventListeners.get(EventType.SERVER_TICK)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(TickEvent.ServerTickEvent event) {
        for (SSModuleBase module : eventListeners.get(EventType.SERVER_TICK_LAST)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent
    public void onEvent(TickEvent.PlayerTickEvent event) {
        if (event.player.worldObj.isRemote) {
            return;
        }
        for (SSModuleBase module : eventListeners.get(EventType.PLAYER_TICK)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(TickEvent.PlayerTickEvent event) {
        if (event.player.worldObj.isRemote) {
            return;
        }
        for (SSModuleBase module : eventListeners.get(EventType.PLAYER_TICK_LAST)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent
    public void onEvent(PlayerInteractEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SSModuleBase module : eventListeners.get(EventType.PLAYER_INTERACT)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(PlayerInteractEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SSModuleBase module : eventListeners.get(EventType.PLAYER_INTERACT_LAST)) {
            module.onEvent(event);
        }

        try {
            Thread.sleep(200);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onEvent(BlockEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SSModuleBase module : eventListeners.get(EventType.BLOCK_EVENT)) {
            module.onEvent(event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEventLast(BlockEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        for (SSModuleBase module : eventListeners.get(EventType.BLOCK_EVENT_LAST)) {
            module.onEvent(event);
        }
    }

    public static enum EventType {
        SERVER_TICK,
        SERVER_TICK_LAST,

        PLAYER_TICK,
        PLAYER_TICK_LAST,

        PLAYER_INTERACT,
        PLAYER_INTERACT_LAST,

        BLOCK_EVENT,
        BLOCK_EVENT_LAST,
    }
}
