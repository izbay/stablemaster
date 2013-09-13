package com.github.izbay;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.v1_6_R2.AttributeInstance;
import net.minecraft.server.v1_6_R2.EntityInsentient;
import net.minecraft.server.v1_6_R2.GenericAttributes;

import org.bukkit.craftbukkit.v1_6_R2.entity.CraftLivingEntity;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
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

import com.github.izbay.IOManager.Action;
import com.github.izbay.StableMgr.Mount;
import com.github.izbay.StableMgr.StableAcct;

/**
 * @author izbay The bulk of stablemaster logic is handled here. By using
 *         inventory to build an interface, navigation of interactions is more
 *         intuitive.
 */
public class Menu implements Listener {
	// Who is looking at the stable menu.
	private static HashMap<Player, PlayerMenu> menuMap = new HashMap<Player, PlayerMenu>();
	// Who is looking at the cash menu.
	private static HashMap<Player, CashMenu> cashMap = new HashMap<Player, CashMenu>();
	// Who is looking at the buy menu.
	private static HashMap<Player, BuyMenu> buyMap = new HashMap<Player, BuyMenu>();
	// For every player viewing a menu, which stablemaster did they speak to?
	private static Map<Player, NPC> npcMap = new HashMap<Player, NPC>();
	// stableMgr imported from the trait. All stable data for every player.
	private static Map<String, StableMgr.StableAcct> stableMgr;
	// Used to handle dynamically built menu of mounts for sale.
	// private static Map<Integer, EntityType> buttonList = new HashMap<Integer,
	// EntityType>();

	private static StableMgr sm = new StableMgr();
	private static Plugin plugin;
	private static Menu menu;
	private static Inventory rootMenu;
	private static Inventory nobleMenu;

