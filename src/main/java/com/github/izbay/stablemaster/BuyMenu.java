package com.github.izbay.stablemaster;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.github.izbay.stablemaster.IOManager.Action;
import com.github.izbay.stablemaster.IOManager.CarpetColors;
import com.github.izbay.stablemaster.IOManager.HorseColors;
import com.github.izbay.stablemaster.IOManager.HorsePatterns;
import com.github.izbay.stablemaster.IOManager.Interface;
import com.github.izbay.stablemaster.IOManager.LlamaColors;
import com.github.izbay.stablemaster.IOManager.MountNames;
import com.github.izbay.stablemaster.StableMgr.StableAcct;

import net.citizensnpcs.api.npc.NPC;
	/**
	 * @author izbay Used to design mounts and purchase them.
	 */
public class BuyMenu implements Listener {
		private String name;
		private NPC npc;
		private Player player;

		// As much as I'd rather use enums, I need to use modulus, so these are
		// ints.
		private int type = 0;
		private int health = 0;
		private int jump = 0;
		private int speed = 0;
		private int color = 0;
		private int pattern = 0;
		private int carpetcolor = 0;

		private String[] typeOrder = { "pig", "horse", "mule", "donkey", "llama", "skeleton", "zombie" };
		private int healthmin = 10;
		private int healthmax = 60;
		private int healthrange = healthmax - healthmin + 1;
		private int jumpmin = IOManager.minjump;
		private int jumpmax = IOManager.maxjump;
		private double jumpmod = IOManager.modjump;
		private int jumprange = jumpmax - jumpmin + 1;
		private int speedmin = IOManager.minspeed;
		private int speedmax = IOManager.maxspeed;
		private double speedmod = IOManager.modspeed;
		private int speedrange = speedmax - speedmin + 1;

		private Inventory menu;
		private double cost = IOManager.mountPrice.get("pig");

		/**
		 * Constructor for BuyMenu GUI.
		 * 
		 * @param player
		 *            Player who is viewing the CashMenu.
		 * @param stableAcct
		 *            Corresponding StableAcct.
		 * @param npc
		 *            The NPC that was being spoken to.
		 */
		public BuyMenu(Player player, StableAcct stableAcct, NPC npc) {
			name = IOManager.getString(Interface.shop,
					player.hasPermission("stablemaster.noble"));
			this.npc = npc;
			this.player = player;
			Menu.plugin.getServer().getPluginManager()
					.registerEvents(this, Menu.plugin);
			menu = buildMenu(player);
			if (menu == null) {
				IOManager.msg(player, Action.empty, null);
				killMenu(player);
			} else {
				player.openInventory(menu);
				updateMenu(player);
			}
		}

		private Inventory buildMenu(Player player) {
			Inventory cashMenu = Bukkit.createInventory(player, 27, name);
			cashMenu.setItem(0, IOManager.makeButton(IOManager.Icons.button, Interface.prevType, false));
			cashMenu.setItem(1, IOManager.makeButton(IOManager.Icons.pig, 1, IOManager.mountNames.get(MountNames.pig),
					IOManager.econFormat(player, IOManager.mountPrice.get("pig"))));
			cashMenu.setItem(2, IOManager.makeButton(IOManager.Icons.button, Interface.nextType, false));
			cashMenu.setItem(8, IOManager.makeButton(IOManager.Icons.exitBuy, Interface.shopexit, false));
			
			if (player.hasPermission("stablemaster.noble.discount")) {
				double oldcost = cost;
				cost *= IOManager.mountPrice.get("noble");
				cashMenu.setItem(26, IOManager.makeButton(IOManager.Icons.buy, Interface.shoppurchase, true, 
						ChatColor.translateAlternateColorCodes('&', ("&9" + oldcost + " (" + IOManager.econFormat(player, cost) + "&9)"))));
			} else {
				cashMenu.setItem(26, IOManager.makeButton(IOManager.Icons.buy, Interface.shoppurchase, false,
						IOManager.econFormat(player, cost)));
			}

			// Loop to the first available mount.
			while (type < typeOrder.length && IOManager.mountPrice.get(typeOrder[type]) < 0) {
				type = (type + 1);
			}
			// Check if nothing's available and close if that's the case.
			if (type >= typeOrder.length)
				return null;

			return cashMenu;
		}

