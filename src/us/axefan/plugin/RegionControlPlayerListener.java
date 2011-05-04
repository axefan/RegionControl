package us.axefan.plugin;

import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class RegionControlPlayerListener extends org.bukkit.event.player.PlayerListener {

	public static RegionControl plugin;
	
	public RegionControlPlayerListener(RegionControl instance) {
		plugin = instance;
	}
	
	public void onPlayerMove(PlayerMoveEvent event) {
        event.setCancelled(true);
        event.setTo(event.getFrom());
	}

	public void onPlayerQuit(PlayerQuitEvent event) {
	}
	
	public void onPlayerKick(PlayerKickEvent event) {
	}
	
	public void onPlayerRespawn(PlayerRespawnEvent event) {
	}
	
	public void onPlayerTeleport(PlayerTeleportEvent event) {
	}

}
