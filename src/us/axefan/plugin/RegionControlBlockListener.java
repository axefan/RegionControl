package us.axefan.plugin;

import org.bukkit.block.Sign;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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
		if (blockState instanceof Sign) {
		    Sign sign = (Sign)blockState;
		    String[] lines = sign.getLines();
		    String label = lines[0].trim();
		    if (!label.equalsIgnoreCase("[rc]")) return;
		    String[] args = new String[lines.length-1];
		    System.arraycopy(lines, 1, args, 0, lines.length-1);
		    args = ArrayUtils.trim(args);
		    if (plugin.verbose) plugin.sendMessage("Sending redstone command: " + label + " " + ArrayUtils.join(args, " "));
		    plugin.getCommand("rc").getExecutor().onCommand(null, null, label, args);
		}
	}
	
	
	
}