		private void updateMenu(Player player) {

			// Type (and subsequently which buttons are displayed)
			switch (type) {
			case 0:
				menu.setItem(1, IOManager.makeButton(IOManager.Icons.pig, 1, IOManager.mountNames.get(MountNames.pig), 
						IOManager.econFormat(player, IOManager.mountPrice.get("pig"))));

				// Remove all extra buttons.
				menu.setItem(4, IOManager.makeButton(IOManager.Icons.button, Interface.prevHealth, false));
				menu.setItem(9,
						IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(13, IOManager.makeButton(IOManager.Icons.button, Interface.prevSpeed, false));
				menu.setItem(18,
						IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(22,
						IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(6, IOManager.makeButton(IOManager.Icons.button, Interface.nextHealth, false));
				menu.setItem(11,
						IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(15, IOManager.makeButton(IOManager.Icons.button, Interface.nextSpeed, false));
				menu.setItem(20,
						IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(24,
						IOManager.makeButton(IOManager.Icons.nil, "", ""));

				cost = IOManager.mountPrice.get("pig");
				break;

			case 1:
				menu.setItem(1, IOManager.makeButton(IOManager.Icons.horse, 1, IOManager.mountNames.get(MountNames.horse),
						IOManager.econFormat(player, IOManager.mountPrice.get("horse"))));

				// Draw all buttons in.
				menu.setItem(4, IOManager.makeButton(IOManager.Icons.button, Interface.prevHealth, false));
				menu.setItem(9, IOManager.makeButton(IOManager.Icons.button, Interface.prevJump, false));
				menu.setItem(13, IOManager.makeButton(IOManager.Icons.button, Interface.prevSpeed, false));
				menu.setItem(18, IOManager.makeButton(IOManager.Icons.button, Interface.prevColor, false));
				menu.setItem(22, IOManager.makeButton(IOManager.Icons.button, Interface.prevPattern, false));
				menu.setItem(6, IOManager.makeButton(IOManager.Icons.button, Interface.nextHealth, false));
				menu.setItem(11, IOManager.makeButton(IOManager.Icons.button, Interface.nextJump, false));
				menu.setItem(15, IOManager.makeButton(IOManager.Icons.button, Interface.nextSpeed, false));
				menu.setItem(20, IOManager.makeButton(IOManager.Icons.button, Interface.nextColor, false));
				menu.setItem(24, IOManager.makeButton(IOManager.Icons.button, Interface.nextPattern, false));

				cost = IOManager.mountPrice.get("horse");
				break;
			case 2:
				menu.setItem(1, IOManager.makeButton(IOManager.Icons.mule, 1, IOManager.mountNames.get(MountNames.mule),
						IOManager.econFormat(player, IOManager.mountPrice.get("mule"))));

				// Draw in only the used buttons.
				menu.setItem(4, IOManager.makeButton(IOManager.Icons.button, Interface.prevHealth, false));
				menu.setItem(9, IOManager.makeButton(IOManager.Icons.button, Interface.prevJump, false));
				menu.setItem(13, IOManager.makeButton(IOManager.Icons.button, Interface.prevSpeed, false));
				menu.setItem(18, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(22, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(6, IOManager.makeButton(IOManager.Icons.button, Interface.nextHealth, false));
				menu.setItem(11, IOManager.makeButton(IOManager.Icons.button, Interface.nextJump, false));
				menu.setItem(15, IOManager.makeButton(IOManager.Icons.button, Interface.nextSpeed, false));
				menu.setItem(20, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(24, IOManager.makeButton(IOManager.Icons.nil, "", ""));

				cost = IOManager.mountPrice.get("mule");
				break;

			case 3:
				menu.setItem(1, IOManager.makeButton(
						IOManager.Icons.donkey,
						1,
						IOManager.mountNames.get(MountNames.donkey),
						IOManager.econFormat(player,
								IOManager.mountPrice.get("donkey"))));

				// Draw in only the used buttons.
				menu.setItem(4, IOManager.makeButton(IOManager.Icons.button, Interface.prevHealth, false));
				menu.setItem(9, IOManager.makeButton(IOManager.Icons.button, Interface.prevJump, false));
				menu.setItem(13, IOManager.makeButton(IOManager.Icons.button, Interface.prevSpeed, false));
				menu.setItem(18, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(22, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(6, IOManager.makeButton(IOManager.Icons.button, Interface.nextHealth, false));
				menu.setItem(11, IOManager.makeButton(IOManager.Icons.button, Interface.nextJump, false));
				menu.setItem(15, IOManager.makeButton(IOManager.Icons.button, Interface.nextSpeed, false));
				menu.setItem(20, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(24, IOManager.makeButton(IOManager.Icons.nil, "", ""));

				cost = IOManager.mountPrice.get("donkey");
				break;
			case 4:
				menu.setItem(1, IOManager.makeButton(IOManager.Icons.llama, 1, IOManager.mountNames.get(MountNames.llama),
						IOManager.econFormat(player, IOManager.mountPrice.get("llama"))));

				// Draw all buttons in.
				menu.setItem(4, IOManager.makeButton(IOManager.Icons.button, Interface.prevHealth, false));
				menu.setItem(9, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(13, IOManager.makeButton(IOManager.Icons.button, Interface.prevSpeed, false));
				menu.setItem(18, IOManager.makeButton(IOManager.Icons.button, Interface.prevColor, false));
				menu.setItem(22, IOManager.makeButton(IOManager.Icons.button, Interface.prevCarpetColor, false));
				menu.setItem(6, IOManager.makeButton(IOManager.Icons.button, Interface.nextHealth, false));
				menu.setItem(11, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(15, IOManager.makeButton(IOManager.Icons.button, Interface.nextSpeed, false));
				menu.setItem(20, IOManager.makeButton(IOManager.Icons.button, Interface.nextColor, false));
				menu.setItem(24, IOManager.makeButton(IOManager.Icons.button, Interface.nextCarpetColor, false));

				cost = IOManager.mountPrice.get("llama");
				break;
			case 5:
				menu.setItem(1, IOManager.makeButton(IOManager.Icons.skeleton, 1, IOManager.mountNames.get(MountNames.skeleton),
						IOManager.econFormat(player, IOManager.mountPrice.get("skeleton"))));

				// Draw in only the used buttons.
				menu.setItem(4, IOManager.makeButton(IOManager.Icons.button, Interface.prevHealth, false));
				menu.setItem(9, IOManager.makeButton(IOManager.Icons.button, Interface.prevJump, false));
				menu.setItem(13, IOManager.makeButton(IOManager.Icons.button, Interface.prevSpeed, false));
				menu.setItem(18, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(22, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(6, IOManager.makeButton(IOManager.Icons.button, Interface.nextHealth, false));
				menu.setItem(11, IOManager.makeButton(IOManager.Icons.button, Interface.nextJump, false));
				menu.setItem(15, IOManager.makeButton(IOManager.Icons.button, Interface.nextSpeed, false));
				menu.setItem(20, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(24, IOManager.makeButton(IOManager.Icons.nil, "", ""));

				cost = IOManager.mountPrice.get("skeleton");
				break;

			case 6:
				menu.setItem(1, IOManager.makeButton(IOManager.Icons.zombie, 1, IOManager.mountNames.get(MountNames.zombie),
						IOManager.econFormat(player, IOManager.mountPrice.get("zombie"))));

				// Draw in only the used buttons.
				menu.setItem(4, IOManager.makeButton(IOManager.Icons.button, Interface.prevHealth, false));
				menu.setItem(9, IOManager.makeButton(IOManager.Icons.button, Interface.prevJump, false));
				menu.setItem(13, IOManager.makeButton(IOManager.Icons.button, Interface.prevSpeed, false));
				menu.setItem(18, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(22, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(6, IOManager.makeButton(IOManager.Icons.button, Interface.nextHealth, false));
				menu.setItem(11, IOManager.makeButton(IOManager.Icons.button, Interface.nextJump, false));
				menu.setItem(15, IOManager.makeButton(IOManager.Icons.button, Interface.nextSpeed, false));
				menu.setItem(20, IOManager.makeButton(IOManager.Icons.nil, "", ""));
				menu.setItem(24, IOManager.makeButton(IOManager.Icons.nil, "", ""));

				cost = IOManager.mountPrice.get("zombie");
				break;
			}

			// Health
			if (health == 0) {
				menu.setItem(5, IOManager.makeButton(IOManager.Icons.health, healthmin, ChatColor.translateAlternateColorCodes('&', ("&a" + healthmin)), ""));
			} else {
				menu.setItem(5, IOManager.makeButton(
						IOManager.Icons.health, health + healthmin, ChatColor.translateAlternateColorCodes('&', ("&a" + (health + healthmin))),
						IOManager.econFormat(player, IOManager.mountPrice.get("health") * (health))));
				cost += (IOManager.mountPrice.get("health") * health);
			}

			// Jump
			if (type == 0 || type == 4) {
				menu.setItem(10, IOManager.makeButton(IOManager.Icons.nil, "", ""));
			} else {
				if (jump == 0) {
					menu.setItem(10, IOManager.makeButton(
							IOManager.Icons.jump, 1, ChatColor.translateAlternateColorCodes('&', ("&a" + ((double) jumpmin) / 100)), ""));
				} else {
					menu.setItem(10, IOManager.makeButton(
							IOManager.Icons.jump, 1, ChatColor.translateAlternateColorCodes('&', ("&a" + ((double) (jump + jumpmin)) / 100)),
							IOManager.econFormat(player, IOManager.mountPrice.get("jump") * (jump))));
					cost += (IOManager.mountPrice.get("jump") * jump);
				}
			}

			// Speed
			if (speed == 0) {
				menu.setItem(14, IOManager.makeButton(IOManager.Icons.speed, 1, ChatColor.translateAlternateColorCodes('&', ("&a" + ((double) speedmin) / 100)), ""));
			} else {
				menu.setItem(14, 
						IOManager .makeButton(IOManager.Icons.speed, 1, ChatColor.translateAlternateColorCodes('&', ("&a" + ((double) (speed + speedmin)) / 100)),
						IOManager.econFormat(player, IOManager.mountPrice.get("speed") * (speed))));
				cost += (IOManager.mountPrice.get("speed") * speed);
			}

			// Color
			if (type == 1) {
				switch (color) {
				case 0:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.white, 1,
							IOManager.horseColorsMap.get(HorseColors.white), ""));
					break;
				case 1:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.creamy, 2,
							IOManager.horseColorsMap.get(HorseColors.creamy), ""));
					break;
				case 2:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.chestnut, 3,
							IOManager.horseColorsMap.get(HorseColors.chestnut), ""));
					break;
				case 3:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.brown, 4,
							IOManager.horseColorsMap.get(HorseColors.brown), ""));
					break;
				case 4:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.black, 5,
							IOManager.horseColorsMap.get(HorseColors.black), ""));
					break;
				case 5:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.gray, 6,
							IOManager.horseColorsMap.get(HorseColors.gray), ""));
					break;
				case 6:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.dark_brown, 7,
							IOManager.horseColorsMap.get(HorseColors.dark_brown), ""));
					break;
				}
			}else if (type == 4) {
				switch (color) {
				case 0:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.creamy, 1,
							IOManager.llamaColorsMap.get(LlamaColors.creamy), ""));
					break;			
				case 1:
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.white, 2,
							IOManager.llamaColorsMap.get(LlamaColors.white), ""));
					break;
					
				case 2:
					menu.setItem(19, IOManager.makeButton(IOManager.Icons.brown,
							3, IOManager.llamaColorsMap.get(LlamaColors.brown), ""));
					break;
				case 3:			
					menu.setItem(19, IOManager.makeButton(
							IOManager.Icons.gray, 4,
							IOManager.llamaColorsMap.get(LlamaColors.gray), ""));
					break;
				}
			}else {
				menu.setItem(19, IOManager.makeButton(IOManager.Icons.nil, "", ""));
			}
			

			// Style
			if (type == 1) {
				switch (pattern) {
				case 0:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.plain, 1,
							IOManager.horsePatternsMap.get(HorsePatterns.plain), ""));
					break;
				case 1:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.white, 2,
							IOManager.horsePatternsMap.get(HorsePatterns.white), ""));
					break;
				case 2:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.whitefield, 3,
							IOManager.horsePatternsMap.get(HorsePatterns.whitefield), ""));
					break;
				case 3:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.white_dots, 4,
							IOManager.horsePatternsMap.get(HorsePatterns.white_dots), ""));
					break;
				case 4:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.black_dots, 5,
							IOManager.horsePatternsMap.get(HorsePatterns.black_dots), ""));
					break;
				}
			} else if (type == 4){
				switch (carpetcolor) {
				case 0:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.nocarpet, 1,
							IOManager.carpetColorsMap.get(CarpetColors.nocarpet), ""));
					break;
				case 1:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.black, 2,
							IOManager.carpetColorsMap.get(CarpetColors.black),
							IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
					
					cost += (IOManager.mountPrice.get("carpet"));
					break;
				case 2:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.red, 3,
							IOManager.carpetColorsMap.get(CarpetColors.red),
							IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
					cost += (IOManager.mountPrice.get("carpet"));
					break;
				case 3:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.green, 4,
							IOManager.carpetColorsMap.get(CarpetColors.green),
							IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
					cost += (IOManager.mountPrice.get("carpet"));
					break;
				case 4:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.brown, 5,
							IOManager.carpetColorsMap.get(CarpetColors.brown),
							IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
					cost += (IOManager.mountPrice.get("carpet"));
					break;
				case 5:
					menu.setItem(23, IOManager.makeButton(
							IOManager.Icons.blue, 6,
							IOManager.carpetColorsMap.get(CarpetColors.blue),
							IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
					cost += (IOManager.mountPrice.get("carpet"));
					break;
			    case 6:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.purple, 7,
						IOManager.carpetColorsMap.get(CarpetColors.purple),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 7:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.cyan, 8,
						IOManager.carpetColorsMap.get(CarpetColors.cyan),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 8:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.silver, 9,
						IOManager.carpetColorsMap.get(CarpetColors.silver),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 9:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.gray, 10,
						IOManager.carpetColorsMap.get(CarpetColors.gray),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 10:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.pink, 11,
						IOManager.carpetColorsMap.get(CarpetColors.pink),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 11:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.lime, 12,
						IOManager.carpetColorsMap.get(CarpetColors.lime),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 12:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.yellow, 13,
						IOManager.carpetColorsMap.get(CarpetColors.yellow),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 13:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.light_blue, 14,
						IOManager.carpetColorsMap.get(CarpetColors.light_blue),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 14:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.magenta, 15,
						IOManager.carpetColorsMap.get(CarpetColors.magenta),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 15:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.orange, 16,
						IOManager.carpetColorsMap.get(CarpetColors.orange),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
			    case 16:
			    	menu.setItem(23, IOManager.makeButton(
						IOManager.Icons.white, 17,
						IOManager.carpetColorsMap.get(CarpetColors.white),
						IOManager.econFormat(player, IOManager.mountPrice.get("carpet"))));
			    	cost += (IOManager.mountPrice.get("carpet"));
			    	break;
				}
			} else {
				menu.setItem(23,
						IOManager.makeButton(IOManager.Icons.nil, "", ""));
			}

			if (player.hasPermission("stablemaster.noble.discount")) {
				double oldcost = cost;
				cost *= IOManager.mountPrice.get("noble");
				menu.setItem(26, IOManager.makeButton(
						IOManager.Icons.pay, Interface.shoppurchase, true,
						ChatColor.translateAlternateColorCodes('&', ("&9" + oldcost + " (" + IOManager.econFormat(player, cost) + "&9)"))));
			} else {
				menu.setItem(26, IOManager.makeButton(
						IOManager.Icons.pay,Interface.shoppurchase, false,
						IOManager.econFormat(player, cost)));
			}
		}

		/**
		 * Self destructs this menu and removes player from npcMap.
		 * 
		 * @param player
		 *            Player who is exiting this menu.
		 */
		private void killMenu(final Player player) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(Menu.plugin,
					new Runnable() {
						public void run() {
							Menu.openRoot(player, Menu.stableMgr, Menu.plugin,Menu.npcMap.get(player));
						}
					}, 1);
			HandlerList.unregisterAll(this);
		}

		@SuppressWarnings("deprecation")
		@EventHandler
		void onInventoryClick(InventoryClickEvent event) {
			boolean validSlot = (event.getRawSlot() <= 26 && event.getRawSlot() >= 0);
			
			if (event.getInventory().getTitle().equals(name) && validSlot) {
				event.setCancelled(true);
				boolean thisPlayer = (Player) event.getWhoClicked() == this.player;

				if (thisPlayer) {
					final Player player = (Player) event.getWhoClicked();

					switch (event.getRawSlot()) {

					// Previous mount type
					case 0:
						do {
							type = ((((type - 1) % 7) + 7) % 7);
						} while (IOManager.mountPrice.get(typeOrder[type]) < 0);
						
						if (type == 1) {
							color = 0;
							pattern = 0;
						}
						if(type == 4){
							color = 0;
							carpetcolor = 0;
						}
						if (type == 0) {
							jump = 0;
						}
						break;
						
					// Next mount type
					case 2:
						do {
							type = (type + 1) % 7;
						} while (IOManager.mountPrice.get(typeOrder[type]) < 0);
						if (type == 1) {
							color = 0;
							pattern = 0;
						}
						if(type == 4){
							color = 0;
						}
						if (type == 0) {					
							jump = 0;
						}
						break;
						
					// Previous health
					case 4:
						health = ((((health - 1) % healthrange) + healthrange) % healthrange);
						break;
						
					// Next health
					case 6:
						health = (health + 1) % healthrange;
						break;
						
					// Exit
					case 8:
						killMenu(player);
						break;
						
					// Previous jump
					case 9:
						if (type != 0) {
							jump = ((((jump - 1) % jumprange) + jumprange) % jumprange);
						}
						break;
					// Next jump
						
					case 11:
						if (type != 0) {
							jump = (jump + 1) % jumprange;
						}
						break;
						
					// Previous speed
					case 13:
						speed = ((((speed - 1) % speedrange) + speedrange) % speedrange);
						break;
						
					// Next speed
					case 15:
						speed = (speed + 1) % speedrange;
						break;
						
					// Previous color
					case 18:
						if (type == 1) {
							color = ((((color - 1) % 7) + 7) % 7);
						}
						if(type == 4){
							color = ((((color - 1) % 4) + 4) % 4);
						}
						break;
						
					// Next color
					case 20:
						if (type == 1) {
							color = (color + 1) % 7;
						}
						if(type == 4){
							color = (color + 1) % 4;
						}
						break;
					// Previous style/carpet color
					case 22:
						if (type == 1) {
							pattern = ((((pattern - 1) % 5) + 5) % 5);
						}
						if (type == 4) {
							carpetcolor = ((((carpetcolor - 1) % 17) + 17) % 17);
						}
						break;
						
					// Next style/carpet color
					case 24:
						if (type == 1) {
							pattern = (pattern + 1) % 5;
						}
						if (type == 4) {
							carpetcolor = (carpetcolor + 1) % 17;
						}
						break;
						
					// Purchase
					case 26:
						if (IOManager.charge(npc, player, cost)) {
							Mount thisMount = null;
							// Deconstruct those int values to build the mount the player selected!
							switch (type) {
							
							// Pig
							case 0:
								thisMount = new Mount(
										IOManager.getSomeName(),
										EntityType.PIG,
										System.currentTimeMillis());
								thisMount.setSpeed((((double) speed + speedmin) / 100) * speedmod);
								thisMount.setHealth((double) health + healthmin);
								break;

							// Horse
							case 1:
								thisMount = new Mount(
										IOManager.getSomeName(),
										EntityType.HORSE,
										System.currentTimeMillis());

								// Set the color/pattern.
								thisMount.setSpeed((((double) speed + speedmin) / 100) * speedmod);
								thisMount.setJumpstr((((double) jump + jumpmin) / 100) * jumpmod);
								thisMount.setHealth((double) health + healthmin);
								thisMount.setSaddle(new ItemStack(Material.SADDLE, 1));
								thisMount.setColor(Horse.Color.values()[color]);
								thisMount.setStyle(Horse.Style.values()[pattern]);
								break;
							// Mule
							case 2:
								thisMount = new Mount(
										IOManager.getSomeName(),
										EntityType.MULE,
										System.currentTimeMillis());
								thisMount.setSpeed((((double) speed + speedmin) / 100) * speedmod);
								thisMount.setJumpstr((((double) jump + jumpmin) / 100) * jumpmod);
								thisMount.setHealth((double) health + healthmin);
								thisMount.setChest(false);
								thisMount.setSaddle(new ItemStack(Material.SADDLE, 1));
								break;

							// Donkey
							case 3:
								thisMount = new Mount(
										IOManager.getSomeName(),
										EntityType.DONKEY,
										System.currentTimeMillis());
								thisMount.setSpeed((((double) speed + speedmin) / 100) * speedmod);
								thisMount.setJumpstr((((double) jump + jumpmin) / 100) * jumpmod);
								thisMount.setHealth((double) health + healthmin);
								thisMount.setChest(false);
								thisMount.setSaddle(new ItemStack(Material.SADDLE, 1));
								break;
                            // Llama
							case 4:
								thisMount = new Mount(
										IOManager.getSomeName(),
										EntityType.LLAMA,
										System.currentTimeMillis());
										// Set the color/pattern.
										thisMount.setSpeed((((double) speed + speedmin) / 100) * speedmod);
										thisMount.setHealth((double) health + healthmin);
										//fix this and make the carpet color selectable in Gui
										if(carpetcolor!=0){
											thisMount.setCarpet(new ItemStack(Material.CARPET, 1, org.bukkit.DyeColor.values()[carpetcolor-1].getDyeData()));
										}								
										thisMount.setLlamaColor(org.bukkit.entity.Llama.Color.values()[color]);
								break;
							// Skeleton
							case 5:
								thisMount = new Mount(
										IOManager.getSomeName(),
										EntityType.SKELETON_HORSE,
										System.currentTimeMillis());
								thisMount.setSpeed((((double) speed + speedmin) / 100) * speedmod);
								thisMount.setJumpstr((((double) jump + jumpmin) / 100) * jumpmod);
								thisMount.setHealth((double) health + healthmin);
								thisMount.setSaddle(new ItemStack(Material.SADDLE, 1));
								break;

							// Zombie
							case 6:
								thisMount = new Mount(
										IOManager.getSomeName(),
										EntityType.ZOMBIE_HORSE,
										System.currentTimeMillis());
								thisMount.setSpeed((((double) speed + speedmin) / 100) * speedmod);
								thisMount.setJumpstr((((double) jump + jumpmin) / 100) * jumpmod);
								thisMount.setHealth((double) health + healthmin);
								thisMount.setSaddle(new ItemStack(Material.SADDLE, 1));
								break;
							}
							//fix this for pigs/zombie horses, etc
							// Now set the speed, jump, health, chest, and
							// saddle if it's a horse type.

							Menu.spawnMount(player, thisMount, npc, true);

							Menu.npcMap.remove(player);
							HandlerList.unregisterAll(this);
							Menu.buyMap.remove(player);
							player.closeInventory();
							break;
						}
						break;

					}
					// refresh
					updateMenu(player);
				}
			}
		}

	@EventHandler
	void onInventoryClose(InventoryCloseEvent event) {
		if (event.getInventory().getTitle().equals(name) && event.getPlayer().equals(this.player)) {
			Player player = (Player) event.getPlayer();
			Menu.npcMap.remove(player);
			HandlerList.unregisterAll(this);
			Menu.buyMap.remove(player);
		}
	}
}