	// Enforce static class with private constructor
	private Menu() {
		Menu.plugin.getServer().getPluginManager()
				.registerEvents(this, Menu.plugin);
		rootMenu.setItem(0, IOManager.makeButton(Material.SADDLE, "§aPickup",
				"§fI'd like to pick up my mount!"));
		rootMenu.setItem(1,
				IOManager.makeButton(
						Material.BED,
						"§aDropoff ("
								+ IOManager.econFormat(null,
										IOManager.Traits.stable.getPriceInit())
								+ "§a)", "§fWould you look after my mount?"));
		rootMenu.setItem(2, IOManager.makeButton(Material.GOLD_INGOT, "§aPay",
				"§fI'd like to settle my balance."));
		rootMenu.setItem(3, IOManager.makeButton(Material.PAPER, "§aPurchase",
				"§fHave you any mounts for sale?"));
		rootMenu.setItem(8, IOManager.makeButton(Material.WOOD_DOOR, "§aExit",
				"§fNothing today, thanks."));
		
		
		
		nobleMenu.setItem(0, IOManager.makeButton(Material.SADDLE, "§aPickup",
				"§fI'd like to pick up my mount!"));
		nobleMenu.setItem(1, IOManager.makeButton(
				Material.BED,
				"§aDropoff (§9"
						+ IOManager.econFormat(null,
								IOManager.Traits.stable.getPriceInit()) + " ("
						+ IOManager.econFormat(null, 0) + "§9)§a)",
				"§fWould you look after my mount?"));
		nobleMenu.setItem(2, IOManager.makeButton(Material.GOLD_INGOT, "§aPay",
				"§fI'd like to settle my balance."));
		nobleMenu.setItem(3, IOManager.makeButton(Material.PAPER, "§aPurchase",
				"§fHave you any mounts for sale?"));
		nobleMenu.setItem(8, IOManager.makeButton(Material.WOOD_DOOR, "§aExit",
				"§fNothing today, thanks."));

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
	public static void openRoot(Player player,
			Map<String, StableMgr.StableAcct> stablemgr, Plugin sm, NPC npc) {
		stableMgr = stablemgr;
		plugin = sm;
		if (rootMenu == null) {
			rootMenu = Bukkit.createInventory(player, 9, "Stable Options");
			nobleMenu = Bukkit.createInventory(player, 9, "Stable Options");
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
	private void spawnMount(Player player, Mount mount, NPC npc) {
		// Get the midpoint.
		Location location = npc.getBukkitEntity().getLocation();
		location.add((player.getLocation().subtract(location)).multiply(0.8));
		location.setPitch(player.getLocation().getYaw() + 180);
		location.setYaw(0);

		LivingEntity newMount = (LivingEntity) player.getWorld().spawnEntity(
				location, mount.getType());
		newMount.setRemoveWhenFarAway(false);
		if (!mount.getName().equals(
				newMount.getClass().getSimpleName().replace("Craft", ""))
				&& !mount.getName().equals("Donkey")
				&& !mount.getName().equals("Mule")) {
			newMount.setCustomName(mount.getName());
			newMount.setCustomNameVisible(true);
		}

		if (mount.getType().equals(EntityType.PIG)) {
			((Pig) newMount).setSaddle(true);

		} else if (mount.getType().equals(EntityType.HORSE)) {
			((Horse) newMount).setTamed(true);
			((Horse) newMount).setVariant(mount.getVariant());
			if (mount.getVariant().equals(Horse.Variant.HORSE)) {
				((Horse) newMount).setColor(mount.getColor());
				((Horse) newMount).setStyle(mount.getStyle());
			}
			((Horse) newMount).setMaxHealth(mount.getHealth());
			((Horse) newMount).setJumpStrength(mount.getJumpstr());
			((Horse) newMount).setCarryingChest(mount.HasChest());
			if (mount.HasChest()) {
				((Horse) newMount).getInventory().setContents(
						mount.getInventory());
			}
			((Horse) newMount).getInventory().setSaddle(
					new ItemStack(Material.SADDLE));
			((Horse) newMount).getInventory().setArmor(mount.getArmor());
			((Horse) newMount).setTamed(true);
			((Horse) newMount).setOwner(player);
			AttributeInstance attributes = ((EntityInsentient) ((CraftLivingEntity) newMount)
					.getHandle()).getAttributeInstance(GenericAttributes.d);
			attributes.setValue(mount.getSpeed());
		}
	}

	@EventHandler
	void onInventoryClick(InventoryClickEvent event) {
		boolean validSlot = (event.getRawSlot() <= 8 && event.getRawSlot() >= 0);
		if (event.getInventory().getTitle().equalsIgnoreCase("Stable Options")
				&& validSlot) {
			event.setCancelled(true);
			final Player player = (Player) event.getWhoClicked();

			switch (event.getRawSlot()) {
			// Go to Stable
			case 0:
				if (stableMgr.get(player.getName()).getNumMounts() > 0) {
					Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
							new Runnable() {
								public void run() {
									Menu.openStableGUI(player,
											stableMgr.get(player.getName()));
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
				if (player.isInsideVehicle()
						&& player.getVehicle() instanceof LivingEntity) {
					if (stableMgr.get(player.getName()).getDebt() > 0) {
						StableAcct acct = stableMgr.get(player.getName());
						IOManager.msg(player, Action.debt, acct);
					} else {
						LivingEntity vehicle = (LivingEntity) player
								.getVehicle();
						String name = IOManager.getVehicleName(player);
						StableAcct acct = stableMgr.get(player.getName());
						if (acct.hasRoom()) {
							double price = IOManager.Traits.stable
									.getPriceInit();
							if (player
									.hasPermission("stablemaster.noble.service")) {
								price = 0;
							}
							if (IOManager.charge(npcMap.get(player), player,
									price)) {
								IOManager.msg(player, Action.stow, null);
								if (vehicle.getType().equals(EntityType.HORSE)) {
									acct.addMount(sm.new Mount(name, vehicle
											.getType(), System
											.currentTimeMillis(),
											(Horse) vehicle));
								} else {
									acct.addMount(sm.new Mount(name, vehicle
											.getType(), System
											.currentTimeMillis()));
								}
								Entity veh = player.getVehicle();
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
								cashMap.put(player, new CashMenu(player,
										stableMgr.get(player.getName())));
							}
						}, (long) .005);
				break;
			// Buy a mount
			case 3:
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
						new Runnable() {
							public void run() {
								buyMap.put(
										player,
										new BuyMenu(player, stableMgr
												.get(player.getName()), npcMap
												.get(player)));
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
			name = player.getName() + "'s Stable";

			// Number of mounts
			int maxSize = (int) (Math.ceil((double) (IOManager.Traits.stable
					.getMaxMounts()) / 9) * 9);
			int currSize = (int) (Math
					.ceil((double) (stableAcct.getNumMounts()) / 9) * 9);
			if (maxSize == 0 || currSize < maxSize) {
				size = currSize;
			} else {
				size = maxSize;
			}

			acct = stableAcct;
			Menu.plugin.getServer().getPluginManager()
					.registerEvents(this, Menu.plugin);
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

				if (mount.getVariant() == null || mount.getVariant().equals(Horse.Variant.HORSE)) {
					button = new ItemStack(Material.SADDLE, 1);

				} else if (mount.getVariant().equals(Horse.Variant.MULE)) {
					button = new ItemStack(Material.CHEST, 1);

				} else if (mount.getVariant().equals(Horse.Variant.DONKEY)) {
					button = new ItemStack(Material.APPLE, 1);

				} else if (mount.getVariant().equals(
						Horse.Variant.SKELETON_HORSE)) {
					button = new ItemStack(Material.BONE, 1);

				} else {// It's a zombie.
					button = new ItemStack(Material.ROTTEN_FLESH, 1);
				}
			}

			ItemMeta im = button.getItemMeta();
			im.setDisplayName(mount.getName());
			im.setLore(Arrays.asList(IOManager.econFormat(player,
					IOManager.getCost(player, mount))));
			button.setItemMeta(im);
			return button;
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
			boolean validSlot = (event.getRawSlot() < acct.getNumMounts() && event
					.getRawSlot() >= 0);

			if (event.getInventory().getTitle().equals(name) && validSlot) {
				event.setCancelled(true);

				Player player = (Player) event.getWhoClicked();
				StableAcct acct = stableMgr.get(player.getName());
				Mount selected = acct.getMount(event.getRawSlot());
				IOManager.pickUpCharge(npcMap.get(player), player, acct,
						IOManager.getCost(player, selected), selected);
				spawnMount(player, selected, npcMap.get(player));

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
			name = player.getName() + "'s Account";
			acct = stableAcct;
			Menu.plugin.getServer().getPluginManager()
					.registerEvents(this, Menu.plugin);

			Inventory cashMenu = Bukkit.createInventory(player, 9, name);
			cashMenu.setItem(
					0,
					IOManager.makeButton(Material.FEATHER, "§aSettle Debt ("
							+ IOManager.econFormat(player, acct.getDebt())
							+ "§a)", "§fI make good on my word!"));
			cashMenu.setItem(1, IOManager.makeButton(
					Material.BOOK,
					"§aClear the books ("
							+ IOManager.printTotalCost(player, acct) + "§a)",
					"§fI'd like to pay early."));
			cashMenu.setItem(8, IOManager.makeButton(Material.WOOD_DOOR,
					"§aExit", "§fNevermind!"));
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
							Menu.openRoot(player, stableMgr, plugin,
									npcMap.get(player));
						}
					}, 1);
			HandlerList.unregisterAll(this);
			cashMap.remove(player);
		}

		@EventHandler
		void onInventoryClick(InventoryClickEvent event) {
			boolean validSlot = (event.getRawSlot() <= 8 && event.getRawSlot() >= 0);
			if (event.getInventory().getTitle().contains("Account")
					&& validSlot) {
				event.setCancelled(true);
				final Player player = (Player) event.getWhoClicked();

				switch (event.getRawSlot()) {

				// Pay Debt
				case 0:
					if (IOManager.charge(npcMap.get(player), player,
							acct.getDebt())) {
						IOManager.msg(player, Action.paid, null);
						acct.setDebt(0);
					}
					killMenu(player);
					break;

				// Pay Balance
				case 1:
					if (IOManager.charge(npcMap.get(player), player,
							IOManager.getTotalCost(player, acct))) {
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

	/**
	 * @author izbay Used to design mounts and purchase them.
	 */
	private class BuyMenu implements Listener {
		private String name;
		@SuppressWarnings("unused")
		private StableAcct acct;
		private NPC npc;
		private Player player;

		// private Mount mount = sm.new Mount();
		// As much as I'd rather use enums, I need to use modulus, so these are
		// ints.
		private int type = 0;
		private int health = 0;
		private int jump = 0;
		private int speed = 0;
		private int color = 0;
		private int pattern = 0;
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
			name = "Purchase a Mount";
			acct = stableAcct;
			this.npc = npc;
			this.player = player;
			Menu.plugin.getServer().getPluginManager()
					.registerEvents(this, Menu.plugin);
			menu = buildMenu(player);
			player.openInventory(menu);
		}

		private Inventory buildMenu(Player player) {
			Inventory cashMenu = Bukkit.createInventory(player, 27, name);
			cashMenu.setItem(0, IOManager.makeButton(Material.STONE_BUTTON,
					"§aPrevious", "§fType"));
			cashMenu.setItem(1, IOManager.makeButton(
					Material.CARROT_ITEM,
					1,
					"§aPig",
					IOManager.econFormat(player,
							IOManager.mountPrice.get("pig"))));
			cashMenu.setItem(2, IOManager.makeButton(Material.STONE_BUTTON,
					"§aNext", "§fType"));
			cashMenu.setItem(8, IOManager.makeButton(Material.WOOD_DOOR,
					"§aExit", "§fNevermind!"));
			if (player.hasPermission("stablemaster.noble.discount")) {
				double oldcost = cost;
				cost *= IOManager.mountPrice.get("noble");
				cashMenu.setItem(26, IOManager.makeButton(
						Material.GOLD_INGOT,
						"§aPurchase",
						"§9" + oldcost + " ("
								+ IOManager.econFormat(player, cost) + "§9)"));
			} else {
				cashMenu.setItem(26, IOManager.makeButton(Material.GOLD_INGOT,
						"§aPurchase", IOManager.econFormat(player, cost)));
			}
			return cashMenu;
		}

		private void updateMenu(Player player) {

			// Type (and subsequently which buttons are displayed)
			switch (type) {
			case 0:
				menu.setItem(1, IOManager.makeButton(
						Material.CARROT_ITEM,
						1,
						"§aPig",
						IOManager.econFormat(player,
								IOManager.mountPrice.get("pig"))));

				// Remove all extra buttons.
				menu.setItem(4, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(9, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(13, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(18, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(22, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(6, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(11, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(15, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(20, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(24, IOManager.makeButton(Material.AIR, "", ""));

				cost = IOManager.mountPrice.get("pig");
				break;

			case 1:
				menu.setItem(1, IOManager.makeButton(
						Material.SADDLE,
						2,
						"§aHorse",
						IOManager.econFormat(player,
								IOManager.mountPrice.get("horse"))));

				// Draw all buttons in.
				menu.setItem(4, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fHealth"));
				menu.setItem(9, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fJump"));
				menu.setItem(13, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fSpeed"));
				menu.setItem(18, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fColor"));
				menu.setItem(22, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fPattern"));
				menu.setItem(6, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fHealth"));
				menu.setItem(11, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fJump"));
				menu.setItem(15, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fSpeed"));
				menu.setItem(20, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fColor"));
				menu.setItem(24, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fPattern"));

				cost = IOManager.mountPrice.get("horse");
				break;
			case 2:
				menu.setItem(1, IOManager.makeButton(
						Material.CHEST,
						3,
						"§aMule",
						IOManager.econFormat(player,
								IOManager.mountPrice.get("mule"))));

				// Draw in only the used buttons.
				menu.setItem(4, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fHealth"));
				menu.setItem(9, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fJump"));
				menu.setItem(13, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fSpeed"));
				menu.setItem(18, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(22, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(6, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fHealth"));
				menu.setItem(11, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fJump"));
				menu.setItem(15, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fSpeed"));
				menu.setItem(20, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(24, IOManager.makeButton(Material.AIR, "", ""));

				cost = IOManager.mountPrice.get("mule");
				break;

			case 3:
				menu.setItem(1, IOManager.makeButton(
						Material.APPLE,
						4,
						"§aDonkey",
						IOManager.econFormat(player,
								IOManager.mountPrice.get("donkey"))));

				// Draw in only the used buttons.
				menu.setItem(4, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fHealth"));
				menu.setItem(9, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fJump"));
				menu.setItem(13, IOManager.makeButton(Material.STONE_BUTTON,
						"§aPrevious", "§fSpeed"));
				menu.setItem(18, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(22, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(6, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fHealth"));
				menu.setItem(11, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fJump"));
				menu.setItem(15, IOManager.makeButton(Material.STONE_BUTTON,
						"§aNext", "§fSpeed"));
				menu.setItem(20, IOManager.makeButton(Material.AIR, "", ""));
				menu.setItem(24, IOManager.makeButton(Material.AIR, "", ""));

				cost = IOManager.mountPrice.get("donkey");
				break;
			}

			// Health
			if (type == 0) {
				menu.setItem(5, IOManager.makeButton(Material.AIR, "", ""));
			} else {
				if (health == 0) {
					menu.setItem(5, IOManager.makeButton(Material.RED_MUSHROOM,
							15, "§a15", ""));
				} else {
					menu.setItem(5, IOManager.makeButton(
							Material.RED_MUSHROOM,
							health + 15,
							"§a" + (health + 15),
							IOManager.econFormat(player,
									IOManager.mountPrice.get("health")
											* (health))));
					cost += (IOManager.mountPrice.get("health") * health);
				}
			}

			// Jump
			if (type == 0) {
				menu.setItem(10, IOManager.makeButton(Material.AIR, "", ""));
			} else {
				if (jump == 0) {
					menu.setItem(10, IOManager.makeButton(Material.FENCE_GATE,
							30, "§a0.30", ""));
				} else {
					menu.setItem(10, IOManager.makeButton(
							Material.FENCE_GATE,
							jump + 30,
							"§a0." + (jump + 30),
							IOManager.econFormat(player,
									IOManager.mountPrice.get("jump") * (jump))));
					cost += (IOManager.mountPrice.get("jump") * jump);
				}
			}

			// Speed
			if (type == 0) {
				menu.setItem(14, IOManager.makeButton(Material.AIR, "", ""));
			} else {
				if (speed == 0) {
					menu.setItem(14, IOManager.makeButton(Material.FEATHER, 30,
							"§a0.30", ""));
				} else {
					menu.setItem(14, IOManager.makeButton(Material.FEATHER,
							speed + 30, "§a0." + (speed + 30), IOManager
									.econFormat(player,
											IOManager.mountPrice.get("speed")
													* (speed))));
					cost += (IOManager.mountPrice.get("speed") * speed);
				}
			}

			// Color
			if (type == 1) {
				switch (color) {
				case 0:
					menu.setItem(19, IOManager.makeButton(Material.SNOW_BLOCK,
							1, "§aWhite", ""));
					break;
				case 1:
					menu.setItem(19, IOManager.makeButton(Material.WOOD, 2,
							"§aBuckskin", ""));
					break;
				case 2:
					menu.setItem(19, IOManager.makeButton(Material.HARD_CLAY,
							3, "§fChestnut", ""));
					break;
				case 3:
					menu.setItem(19, IOManager.makeButton(Material.SOUL_SAND,
							4, "§fBay", ""));
					break;
				case 4:
					menu.setItem(19, IOManager.makeButton(Material.COAL_BLOCK,
							5, "§aBlack", ""));
					break;
				case 5:
					menu.setItem(19, IOManager.makeButton(Material.STONE, 6,
							"§aDapple", ""));
					break;
				case 6:
					menu.setItem(19, IOManager.makeButton(
							Material.NETHER_BRICK, 7, "§aLiver Chestnut", ""));
					break;
				}
			} else {
				menu.setItem(19, IOManager.makeButton(Material.AIR, "", ""));
			}

			// Style
			if (type == 1) {
				switch (pattern) {
				case 0:
					menu.setItem(23, IOManager.makeButton(Material.LEATHER, 1,
							"§aPlain", ""));
					break;
				case 1:
					menu.setItem(23, IOManager.makeButton(Material.GHAST_TEAR,
							2, "§aBlaze", ""));
					break;
				case 2:
					menu.setItem(23, IOManager.makeButton(Material.MILK_BUCKET,
							3, "§aPaint", ""));
					break;
				case 3:
					menu.setItem(23, IOManager.makeButton(
							Material.PUMPKIN_SEEDS, 4, "§aAppaloosa", ""));
					break;
				case 4:
					menu.setItem(23, IOManager.makeButton(Material.COAL, 5,
							"§aSooty", ""));
					break;
				}
			} else {
				menu.setItem(23, IOManager.makeButton(Material.AIR, "", ""));
			}

			if (player.hasPermission("stablemaster.noble.discount")) {
				double oldcost = cost;
				cost *= IOManager.mountPrice.get("noble");
				menu.setItem(26, IOManager.makeButton(
						Material.GOLD_INGOT,
						"§aPurchase",
						"§9" + oldcost + " ("
								+ IOManager.econFormat(player, cost) + "§9)"));
			} else {
				menu.setItem(26, IOManager.makeButton(Material.GOLD_INGOT,
						"§aPurchase", IOManager.econFormat(player, cost)));
			}
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
							Menu.openRoot(player, stableMgr, plugin,
									npcMap.get(player));
						}
					}, 1);
			HandlerList.unregisterAll(this);
		}

		@EventHandler
		void onInventoryClick(InventoryClickEvent event) {
			boolean validSlot = (event.getRawSlot() <= 26 && event.getRawSlot() >= 0);
			boolean thisPlayer = (Player) event.getWhoClicked() == this.player;

			if (event.getInventory().getTitle().contains("Purchase")
					&& validSlot) {
				event.setCancelled(true);

				if (thisPlayer) {
					final Player player = (Player) event.getWhoClicked();

					switch (event.getRawSlot()) {

					// Previous mount type
					case 0:
						type = ((((type - 1) % 4) + 4) % 4);
						if (type != 2) {
							color = 0;
							pattern = 0;
						}
						if (type == 0) {
							health = 0;
							jump = 0;
							speed = 0;
						}
						break;
					// Next mount type
					case 2:
						type = (type + 1) % 4;
						if (type != 3) {
							color = 0;
							pattern = 0;
						}
						if (type == 0) {
							health = 0;
							jump = 0;
							speed = 0;
						}
						break;
					// Previous health
					case 4:
						if (type != 0) {
							health = ((((health - 1) % 16) + 16) % 16);
						}
						break;
					// Next health
					case 6:
						if (type != 0) {
							health = (health + 1) % 16;
						}
						break;
					// Exit
					case 8:
						killMenu(player);
						break;
					// Previous jump
					case 9:
						if (type != 0) {
							jump = ((((jump - 1) % 35) + 35) % 35);
						}
						break;
					// Next jump
					case 11:
						if (type != 0) {
							jump = (jump + 1) % 35;
						}
						break;
					// Previous speed
					case 13:
						if (type != 0) {
							speed = ((((speed - 1) % 35) + 35) % 35);
						}
						break;
					// Next speed
					case 15:
						if (type != 0) {
							speed = (speed + 1) % 35;
						}
						break;
					// Previous color
					case 18:
						if (type == 1) {
							color = ((((color - 1) % 7) + 7) % 7);
						}
						break;
					// Next color
					case 20:
						if (type == 1) {
							color = (color + 1) % 7;
						}
						break;
					// Previous style
					case 22:
						if (type == 1) {
							pattern = ((((pattern - 1) % 5) + 5) % 5);
						}
						break;
					// Next style
					case 24:
						if (type == 1) {
							pattern = (pattern + 1) % 5;
						}
						break;
					// Purchase
					case 26:
						if (IOManager.charge(npc, player, cost)) {
							Mount thisMount = null;
							// Deconstruct those int values to build the mount
							// the player selected!
							switch (type) {
							// Pig
							case 0:
								thisMount = sm.new Mount(
										IOManager.getSomeName(),
										EntityType.PIG,
										System.currentTimeMillis());
								break;

							// Horse
							case 1:
								thisMount = sm.new Mount(
										IOManager.getSomeName(),
										EntityType.HORSE,
										System.currentTimeMillis());
								thisMount.setVariant(Horse.Variant.HORSE);

								// Set the color/pattern.
								thisMount.setColor(Horse.Color.values()[color]);
								thisMount
										.setStyle(Horse.Style.values()[pattern]);
								break;

							// Mule
							case 2:
								thisMount = sm.new Mount(
										IOManager.getSomeName(),
										EntityType.HORSE,
										System.currentTimeMillis());
								thisMount.setVariant(Horse.Variant.MULE);
								break;

							// Donkey
							case 3:
								thisMount = sm.new Mount(
										IOManager.getSomeName(),
										EntityType.HORSE,
										System.currentTimeMillis());
								thisMount.setVariant(Horse.Variant.DONKEY);
								break;

							}

							// Now set the speed, jump, health, chest, and
							// saddle if it's a horse type.
							if (type != 0) {
								thisMount.setSpeed(((double) speed + 30) / 100);
								thisMount
										.setJumpstr((((double) jump + 30) / 100) + 0.15);
								thisMount.setHealth((double) health + 15);
								thisMount.setChest(false);
								thisMount.setSaddle(new ItemStack(
										Material.SADDLE, 1));
							}

							spawnMount(player, thisMount, npc);

							IOManager.msg(player, Action.give, thisMount);

							npcMap.remove(player);
							HandlerList.unregisterAll(this);
							buyMap.remove(player);
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

			if (event.getInventory().getTitle().contains("Purchase")
					&& event.getPlayer().equals(this.player)) {
				Player player = (Player) event.getPlayer();
				npcMap.remove(player);
				HandlerList.unregisterAll(this);
				buyMap.remove(player);
			}
		}
	}
}
