package com.brandon3055.ssf.modules;

import com.brandon3055.ssf.ModEventHandler;
import com.brandon3055.ssf.ModEventHandler.EventType;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brandon3055 on 27/11/2016.
 */
public abstract class SSModuleBase {

    private List<EventType> events = new ArrayList<EventType>();
    public final String moduleID;
    public final String moduleDescription;

    protected SSModuleBase(String moduleID, String moduleDescription) {
        this.moduleID = moduleID;
        this.moduleDescription = moduleDescription;
    }

    /**
     * This is called if the module is enabled and after loadConfig is called.
     * If you override this be sure to call super so events still get registered.
     * This must only be called ONCE! So dont ever call it manually.
     */
    public void initialize() {
        for (EventType type : events) {
            ModEventHandler.eventListeners.get(type).add(this);
        }
    }

    public SSModuleBase addListener(EventType eventType) {
        events.add(eventType);
        return this;
    }

    public void loadConfig(Configuration config) {

    }

    public void onEvent(TickEvent.ServerTickEvent event) {

    }

    public void onEvent(TickEvent.PlayerTickEvent event) {

    }

    public void onEvent(PlayerInteractEvent event) {

    }

    public void onEvent(BlockEvent event) {

    }
}
