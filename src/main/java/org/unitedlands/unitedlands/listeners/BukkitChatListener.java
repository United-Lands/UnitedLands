package org.unitedlands.unitedlands.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.unitedlands.annotations.UnitedListener;
import org.unitedlands.unitedlands.managers.ChatChannelManager;
import io.papermc.paper.event.player.AsyncChatEvent;

@UnitedListener 
public class BukkitChatListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {
        ChatChannelManager.instance().handleMessage(event);
    }

}
