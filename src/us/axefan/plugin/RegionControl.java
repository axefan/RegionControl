package us.axefan.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditAPI;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class RegionControl extends JavaPlugin {
	
	private WorldEditPlugin worldEditPlugin = null;
	private WorldEditAPI worldEditApi = null;
    private final RegionControlBlockListener blockListener = new RegionControlBlockListener(this);
	
	public void onEnable() {
        PluginDescriptionFile desc = this.getDescription();
        System.out.println(desc.getName() + " starting...");
		PluginManager pm = getServer().getPluginManager();
		//pm.registerEvent(Event.Type.REDSTONE_CHANGE, blockListener, Event.Priority.Normal, this);
		this.getCommand("rc").setExecutor(new CommandExecutor (this));
		this.worldEditPlugin = (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
		if (this.worldEditPlugin != null){
			this.worldEditApi = new WorldEditAPI(this.worldEditPlugin);
			if (this.worldEditApi != null) System.out.println(desc.getName() + " is hooked into WorldEdit");
		}
		this.setupDatabase();
        System.out.println(desc.getName() + " " + desc.getVersion() + " enabled");
	}
	
    private void setupDatabase() {
		try {
			File ebeans = new File("ebean.properties");
			if (!ebeans.exists()) {
				try {
					ebeans.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.getDatabase().find(ControlledRegion.class).findRowCount();
		} catch (PersistenceException ex) {
			System.out.println(this.getDescription().getName() + " installing database");
			installDDL();
		}
	}
    
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(ControlledRegion.class);
		list.add(RegionSnapshot.class);
		list.add(SavedBlock.class);
		return list;
	}
	
	public void onDisable() {
	}
	
	public WorldEditPlugin getWorldEditPlugin() {
		return this.worldEditPlugin;
	}
	
	public WorldEditAPI getWorldEditApi() {
		return this.worldEditApi;
	}
	
}