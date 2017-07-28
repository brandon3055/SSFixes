package com.brandon3055.ssf.modules;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by brandon3055 on 27/11/2016.
 */
public class ModuleMinecartDupeFix extends SSModuleBase {

    public ModuleMinecartDupeFix() {
        super("minecartDupeFix", "Fixes an issue where when chest minecarts go through some mod portals they drop their inventory before teleporting but also retain their inventory when they reach the other side. ");
    }

    @Override
    public void initialize() {
        super.initialize();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onEvent(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!entity.isDead && entity instanceof EntityMinecart) {
            entity.isDead = true;
            entity.changeDimension(entity.dimension);
            entity.isDead = false;
        }
    }

    @SubscribeEvent
    public void onEvent(EntityEvent event) {
        if (event.getClass().getName().equals("crazypants.enderio.api.teleport.TeleportEntityEvent") && event.getEntity() instanceof EntityMinecart) {
            event.getEntity().isDead = true;
            event.getEntity().changeDimension(0);
            event.getEntity().isDead = false;
        }
    }
}
