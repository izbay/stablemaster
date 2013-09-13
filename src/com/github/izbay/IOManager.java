package com.github.izbay;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.izbay.StableMgr.Mount;
import com.github.izbay.StableMgr.StableAcct;
import com.github.izbay.StablemasterPlugin;
import me.tehbeard.cititrader.WalletTrait;
import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;

/**
 * @author izbay Takes care of all String operations and data loaded from
 *         config. Formats messages to players, and handles Economy.
 */
public class IOManager {
	private static Economy economy;
	private static boolean hasCitiTrader;
	private static FileConfiguration config;
	public static HashMap<String, Double> mountPrice;
	public static IOManager ioManager;

	public static void init(StablemasterPlugin plugin) {
		economy = plugin.economy;
		hasCitiTrader = plugin.hasCitiTrader;
		config = plugin.config;
		ioManager = new IOManager();
	}

	// public static Map<EntityType, Integer> mountPrice = new
	// HashMap<EntityType, Integer>();
	private static Map<Action, String> messages = new HashMap<Action, String>();
	private static Map<Action, String> nobleMsg = new HashMap<Action, String>();
	private static String[] namesList;

	// Enforce static class with private constructor
	private IOManager() {
		// Unpack the config.
		mountPrice = new HashMap<String, Double>();
		mountPrice.put("pig", config.getDouble("mount-cost.pig"));
		mountPrice.put("horse", config.getDouble("mount-cost.horse"));
		mountPrice.put("donkey", config.getDouble("mount-cost.horse"));
		mountPrice.put("mule", config.getDouble("mount-cost.horse"));
		mountPrice.put("boat", config.getDouble("mount-cost.boat"));
		mountPrice.put("cart", config.getDouble("mount-cost.cart"));
		mountPrice.put("health", config.getDouble("mount-cost.health"));
		mountPrice.put("speed", config.getDouble("mount-cost.speed"));
		mountPrice.put("jump", config.getDouble("mount-cost.jump"));
		mountPrice.put("noble", config.getDouble("mount-cost.noble"));

		namesList = (config.getString("stable.random-names")).split(", ");

		for (Action e : Action.values()) {
			messages.put(e, config.getString("text." + e.toString()));
			if (config.contains("noble." + e.toString())) {
				nobleMsg.put(e, config.getString("noble." + e.toString()));
			} else {
				nobleMsg.put(e, config.getString("text." + e.toString()));
			}
		}
	}

	/**
	 * @return A random name selected from the configuration file.
	 */
	public static String getSomeName() {
		int selection = (int) Math.floor(Math.random() * namesList.length);
		return namesList[selection];
	}

	/**
	 * Creates a button for the GUI with a given Icon, title, and description.
	 * 
	 * @param icon
	 *            The material to be used for this button's icon.
	 * @param option
	 *            The title of this button.
	 * @param subtext
	 *            Any description to appear below the title of this button.
	 * @return Returns an ItemStack which can be used in the inventory as a
	 *         button. Has option as the item name and subtext as the lore.
	 */
	public static ItemStack makeButton(Material icon, String option,
			String subtext) {
		ItemStack button = new ItemStack(icon, 1);
		ItemMeta im = button.getItemMeta();
		if (!option.equals(""))
			im.setDisplayName(option);
		if (!subtext.equals(""))
			im.setLore(Arrays.asList(subtext));
		button.setItemMeta(im);
		return button;
	}

	/**
	 * Creates a button for the GUI with a given Icon, title, and description.
	 * 
	 * @param icon
	 *            The material to be used for this button's icon. @ param amt
	 *            How many of the items will be in the stack?
	 * @param option
	 *            The title of this button.
	 * @param subtext
	 *            Any description to appear below the title of this button.
	 * @return Returns an ItemStack which can be used in the inventory as a
	 *         button. Has option as the item name and subtext as the lore.
	 */
	public static ItemStack makeButton(Material icon, int amt, String option,
			String subtext) {
		ItemStack button = new ItemStack(icon, amt);
		ItemMeta im = button.getItemMeta();
		if (!option.equals(""))
			im.setDisplayName(option);
		if (!subtext.equals(""))
			im.setLore(Arrays.asList(subtext));
		button.setItemMeta(im);
		return button;
	}

