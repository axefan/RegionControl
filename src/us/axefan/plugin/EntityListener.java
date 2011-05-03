package us.axefan.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityListener extends org.bukkit.event.entity.EntityListener {
	
	public static RegionControl plugin;
	
	public EntityListener(RegionControl instance) {
		plugin = instance;
	}

	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player)) {
	        return;
	    }
	}
	
}
