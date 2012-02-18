/*
 The MIT License

 Copyright (c) 2012 Zloteanu Nichita (ZNickq) and Andre Mohren (IceReaper)

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

package net.morematerials.morematerials.manager;

import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;



public class Utils {

	public boolean hasPermission(CommandSender sender, String perm, boolean verbose) {
		// Allow console
		if (!(sender instanceof Player)) {
			return true;
			// Or players with this permission
		} else if (sender.hasPermission("morematerials.*")) {
			return true;
		} else if (((Player) sender).hasPermission(perm)) {
			return true;
		}
		if (verbose) {
			sender.sendMessage(getMessage("You do not have permission to do that! You need " + perm + "!", Level.SEVERE));
		}
		return false;
	}
	
	// Generalize all console or chat output!
	public String getMessage(String logMessage) {
		return this.getMessage(logMessage, Level.INFO);
	}
	
	public String getMessage(String logMessage, Level level) {
		if (level == Level.WARNING) {
			return ChatColor.GREEN + "[" + "MoreMaterials" + "] " + ChatColor.YELLOW + logMessage;
		} else if (level == Level.SEVERE) {
			return ChatColor.GREEN + "[" + "MoreMaterials" + "] " + ChatColor.RED + logMessage;
		}
		return ChatColor.GREEN + "[" + "MoreMaterials" + "] " + ChatColor.WHITE + logMessage;
	}
	
	public void log(String logMessage) {
		this.log(logMessage, Level.INFO);
	}
	
	public void log(String logMessage, Level level) {
		if (level == Level.WARNING) {
			//TODO add console text color yellow
			System.out.println("[" + "MoreMaterials" + "] Warning: " + logMessage);
		} else if (level == Level.SEVERE) {
			//TODO add console text color red
			System.out.println("[" + "MoreMaterials" + "] ERROR: " + logMessage);
		} else {
			//TODO add console text color normal
			System.out.println("[" + "MoreMaterials" + "] " + logMessage);
		}
	}
	
}