	public static double getCost(Player player, Mount mount) {
		long elapsedDays = (System.currentTimeMillis() - mount.getTime())
				/ (86400 * 1000);
		double cost = (double) 0;
		if (elapsedDays < 1
				|| player.hasPermission("stablemaster.noble.service")) {
			return cost;
		} else {
			if (mount.getType().equals(EntityType.BOAT)) {
				cost = (double) (elapsedDays * Traits.wharf.getPricePerDay());
			} else if (mount.getType().equals(EntityType.MINECART)) {
				cost = (double) (elapsedDays * Traits.station.getPricePerDay());
			} else {
				cost = (double) (elapsedDays * Traits.stable.getPricePerDay());
			}
			return cost;
		}
	}

	public static String econFormat(Player player, double cost) {
		if (cost == 0)
			return "§2Paid in Full";
		if (player == null)
			return "§9" + economy.format(cost);
		if (canAfford(player, cost))
			return "§6" + economy.format(cost);
		return "§c" + economy.format(cost);
	}

	private static boolean canAfford(Player player, double cost) {
		return economy.getBalance(player.getName()) >= cost;
	}

	public static String printCost(Player player, Mount mount) {
		return econFormat(player, getCost(player, mount));
	}

	public static double getTotalCost(Player player, StableAcct acct) {
		double cost = (double) 0;
		for (int i = 0; i < acct.getNumMounts(); i++) {
			cost += getCost(player, acct.getMount(i));
		}
		return cost;
	}

	public static String printTotalCost(Player player, StableAcct acct) {
		return econFormat(player, getTotalCost(player, acct));
	}

	public static String printDebt(Player player, StableAcct acct) {
		return econFormat(player, acct.getDebt());
	}

	public static boolean charge(NPC npc, Player player, double cost) {
		if (canAfford(player, cost)) {
			economy.withdrawPlayer(player.getName(), cost);
			if (hasCitiTrader) {
				if (npc.hasTrait(WalletTrait.class)) {
					npc.getTrait(WalletTrait.class).deposit(cost);
				}
			}
			if (!messages.get(Action.pay).equals("") && cost > 0)
				player.sendMessage(format(
						Action.pay,
						player,
						messages.get(Action.pay)
								.replace("<AMOUNT>", economy.format(cost))
								.replace("<NPC_NAME>", npc.getName()), null));
			return true;
		} else {
			msg(player, Action.funds, null);
			return false;
		}
	}

	public static void pickUpCharge(NPC npc, Player player, StableAcct acct,
			double cost, Mount mount) {
		if (canAfford(player, cost)) {
			msg(player, Action.give, mount);
			charge(npc, player, cost);
		} else {
			msg(player, Action.broke, mount);
			acct.setDebt(cost - economy.getBalance(player.getName()));
			charge(npc, player, economy.getBalance(player.getName()));
		}
	}

