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
		label = label.toLowerCase();
		if(!label.equals("rc") && !label.equals("[rc]")) return false;
		if (args.length < 1)  return false;
		String action = args[0].toLowerCase();
		if (action.equals("list")) {
			this.plugin.list(sender);
			return true;
		}else if (action.equals("version")) {
			// TODO: Add version command.
			return true;
		}else if (action.equals("help")) {
			// TODO: Add help command.
			return true;
		}else if (action.equals("info") || action.equals("i")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc info <regionName>");
				return true;
			}
			this.plugin.info(sender, args[1].trim());
			return true;
		}
		else if (action.equals("messages") || action.equals("m")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc messages <regionName>");
				return true;
			}
			this.plugin.messages(sender, args[1].trim());
			return true;
		}else if (action.equals("create") || action.equals("c")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc create <regionName>");
				return true;
			}
			if (!(sender instanceof Player)){
				plugin.sendMessage(sender, Messages.PlayerCommandError);
				return true;
			}
			if (args.length > 2) {
				plugin.sendMessage(sender, Messages.RegionNameError);
				return true;				
			}
			this.plugin.create((Player)sender, args[1].trim());
			return true;
		}else if (action.equals("edit") || action.equals("e")) {
			if (args.length < 4) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc edit <regionName> <settingName> <newValue>");
				return true;
			}
			this.plugin.edit(sender, args[1].trim(), args[2].trim(), this.concatArgs(args, 3));
			return true;
		}else if (action.equals("set")) {
			if (!(sender instanceof Player)){
				plugin.sendMessage(sender, Messages.PlayerCommandError);
				return true;
			}
			if (args.length < 3) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc set <regionName> <locationName>");
				return true;
			}
			this.plugin.set(sender, args[1].trim(), args[2].trim());
			return true;
		}else if (action.equals("delete") || action.equals("d")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc delete <regionName>");
				return true;
			}
			this.plugin.delete(sender, args[1].trim());
			return true;
		}else if (action.equals("snap") || action.equals("s")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc snap <regionName>");
				return true;
			}
			this.plugin.snap(sender, args[1].trim());
			return true;
		}else if (action.equals("deleteSnapshot") || action.equals("ds")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc deleteSnapshot <regionName> [frameNumber]");
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
				plugin.sendMessage(sender, ChatColor.RED + "/rc frame <regionName> [frameNumber]");
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
				plugin.sendMessage(sender, ChatColor.RED + "/rc lock <regionName>");
				return true;
			}
			plugin.lock(sender, args[1].trim());
			return true;
		}else if (action.equals("unlock") || action.equals("u")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc unlock <regionName>");
				return true;
			}
			plugin.unlock(sender, args[1].trim());
			return true;
		}else if (action.equals("fail")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc fail <regionName>");
				return true;
			}
			plugin.fail(sender, args[1].trim());
			return true;
		}else if (action.equals("teleport") || action.equals("tp")) {
			if (args.length < 2) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc teleport <regionName>");
				return true;
			}
			if (!(sender instanceof Player)){
				plugin.sendMessage(sender, Messages.PlayerCommandError);
				return true;
			}
			plugin.teleport((Player)sender, args[1].trim());
			return true;
		}else if (action.equals("loadTemplate") || action.equals("lt")) {
			if (args.length < 3) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc loadTemplate <regionName> <templateName>");
				return true;
			}
			plugin.loadTemplate(sender, args[1].trim(), args[2].trim());
			return true;
		}else if (action.equals("saveTemplate") || action.equals("st")) {
			if (args.length < 3) {
				plugin.sendMessage(sender, ChatColor.RED + "/rc saveTemplate <regionName> <templateName>");
				return true;
			}
			if (args.length > 3) {
				plugin.sendMessage(sender, Messages.TemplateNameError);
				return true;				
			}
			plugin.saveTemplate(sender, args[1].trim(), args[2].trim());
			return true;
		}
		return false;
	}
	
	/*
	 * Concatenates arguments into a space-delimited string.
	 */
	private String concatArgs(String[] args, int index){
		if (args.length == index + 1) return args[index];
		String retval = args[index];
		for (int i=index+1; i<args.length; i++){
			retval += " " + args[i];
		}
		return retval;
	}
}