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
		}else if (action.equals("info")) {
			if (args.length < 2) {
				player.sendMessage("/rc info <name>");
				return true;
			}
			this.plugin.info(player, args[1].trim());
			return true;
		}else if (action.equals("create")) {
			if (args.length < 2) {
				player.sendMessage("/rc create <name>");
				return true;
			}
			this.plugin.create(player, args[1].trim());
			return true;
		}else if (action.equals("update")) {
			if (args.length < 4) {
				player.sendMessage("/rc update <name> <field> <value>");
				return true;
			}
			this.plugin.update(player, args[1].trim(), args[2].trim(), args[3].trim());
			return true;
		}else if (action.equals("delete")) {
			if (args.length < 2) {
				player.sendMessage("/rc delete <name>");
				return true;
			}
			this.plugin.delete(player, args[1].trim());
			return true;
		}else if (action.equals("snap")) {
			if (args.length < 2) {
				player.sendMessage("/rc snap <name>");
				return true;
			}
			this.plugin.snap(player, args[1].trim());
			return true;
		}else if (action.equals("delsnap")) {
			if (args.length < 2) {
				player.sendMessage("/rc delsnap <name> [frame]");
				return true;
			}
			if (args.length == 2) {
				plugin.delsnap(player, args[1].trim());
			}else{
				plugin.delsnap(player, args[1].trim(), Integer.parseInt(args[2].trim()));
			}
			return true;
		}else if (action.equals("frame")) {
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
		}else if (action.equals("lock")) {
			if (args.length < 2) {
				player.sendMessage("/rc lock <name>");
				return true;
			}
			plugin.lock(player, args[1].trim());
			return true;
		}else if (action.equals("unlock")) {
			if (args.length < 2) {
				player.sendMessage("/rc unlock <name>");
				return true;
			}
			plugin.unlock(player, args[1].trim());
			return true;
		}else return false;
	}	
}