	public static String getVehicleName(Player player) {
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof Boat) && !(vehicle instanceof Minecart)) {
			LivingEntity lvehicle = (LivingEntity) vehicle;
			if (lvehicle.getCustomName() != null) {
				return lvehicle.getCustomName();
			}
		}
		if (vehicle.getType().equals(EntityType.HORSE)) {
			if (((Horse) vehicle).getVariant().equals(Horse.Variant.MULE)) {
				return "Mule";
			} else if (((Horse) vehicle).getVariant().equals(
					Horse.Variant.DONKEY)) {
				return "Donkey";
			}
		}
		return vehicle.getClass().getSimpleName().replace("Craft", "");
	}

	private static String format(Action action, Player player, String input,
			Object arg) {
		input = input.replace("<PLAYER>", player.getName());
		if (arg instanceof StableAcct) {
			input = input.replace("<DEBT>",
					econFormat(player, ((StableAcct) arg).getDebt()));
		}
		if (action.equals(Action.stow)) {
			input = input.replace("<MOUNT_NAME>", getVehicleName(player));
			input = input.replace("<MOUNT_TYPE>", player.getVehicle()
					.getClass().getSimpleName().replace("Craft", ""));
		}
		if (arg instanceof Entity) {
			String name = "your "
					+ arg.getClass().getSimpleName().replace("Craft", "")
							.toLowerCase();
			input = input.replace("<MOUNT_NAME>", name);
			if (arg instanceof Boat) {
				input = input.replace("<MOUNT_PAMPERED>",
						Traits.wharf.getPampered());
			} else if (arg instanceof Minecart) {
				input = input.replace("<MOUNT_PAMPERED>",
						Traits.station.getPampered());
			} else {

			}
		}
		if (arg instanceof Mount) {
			input = input.replace("<MOUNT_NAME>", ((Mount) arg).getName());
			input = input.replace("<MOUNT_TYPE>", ((Mount) arg).getType()
					.name().toLowerCase());
			input = input.replace("<MOUNT_PAMPERED>",
					Traits.stable.getPampered());
		}

		if (arg instanceof String) {
			if (arg.equals("boat")) {
				input = input.replace("<MOUNT_NAME>", "your boat");
				input = input.replace("<MOUNT_PAMPERED>",
						Traits.wharf.getPampered());
			} else if (arg.equals("cart")) {
				input = input.replace("<MOUNT_NAME>", "your minecart");
				input = input.replace("<MOUNT_PAMPERED>",
						Traits.station.getPampered());
			}
		}

		input = input.replace("Pig", "the pig");
		input = input.replace("Boat", "the boat");
		input = input.replace("MinecartRideable", "the minecart");

		// Color Codes
		for (int i = 0; i < 24; i++) {
			String temp = "" + i;
			if (i > 9) {
				temp = "" + (char) (87 + i);
			}
			input = input.replace("&" + temp, "§" + temp);
		}

		return input;
	}

	public static void msg(Player player, Action action, Object arg) {
		if (player.hasPermission("stablemaster.noble") || player.hasPermission("stablemaster.noble.discount") || player.hasPermission("stablemaster.noble.service")) {
			if (!nobleMsg.get(action).equals(""))
				player.sendMessage(format(action, player, nobleMsg.get(action),
						arg));
		} else {
			if (!messages.get(action).equals(""))
				player.sendMessage(format(action, player, messages.get(action),
						arg));
		}
	}

	/**
	 * Types of messages that can be sent to the player based on action.
	 */
	public static enum Action {
		/**
		 * Greet the player.
		 */
		greet,
		/**
		 * Player tried to collect mount but had none.
		 */
		nil,
		/**
		 * Out of funds.
		 */
		funds,
		/**
		 * Incorrect mount or insufficient permission.
		 */
		invalid,
		/**
		 * Reached the mount limit.
		 */
		full,
		/**
		 * Report that their mount has been given.
		 */
		give,
		/**
		 * Insufficient funds to retrieve mount.
		 */
		broke,
		/**
		 * Tried to store mount while in debt.
		 */
		debt,
		/**
		 * Paid down balance.
		 */
		paid,
		/**
		 * Report that their mount has been accepted.
		 */
		stow,
		/**
		 * Report that you have paid the NPC.
		 */
		pay
	};

	public static enum Traits {
		stable, wharf, station;

		private int maxMounts;
		private double priceInit;
		private double pricePerDay;
		private boolean locLog;
		private int cooldown;
		private String pampered;

		private Traits() {
			this.maxMounts = config.getInt(this.name() + ".max-mounts");
			this.priceInit = config.getDouble(this.name() + ".price-init");
			this.pricePerDay = config.getDouble(this.name() + ".price-per-day");
			if (config.contains(this.name() + ".location-log")) {
				this.locLog = config.getBoolean(this.name() + ".location-log");
			} else {
				this.locLog = false;
			}
			this.cooldown = config.getInt(this.name() + ".cooldown");
			this.pampered = config.getString(this.name() + ".pampered");
		}

		public int getMaxMounts() {
			return maxMounts;
		}

		public double getPriceInit() {
			return priceInit;
		}

		public double getPricePerDay() {
			return pricePerDay;
		}

		public boolean getLocLog() {
			return locLog;
		}

		public int getCooldown() {
			return cooldown;
		}

		public String getPampered() {
			return pampered;
		}
	}
}
