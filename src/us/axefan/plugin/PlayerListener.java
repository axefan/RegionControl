package us.axefan.plugin;

import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerListener extends org.bukkit.event.player.PlayerListener {

	public static RegionControl plugin;
	
	public PlayerListener(RegionControl instance) {
		plugin = instance;
//		this.onPlayerAnimation(event);
//		this.onPlayerInteract(event);
//		this.onPlayerTeleport(event)
	}
	
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled()) return;
//		Player player = event.getPlayer();
	//	event.setTo(event.getFrom());
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
