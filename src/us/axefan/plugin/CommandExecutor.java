package us.axefan.plugin;

import org.bukkit.ChatColor;
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
		String action = args[0].toLowerCase();
		if (action.equals("list")) {
			this.plugin.list(sender);
			return true;
		}else if (action.equals("info") || action.equals("i")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc info <name>");
				return true;
			}
			this.plugin.info(sender, args[1].trim());
			return true;
		}
		else if (action.equals("messages") || action.equals("m")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc messages <name>");
				return true;
			}
			this.plugin.messages(sender, args[1].trim());
			return true;
		}else if (action.equals("create") || action.equals("c")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc create <name>");
				return true;
			}
			if (!(sender instanceof Player)){
				sender.sendMessage(Messages.PlayerCommandError);
				return true;
			}
			this.plugin.create((Player)sender, args[1].trim());
			return true;
		}else if (action.equals("edit") || action.equals("e")) {
			if (args.length < 4) {
				sender.sendMessage(ChatColor.RED + "/rc edit <name> <field> <value>");
				return true;
			}
			this.plugin.edit(sender, args[1].trim(), args[2].trim(), this.concatArgs(args, 3));
			return true;
		}else if (action.equals("delete") || action.equals("d")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc delete <name>");
				return true;
			}
			this.plugin.delete(sender, args[1].trim());
			return true;
		}else if (action.equals("snap") || action.equals("s")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc snap <name>");
				return true;
			}
			this.plugin.snap(sender, args[1].trim());
			return true;
		}else if (action.equals("deleteSnapshot") || action.equals("ds")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc deleteSnapshot <name> [frame]");
				return true;
			}
			if (args.length == 2) {
				plugin.delsnap(sender, args[1].trim());
			}else{
				plugin.delsnap(sender, args[1].trim(), Integer.parseInt(args[2].trim()));
			}
			return true;
		}else if (action.equals("frame") || action.equals("f")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc frame <name> [frame]");
				return true;
			}
			if (args.length == 2) {
				plugin.frame(sender, args[1].trim());
			}else{
				plugin.frame(sender, args[1].trim(), Integer.parseInt(args[2].trim()));
			}
			return true;
		}else if (action.equals("lock") || action.equals("l")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc lock <name>");
				return true;
			}
			plugin.lock(sender, args[1].trim());
			return true;
		}else if (action.equals("unlock") || action.equals("u")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc unlock <name>");
				return true;
			}
			plugin.unlock(sender, args[1].trim());
			return true;
		}else if (action.equals("setSpawnLocation") || action.equals("ssl")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc setSpawnLocation <name>");
				return true;
			}
			if (!(sender instanceof Player)){
				sender.sendMessage(Messages.PlayerCommandError);
				return true;
			}
			plugin.setSpawnLocation((Player)sender, args[1].trim());
			return true;
		}else if (action.equals("teleport") || action.equals("tp")) {
			if (args.length < 2) {
				sender.sendMessage(ChatColor.RED + "/rc teleport <name>");
				return true;
			}
			if (!(sender instanceof Player)){
				sender.sendMessage(Messages.PlayerCommandError);
				return true;
			}
			plugin.teleport((Player)sender, args[1].trim());
			return true;
		}
		return false;
	}
	
	/*
	 * Concatenates arguments into a space-delimited string.
	 */
	private String concatArgs(String[] args, int index){
		if (args.length == index + 1) return args[index].trim();
		String retval = "";
		for (int i=index; i<args.length; i++){
			retval += args[i] + " ";
		}
		return retval.trim();
	}
}