package us.axefan.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class RegionControlEntityListener extends org.bukkit.event.entity.EntityListener {
	
	public static RegionControl plugin;
	
	public RegionControlEntityListener(RegionControl instance) {
		plugin = instance;
	}
	
	public void onEntityDeath(EntityDeathEvent event) {
		if ((event.getEntity() instanceof Player)) {
			Player player = (Player) event.getEntity();
			// TODO: If minPlayers is violated, all players get fail condition.
			System.out.print(player.getName() + " died");
	        return;
	    }
	}
	
}
