package com.brandon3055.ssf.modules;

import com.brandon3055.ssf.ModEventHandler.EventType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketHeldItemChange;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by brandon3055 on 27/11/2016.
 */
public class ModuleBagDupeFix extends SSModuleBase {
    private Map<String, Integer[]> playersWithBagsList = new HashMap<String, Integer[]>();

    public ModuleBagDupeFix() {
        super("bagDupeFix", "Stops a dupe bug where you can right click a bag and before the gui opens switch to a different bag of the same type using the number keys \nand the items in the first bag will be duplicated in the second bag. \nThis is a bigger issue on servers where latency makes switching before the gui opens easier.\nKnown Culprits:\nTraveler's Sack (Actually Additions)\nForestry backpacks");
        this.addListener(EventType.PLAYER_TICK).addListener(EventType.PLAYER_INTERACT);
    }

    @Override
    public void onEvent(TickEvent.PlayerTickEvent event) {
        if (playersWithBagsList.keySet().contains(event.player.getName())){
            if (event.player.inventory.currentItem != playersWithBagsList.get(event.player.getName())[0]) {
                event.player.inventory.currentItem = playersWithBagsList.get(event.player.getName())[0];
                ((EntityPlayerMP)event.player).connection.sendPacket(new SPacketHeldItemChange(event.player.inventory.currentItem));
            }

            if (FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() - playersWithBagsList.get(event.player.getName())[1] > 20) {
                playersWithBagsList.remove(event.player.getName());
            }
        }
    }

    @Override
    public void onEvent(PlayerInteractEvent event) {
        if (event instanceof RightClickItem && event.getItemStack() != null) {
            ItemStack stack = event.getItemStack();
            Item item = stack.getItem();
            String itemName = item.getUnlocalizedName().toLowerCase();
            EntityPlayer player = event.getEntityPlayer();

            if (itemName.contains("bag") || itemName.contains("backpack") || itemName.contains("satchel") || itemName.contains("pouch")) {
                playersWithBagsList.put(player.getName(), new Integer[]{player.inventory.currentItem, FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter()});
            }
        }
    }
}
