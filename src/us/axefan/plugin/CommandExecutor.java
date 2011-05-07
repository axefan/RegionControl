package us.axefan.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandExecutor implements org.bukkit.command.CommandExecutor {
	
	private RegionControl plugin;
	
	public CommandExecutor(RegionControl instance) {
		this.plugin = instance;
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!label.toLowerCase().equals("rc")) return false;
		if (args.length < 1)  return false;
		Player player = (Player) sender;
		String action = args[0].toLowerCase();
		if (action.equals("list")) {
			this.plugin.list(player);
			return true;
		}else if (action.equals("info") || action.equals("i")) {
			if (args.length < 2) {
				player.sendMessage("/rc info <name>");
				return true;
			}
			this.plugin.info(player, args[1].trim());
			return true;
		}else if (action.equals("messages") || action.equals("m")) {
			if (args.length < 2) {
				player.sendMessage("/rc messages <name>");
				return true;
			}
			this.plugin.messages(player, args[1].trim());
			return true;
		}else if (action.equals("create") || action.equals("c")) {
			if (args.length < 2) {
				player.sendMessage("/rc create <name>");
				return true;
			}
			this.plugin.create(player, args[1].trim());
			return true;
		}else if (action.equals("edit") || action.equals("e")) {
			if (args.length < 4) {
				player.sendMessage("/rc edit <name> <field> <value>");
				return true;
			}
			String arg3;
			if (args.length == 4) {
				arg3 = args[3];
			}else{
				arg3 = "";
				for (int i=3; i<args.length; i++){
					arg3 += args[i] + " ";
				}
			}
			this.plugin.edit(player, args[1].trim(), args[2].trim(), arg3.trim());
			return true;
		}else if (action.equals("delete") || action.equals("d")) {
			if (args.length < 2) {
				player.sendMessage("/rc delete <name>");
				return true;
			}
			this.plugin.delete(player, args[1].trim());
			return true;
		}else if (action.equals("snap") || action.equals("s")) {
			if (args.length < 2) {
				player.sendMessage("/rc snap <name>");
				return true;
			}
			this.plugin.snap(player, args[1].trim());
			return true;
		}else if (action.equals("deleteFrame") || action.equals("df")) {
			if (args.length < 2) {
				player.sendMessage("/rc deleteFrame <name> [frame]");
				return true;
			}
			if (args.length == 2) {
				plugin.delsnap(player, args[1].trim());
			}else{
				plugin.delsnap(player, args[1].trim(), Integer.parseInt(args[2].trim()));
			}
			return true;
		}else if (action.equals("frame") || action.equals("f")) {
			if (args.length < 2) {
				player.sendMessage("/rc frame <name> [frame]");
				return true;
			}
			if (args.length == 2) {
				plugin.frame(player, args[1].trim());
			}else{
				plugin.frame(player, args[1].trim(), Integer.parseInt(args[2].trim()));
			}
			return true;
		}else if (action.equals("lock") || action.equals("l")) {
			if (args.length < 2) {
				player.sendMessage("/rc lock <name>");
				return true;
			}
			plugin.lock(player, args[1].trim());
			return true;
		}else if (action.equals("unlock") || action.equals("u")) {
			if (args.length < 2) {
				player.sendMessage("/rc unlock <name>");
				return true;
			}
			plugin.unlock(player, args[1].trim());
			return true;
		}else return false;
	}	
}