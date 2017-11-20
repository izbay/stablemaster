package com.github.izbay.stablemaster;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import plugin.Nogtail.nHorses.*;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Mule;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Llama;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import com.github.izbay.stablemaster.IOManager.Action;
import com.github.izbay.stablemaster.IOManager.Interface;
import com.github.izbay.stablemaster.Mount;
import com.github.izbay.stablemaster.StableMgr.StableAcct;

/**
 * @author izbay The bulk of stablemaster logic is handled here. By using
 *         inventory to build an interface, navigation of interactions is more
 *         intuitive.
 */
public class Menu implements Listener {
	// Who is looking at the stable menu.
	public static HashMap<Player, PlayerMenu> menuMap = new HashMap<Player, PlayerMenu>();
	// Who is looking at the cash menu.
	public static HashMap<Player, CashMenu> cashMap = new HashMap<Player, CashMenu>();
	// Who is looking at the buy menu.
	public static HashMap<Player, BuyMenu> buyMap = new HashMap<Player, BuyMenu>();
	// For every player viewing a menu, which stablemaster did they speak to?
	public static Map<Player, NPC> npcMap = new HashMap<Player, NPC>();
	// stableMgr imported from the trait. All stable data for every player.
	public static Map<String, StableMgr.StableAcct> stableMgr;

	public static Plugin plugin;
	public static boolean hasnHorses; // Will be used for future compatability.
	private static Menu menu;
	private static Inventory rootMenu;
	private static Inventory nobleMenu;

	// Enforce static class with private constructor
	private Menu() {
		Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);
		
		rootMenu.setItem(0, IOManager.makeButton(IOManager.Icons.pickup, Interface.pickup, false));
		rootMenu.setItem(1, IOManager.makeButton(IOManager.Icons.dropoff, Interface.dropoff, false,
				IOManager.econFormat(null, IOManager.Traits.stable.getPriceInit())));
		rootMenu.setItem(2,IOManager.makeButton(IOManager.Icons.pay, Interface.pay, false));
		rootMenu.setItem(3, IOManager.makeButton(IOManager.Icons.purchase, Interface.purchase, false));
		rootMenu.setItem(8, IOManager.makeButton(IOManager.Icons.exit, Interface.exit, false));

