package us.axefan.plugin;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.block.BlockRedstoneEvent;

public class RegionControlBlockListener extends org.bukkit.event.block.BlockListener {
	
	public static RegionControl plugin;
	
	public RegionControlBlockListener(RegionControl instance) {
		plugin = instance;
	}
	
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		Block block = event.getBlock();
		if (block.getBlockPower() == 0) return;
		BlockState blockState = block.getState();
		if (blockState instanceof Sign)
		{
		    Sign sign = (Sign)blockState;
		    String[] lines = sign.getLines();
		    if (lines[0].trim().equalsIgnoreCase("[RegionControl]")) {
		    	if (lines[1].trim().equalsIgnoreCase("frame")) {
		    		plugin.frame(null, lines[2]);
		    	}
		    }
		}
	}
	
}
