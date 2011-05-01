package us.axefan.plugin;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class RegionControlBlockListener extends BlockListener {

	public static RegionControl plugin;
	
	public RegionControlBlockListener(RegionControl instance) {
		plugin = instance;
	}
	
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		Block block = event.getBlock();
		plugin.getServer().broadcastMessage("Redstone event on block " + block.getType().name());
	}

}