		nobleMenu.setItem(0, IOManager.makeButton(IOManager.Icons.pickup, Interface.pickup, true));
		nobleMenu.setItem(1, IOManager.makeButton(IOManager.Icons.dropoff, Interface.dropoff, true,
				ChatColor.translateAlternateColorCodes('&', ("&9" + IOManager.econFormat(null,IOManager.Traits.stable.getPriceInit()) + " ("+ IOManager.econFormat(null, 0) + "&9)"))));
		nobleMenu.setItem(2, IOManager.makeButton(IOManager.Icons.pay, Interface.pay, true));
		nobleMenu.setItem(3, IOManager.makeButton(IOManager.Icons.purchase, Interface.purchase, true));
		nobleMenu.setItem(8, IOManager.makeButton(IOManager.Icons.exit, Interface.exit, true));
	}
	
	/**
	 * 
	 * @param player
	 *            Which player is opening the root menu.
	 * @param stablemgr
	 *            StableMgr map from the Stablemaster Trait (all stable data).
	 * @param sm
	 *            Instance of StableMgr class (not to be confused with the map
	 *            of data).
	 * @param npc
	 *            Which npc the player spoke to to open this menu.
	 */
	public static void openRoot(Player player, Map<String, StableMgr.StableAcct> stablemgr, Plugin sm, NPC npc) {
		stableMgr = stablemgr;
		plugin = sm;
		if (rootMenu == null) {
			rootMenu = Bukkit.createInventory(player, 9, IOManager.getString(Interface.opt, false));
			nobleMenu = Bukkit.createInventory(player, 9, IOManager.getString(Interface.opt, true));
			menu = new Menu();
		}
		npcMap.put(player, npc);
		if (player.hasPermission("stablemaster.noble.service")) {
			player.openInventory(Menu.nobleMenu);
		} else {
			player.openInventory(Menu.rootMenu);
		}
	}

	/**
	 * This method opens the Stable GUI for the player and adds them to the
	 * viewer map.
	 * 
	 * @param player
	 *            The player opening Stable GUI
	 * @param stableAcct
	 *            The StableAcct for the player.
	 */
	private static void openStableGUI(Player player, StableAcct stableAcct) {
		menuMap.put(player, menu.new PlayerMenu(player, stableAcct));
	}

	/**
	 * Spawns a new mount halfway between the player and npc.
	 * 
	 * @param player
	 *            Player who is to recieve a mount.
	 * @param mount
	 *            The mount object (name/type) that will be generated.
	 * @param npc
	 *            The npc who is offering the mount.
	 */
	@SuppressWarnings("deprecation")
	static void spawnMount(Player player, Mount mount, NPC npc, Boolean sold) {

		if (player.isInsideVehicle() && sold) {
			stableMgr.get(player.getName()).addMount(mount);
			IOManager.msg(player, Action.keep, mount);
		} else {
			if (sold)
				IOManager.msg(player, Action.give, mount);

			// Get the midpoint.
			Location location = npc.getStoredLocation();
			location.add((player.getLocation().subtract(location)).multiply(0.8));
			location.setPitch(player.getLocation().getYaw() + 180);
			location.setYaw(0);

			LivingEntity newMount = (LivingEntity) player.getWorld().spawnEntity(location, mount.getType());
			newMount.setRemoveWhenFarAway(false);
			if (!mount.getName().equals(newMount.getClass().getSimpleName().replace("Craft", ""))
					&& !mount.getName().equals("Donkey") && !mount.getName().equals("Mule")
					&& !mount.getName().equals("Llama")) {
				newMount.setCustomName(mount.getName());
				newMount.setCustomNameVisible(true);
			}

			if (mount.getType().equals(EntityType.PIG)) {
				((Pig) newMount).setSaddle(true);
				((Pig) newMount).setMaxHealth(mount.getHealth());
				((Pig) newMount).setHealth(mount.getHealth());
				((Pig) newMount).setAge(mount.getAge());
				AttributeInstance attributes = newMount.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributes.setBaseValue(mount.getSpeed());
			}else if (mount.getType().equals(EntityType.HORSE)) {
					((Horse) newMount).setTamed(true);
					((Horse) newMount).setAge(mount.getAge());
					((Horse) newMount).setColor(mount.getColor());
					((Horse) newMount).setStyle(mount.getStyle());
					((Horse) newMount).setMaxHealth(mount.getHealth());
					((Horse) newMount).setHealth(mount.getHealth());
					((Horse) newMount).setJumpStrength(mount.getJumpstr());
					
					((Horse) newMount).getInventory().setSaddle(mount.getSaddle());
					((Horse) newMount).getInventory().setArmor(mount.getArmor());
					((Horse) newMount).setTamed(true);
					((Horse) newMount).setOwner(player);
					AttributeInstance attributes = newMount.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
					attributes.setBaseValue(mount.getSpeed());
			} else if(mount.getType().equals(EntityType.MULE)){
				((Mule) newMount).setTamed(true);
				((Mule) newMount).setAge(mount.getAge());
				((Mule) newMount).setMaxHealth(mount.getHealth());
				((Mule) newMount).setHealth(mount.getHealth());
				((Mule) newMount).setJumpStrength(mount.getJumpstr());
				((Mule) newMount).setCarryingChest(mount.HasChest());
				
				if (mount.HasChest()) {
					((Mule) newMount).getInventory().setStorageContents(mount.getInventory());
				}
				
				((Mule) newMount).getInventory().setItem(0, mount.getSaddle());
				((Mule) newMount).setTamed(true);
				((Mule) newMount).setOwner(player);
				AttributeInstance attributes = newMount.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributes.setBaseValue(mount.getSpeed());
			}else if(mount.getType().equals(EntityType.DONKEY)){
				((Donkey) newMount).setTamed(true);
				((Donkey) newMount).setAge(mount.getAge());
				((Donkey) newMount).setMaxHealth(mount.getHealth());
				((Donkey) newMount).setHealth(mount.getHealth());
				((Donkey) newMount).setJumpStrength(mount.getJumpstr());
				((Donkey) newMount).setCarryingChest(mount.HasChest());
				
				if (mount.HasChest()) {
					((Donkey) newMount).getInventory().setStorageContents(mount.getInventory());
				}
				
				((Donkey) newMount).getInventory().setItem(0, mount.getSaddle());
				((Donkey) newMount).setTamed(true);
				((Donkey) newMount).setOwner(player);
				AttributeInstance attributes = newMount.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributes.setBaseValue(mount.getSpeed());
			}else if(mount.getType().equals(EntityType.LLAMA)){
				((Llama) newMount).setTamed(true);
				((Llama) newMount).setAge(mount.getAge());
				((Llama) newMount).setMaxHealth(mount.getHealth());
				((Llama) newMount).setHealth(mount.getHealth());
				((Llama) newMount).setCarryingChest(mount.HasChest());
				((Llama) newMount).setColor(mount.getLlamaColor());
				
				if (mount.HasChest()) {
					((Llama) newMount).getInventory().setStorageContents(mount.getInventory());
				}
				
				((Llama) newMount).getInventory().setItem(1, mount.getCarpet());
				((Llama) newMount).setTamed(true);
				((Llama) newMount).setOwner(player);
				AttributeInstance attributes = newMount.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributes.setBaseValue(mount.getSpeed());
			}else if (mount.getType().equals(EntityType.SKELETON_HORSE)) {
				((SkeletonHorse) newMount).setTamed(true);
				((SkeletonHorse) newMount).setAge(mount.getAge());
				((SkeletonHorse) newMount).setMaxHealth(mount.getHealth());
				((SkeletonHorse) newMount).setHealth(mount.getHealth());
				((SkeletonHorse) newMount).setJumpStrength(mount.getJumpstr());
				
				((SkeletonHorse) newMount).getInventory().setItem(0, mount.getSaddle());
				((SkeletonHorse) newMount).getInventory().setItem(1, mount.getArmor());
				((SkeletonHorse) newMount).setTamed(true);
				((SkeletonHorse) newMount).setOwner(player);
				AttributeInstance attributes = newMount.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributes.setBaseValue(mount.getSpeed());
			}else{
				((ZombieHorse) newMount).setTamed(true);
				((ZombieHorse) newMount).setAge(mount.getAge());
				((ZombieHorse) newMount).setMaxHealth(mount.getHealth());
				((ZombieHorse) newMount).setHealth(mount.getHealth());
				((ZombieHorse) newMount).setJumpStrength(mount.getJumpstr());
				
				((ZombieHorse) newMount).getInventory().setItem(0, mount.getSaddle());
				((ZombieHorse) newMount).getInventory().setItem(1, mount.getArmor());
				((ZombieHorse) newMount).setTamed(true);
				((ZombieHorse) newMount).setOwner(player);
				AttributeInstance attributes = newMount.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributes.setBaseValue(mount.getSpeed());
			}
			newMount.setHealth(mount.getHealth());
			if (!player.isInsideVehicle())
				newMount.setPassenger(player);
			

		}
	}

	@EventHandler
	void onInventoryClick(InventoryClickEvent event) {
		boolean validSlot = (event.getRawSlot() <= 8 && event.getRawSlot() >= 0);
		if ((event.getInventory().getTitle().equalsIgnoreCase(IOManager.getString(Interface.opt, false))
				|| event.getInventory().getTitle().equalsIgnoreCase(IOManager.getString(Interface.opt, true)))
				&& validSlot) {
			event.setCancelled(true);
			final Player player = (Player) event.getWhoClicked();

			switch (event.getRawSlot()) {
			// Go to Stable
			case 0:
				if (stableMgr.get(player.getName()).getNumMounts() > 0) {
					Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,new Runnable() {
						public void run() {
							Menu.openStableGUI(player,stableMgr.get(player.getName()));
						}
					}, (long) .005);
				} else {
					IOManager.msg(player, Action.nil, null);
					player.closeInventory();
					npcMap.remove(player);
				}
				break;

			// Drop off mount
			case 1:
				player.closeInventory();
				if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity) {
					if (stableMgr.get(player.getName()).getDebt() > 0) {
						StableAcct acct = stableMgr.get(player.getName());
						IOManager.msg(player, Action.debt, acct);
					} else {
						LivingEntity vehicle = (LivingEntity) player.getVehicle();
						String name = IOManager.getVehicleName(player);
						StableAcct acct = stableMgr.get(player.getName());
						if (acct.hasRoom()) {
							double price = IOManager.Traits.stable.getPriceInit();
							if (player.hasPermission("stablemaster.noble.service")) {
								price = 0;
							}
							if (IOManager.charge(npcMap.get(player), player, price)) {
								IOManager.msg(player, Action.stow, null);
								Mount mount;
								mount = new Mount(name, vehicle.getType(), 
								System.currentTimeMillis(), vehicle);
								
								Entity veh = player.getVehicle();
								mount.setUUID(veh.getUniqueId());
								acct.addMount(mount);
								
								veh.eject();
								veh.remove();
							}
						} else {
							IOManager.msg(player, Action.full, null);
						}
					}
				} else {
					IOManager.msg(player, Action.invalid, null);
				}
				npcMap.remove(player);
				break;

			// Pay off debt
			case 2:
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
						new Runnable() {
							public void run() {
								cashMap.put(player, new CashMenu(player,stableMgr.get(player.getName())));
							}
						}, (long) .005);
				break;
			// Buy a mount
			case 3:
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
						new Runnable() {
							public void run() {
								buyMap.put(player,
										new BuyMenu(player, stableMgr.get(player.getName()), npcMap.get(player)));
							}
						}, (long) .005);
				break;
			// Exit
			case 8:
				player.closeInventory();
				npcMap.remove(player);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * @author izbay Used to traverse player stables
	 */
	private class PlayerMenu implements Listener {
		private String name;
		private int size;
		private StableAcct acct;

		/**
		 * Stable GUI constructor.
		 * 
		 * @param player
		 *            Player who is viewing this stable.
		 * @param stableAcct
		 *            Corresponding account of the player.
		 */
		public PlayerMenu(Player player, StableAcct stableAcct) {
			name = player.getName() + IOManager.getString(Interface.stable,
					player.hasPermission("stablemaster.noble"));

			// Number of mounts
			int maxSize = (int) (Math.ceil((double) (IOManager.Traits.stable.getMaxMounts()) / 9) * 9);
			int currSize = (int) (Math.ceil((double) (stableAcct.getNumMounts()) / 9) * 9);
			if (maxSize == 0 || currSize < maxSize) {
				size = currSize;
			} else {
				size = maxSize;
			}

			acct = stableAcct;
			Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);
			open(player);
		}

		/**
		 * Opens the Stable GUI.
		 * 
		 * @param player
		 *            Player who is viewing this stable.
		 */
		private void open(Player player) {
			Inventory menu = Bukkit.createInventory(player, size, name);
			for (int i = 0; i < acct.getNumMounts(); i++) {
				Mount thisMount = acct.getMount(i);
				menu.setItem(i, buildItem(player, thisMount));
			}
			player.openInventory(menu);
		}

		/**
		 * Creates a button representing a mount in the Stable GUI
		 * 
		 * @param player
		 *            Player who's Stable GUI this is being generated for.
		 * @param mount
		 *            Mount which this button represents.
		 * @return Returns an ItemStack which represents a mount for the Stable
		 *         GUI. Displays name of mount and how much is owed to reacquire
		 *         it.
		 */
		private ItemStack buildItem(Player player, Mount mount) {

			ItemStack button = new ItemStack(Material.EGG, 1);

			if (mount.getType().equals(EntityType.PIG)) {
				button = new ItemStack(Material.CARROT_ITEM, 1);
			} else if (mount.getType().equals(EntityType.HORSE)) {
				button = new ItemStack(Material.SADDLE, 1);
			} else if (mount.getType().equals(EntityType.MULE)) {
				button = new ItemStack(Material.CHEST, 1);
			} else if (mount.getType().equals(EntityType.DONKEY)) {
				button = new ItemStack(Material.APPLE, 1);
			} else if (mount.getType().equals(EntityType.LLAMA)) {
				button = new ItemStack(Material.CARPET, 1, (short) 3);
			} else if (mount.getType().equals(EntityType.SKELETON_HORSE)) {
				button = new ItemStack(Material.BONE, 1);
			} else if (mount.getType().equals(EntityType.ZOMBIE_HORSE)) {
				button = new ItemStack(Material.ROTTEN_FLESH, 1);
			}

			ItemMeta im = button.getItemMeta();
			im.setDisplayName(mount.getName());
			List<String> lore = new ArrayList<String>();
			if(mount.getSpeed()!=0){
				
				lore.add(ChatColor.translateAlternateColorCodes('&', IOManager.speed + ": " + formatDouble(mount.getSpeed())));
			}
			if(mount.getJumpstr()!=0){
				lore.add(ChatColor.translateAlternateColorCodes('&', IOManager.jump + ": " + formatDouble(mount.getJumpstr())));
			}
			if(mount.getHealth()!=0){
				lore.add(ChatColor.translateAlternateColorCodes('&', IOManager.health + ": " + formatDouble(mount.getHealth())));
			}
			for(String s : Arrays.asList(IOManager.econFormat(player, IOManager.getCost(player, mount)))){
				lore.add(s);
			}
			im.setLore(lore);
			button.setItemMeta(im);
			return button;
		}
		
		private String formatDouble(Double format){
			DecimalFormat df = new DecimalFormat("#0.00");
			return df.format(format.doubleValue());
		}

		/**
		 * Self destructs this menu and removes player from npcMap.
		 * 
		 * @param player
		 *            Player who is exiting this menu.
		 */
		private void killMenu(final Player player) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
					new Runnable() {
						public void run() {
							player.closeInventory();
							npcMap.remove(player);
							menuMap.remove(player);
						}
					}, 1);
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		void onInventoryClick(InventoryClickEvent event) {
			boolean validSlot = (event.getRawSlot() < acct.getNumMounts() && event.getRawSlot() >= 0);

			if (event.getInventory().getTitle().equals(name) && validSlot) {
				event.setCancelled(true);

				Player player = (Player) event.getWhoClicked();
				StableAcct acct = stableMgr.get(player.getName());
				Mount selected = acct.getMount(event.getRawSlot());
				IOManager.pickUpCharge(npcMap.get(player), player, acct,
						IOManager.getCost(player, selected), selected);
				spawnMount(player, selected, npcMap.get(player), false);

				acct.removeMount(event.getRawSlot());

				killMenu(player);
			}
		}

		@EventHandler
		void onInventoryClose(InventoryCloseEvent event) {
			if (event.getInventory().getTitle().equals(name)) {
				Player player = (Player) event.getPlayer();
				killMenu(player);
			}
		}
	}

	/**
	 * @author izbay Used to show users their balances and allow them to pay.
	 */
	private class CashMenu implements Listener {
		private String name;
		private StableAcct acct;

		/**
		 * Constructor for CashMenu GUI.
		 * 
		 * @param player
		 *            Player who is viewing the CashMenu.
		 * @param stableAcct
		 *            Corresponding StableAcct.
		 */
		public CashMenu(Player player, StableAcct stableAcct) {
			name = player.getName() + IOManager.getString(Interface.acct,
					player.hasPermission("stablemaster.noble"));
			acct = stableAcct;
			Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);

			Inventory cashMenu = Bukkit.createInventory(player, 9, name);
			cashMenu.setItem(0, IOManager.makeButton(
					IOManager.Icons.settleDebt, Interface.settle, false,
					IOManager.econFormat(player, acct.getDebt())));
			cashMenu.setItem(1, IOManager.makeButton(
					IOManager.Icons.clearBooks, Interface.clear, false,
					IOManager.printTotalCost(player, acct)));
			cashMenu.setItem(8, IOManager.makeButton(IOManager.Icons.exitBuy,
					Interface.acctexit, false));
			player.openInventory(cashMenu);
		}

		/**
		 * Self destructs this menu and removes player from npcMap.
		 * 
		 * @param player
		 *            Player who is exiting this menu.
		 */
		private void killMenu(final Player player) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
					new Runnable() {
						public void run() {
							Menu.openRoot(player, stableMgr, plugin, npcMap.get(player));
						}
					}, 1);
			HandlerList.unregisterAll(this);
			cashMap.remove(player);
		}

		@EventHandler
		void onInventoryClick(InventoryClickEvent event) {
			boolean validSlot = (event.getRawSlot() <= 8 && event.getRawSlot() >= 0);
			if (event.getInventory().getTitle().equals(name) && validSlot) {
				event.setCancelled(true);
				final Player player = (Player) event.getWhoClicked();

				switch (event.getRawSlot()) {

				// Pay Debt
				case 0:
					if (IOManager.charge(npcMap.get(player), player, acct.getDebt())) {
						IOManager.msg(player, Action.paid, null);
						acct.setDebt(0);
					}
					killMenu(player);
					break;

				// Pay Balance
				case 1:
					if (IOManager.charge(npcMap.get(player), player, IOManager.getTotalCost(player, acct))) {
						IOManager.msg(player, Action.paid, null);
						long newTime = System.currentTimeMillis();
						for (int i = 0; i < acct.getNumMounts(); i++) {
							if (newTime - acct.getMount(i).getTime() > (86400 * 1000)) {
								acct.getMount(i).setTime(newTime);
							}
						}
					}
					killMenu(player);
					break;

				// Exit
				case 8:
					killMenu(player);
					break;
				}
			}
		}

		@EventHandler
		void onInventoryClose(InventoryCloseEvent event) {
			if (event.getInventory().getTitle().equals(name)) {
				Player player = (Player) event.getPlayer();
				npcMap.remove(player);
				HandlerList.unregisterAll(this);
				cashMap.remove(player);
			}
		}
	}	
}
