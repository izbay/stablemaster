package com.github.izbay.stablemaster;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.izbay.stablemaster.Mount;
import com.github.izbay.stablemaster.StableMgr.StableAcct;
import com.github.izbay.stablemaster.StablemasterPlugin;

import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;

/**
 * @author izbay Takes care of all String operations and data loaded from
 *         config. Formats messages to players, and handles Economy.
 */
public class IOManager {
	private static Economy economy;
	private static FileConfiguration config;
	private static YamlConfiguration lang = new YamlConfiguration();
	private static String langsetting = "en";
	private static String paid;	// Paid in Full
	public static String speed; // speed lang
	public static String jump; //jump lang
	public static String health; // health lang
	
	// public static Map<EntityType, Integer> mountPrice = new
	// HashMap<EntityType, Integer>();
	private static Map<Action, String> messages = new HashMap<Action, String>();
	private static Map<Action, String> nobleMsg = new HashMap<Action, String>();
	private static Map<Icons, ItemStack> icons = new HashMap<Icons, ItemStack>();
	private static Map<Interface, String> interfaceTitle = new HashMap<Interface, String>();
	private static Map<Interface, String> interfaceText = new HashMap<Interface, String>();
	private static Map<Interface, String> NinterfaceTitle = new HashMap<Interface, String>();
	private static Map<Interface, String> NinterfaceText = new HashMap<Interface, String>();
	
	private static String[] namesList;

	public static Map<String, Double> mountPrice = new HashMap<String, Double>();
	public static Map<HorseColors, String> horseColorsMap = new HashMap<HorseColors, String>();
	public static Map<HorsePatterns, String> horsePatternsMap = new HashMap<HorsePatterns, String>();
	public static Map<LlamaColors, String> llamaColorsMap = new HashMap<LlamaColors, String>();
	public static Map<CarpetColors, String> carpetColorsMap = new HashMap<CarpetColors, String>();
	public static Map<MountNames, String> mountNames = new HashMap<MountNames, String>();
	public static IOManager ioManager;
	public static int minjump;
	public static int maxjump;
	public static double modjump;
	public static int minspeed;
	public static int maxspeed;
	public static double modspeed;
	
	
	public static void init(StablemasterPlugin plugin) throws Exception{
		economy = plugin.economy;
		//hasCitiTrader = plugin.hasCitiTrader;
		config = plugin.config;
		ioManager = new IOManager();
		
		langsetting = config.getString("lang");
		
		try {
			lang.load(new File(plugin.getDataFolder() + File.separator
					+ "lang_"+langsetting+".yml"));
		} catch (Exception e) {
			plugin.saveResource("lang_en.yml", true);
			plugin.saveResource("lang_fr.yml", true);
			plugin.saveResource("lang_de.yml", true);
			plugin.saveResource("lang_sv.yml", true);
			lang.load(plugin.getDataFolder() + File.separator + "lang_en.yml");
		}
		
		namesList = (lang.getString("stable.random-names")).split(", ");

		for (Action e : Action.values()) {
			messages.put(e, colorCodes(lang.getString("text." + e.toString())));
			if (config.contains("noble." + e.toString())) {
				nobleMsg.put(e, colorCodes(lang.getString("noble." + e.toString())));
			} else {
				nobleMsg.put(e, colorCodes(lang.getString("text." + e.toString())));
			}
		}
		
		for (Interface e: Interface.values()){
			if (lang.contains("titles." + e.toString())){
				interfaceTitle.put(e, colorCodes(lang.getString("titles." + e.toString())));
			} else {
				interfaceTitle.put(e, "");
			}
			
			if (lang.contains("interface." + e.toString())){
				interfaceText.put(e, colorCodes(lang.getString("interface." + e.toString())));
			} else {
				interfaceTitle.put(e, "");
			}
			
			if (lang.contains("nobletitles." + e.toString())){
				NinterfaceTitle.put(e, colorCodes(lang.getString("nobletitles." + e.toString())));
			} else {
				NinterfaceTitle.put(e, interfaceTitle.get(e));
			}
			
			if (lang.contains("nobleinterface." + e.toString())){
				NinterfaceText.put(e, colorCodes(lang.getString("nobleinterface." + e.toString())));
			} else {
				NinterfaceText.put(e,  interfaceText.get(e));
			}
		}
		
		for (MountNames e: MountNames.values()){
			if(lang.contains("mounts." + e.toString())){
				mountNames.put(e,  colorCodes(lang.getString("mounts." + e.toString())));
			} else {
				mountNames.put(e, e.toString()+" not found.");
			}
		}
		
		for (HorseColors e: HorseColors.values()){
			if(lang.contains("horsecolors." + e.toString())){
				horseColorsMap.put(e, colorCodes(lang.getString("horsecolors." + e.toString())));
			} else {
				horseColorsMap.put(e, e.toString()+" not found.");
			}
		}
		
		for (HorsePatterns e: HorsePatterns.values()){
			if(lang.contains("horsepatterns." + e.toString())){
				horsePatternsMap.put(e, colorCodes(lang.getString("horsepatterns." + e.toString())));
			} else {
				horsePatternsMap.put(e, e.toString()+" not found.");
			}
		}
		
		for (LlamaColors e: LlamaColors.values()){
			if(lang.contains("llamacolors." + e.toString())){
				llamaColorsMap.put(e, colorCodes(lang.getString("llamacolors." + e.toString())));
			} else {
				llamaColorsMap.put(e, e.toString()+" not found.");
			}
		}
		
		for (CarpetColors e: CarpetColors.values()){
			if(lang.contains("carpetcolors." + e.toString())){
				carpetColorsMap.put(e, colorCodes(lang.getString("carpetcolors." + e.toString())));
			} else {
				carpetColorsMap.put(e, e.toString()+" not found.");
			}
		}
        speed = lang.getString("stable.speed");
        jump = lang.getString("stable.jump");
        health = lang.getString("stable.health");
		paid = lang.getString("stable.paid");
	}
	
	// Enforce static class with private constructor
	private IOManager() {
		// Unpack the config.
		for (Costs e: Costs.values()){
			if(config.contains("mount-cost." + e.toString())){
				mountPrice.put(e.toString(), config.getDouble("mount-cost." + e.toString()));
			} else {
				mountPrice.put(e.toString(), (double) -1);
			}
		}
		
		for (Icons e: Icons.values()){
			if(config.contains("icons." + e.toString())){
				if(config.getString("icons." + e.toString()).contains(";")){
					String[] split = (config.getString("icons." + e.toString())).split(";");
					String material = split[0];
					String dataValue = split[1];
					Byte value = Byte.valueOf(dataValue);
					icons.put(e, new ItemStack(Material.valueOf(material), 1, (byte)value));
				}else{
					icons.put(e, new ItemStack(Material.valueOf(config.getString("icons." + e.toString())), 1));
				}
			} else {
				icons.put(e, new ItemStack(Material.AIR, 1));
			}
			
		}
		
		minspeed = config.getInt("stable.min-speed");
		maxspeed = config.getInt("stable.max-speed");
		modspeed = config.getDouble("stable.mod-speed");
		minjump = config.getInt("stable.min-jump");
		maxjump = config.getInt("stable.max-jump");
		modjump = config.getDouble("stable.mod-jump");
	}
	
	public static String getString(Interface inter, boolean noble){
		if(noble){
			return NinterfaceTitle.get(inter);
		} else {
			return interfaceTitle.get(inter);
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
	public static ItemStack makeButton(Icons icon, String option,
			String subtext) {
		return makeButton(icon, 1, option, subtext);
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
	public static ItemStack makeButton(Icons icon, int amt, String option,
			String subtext) {
		ItemStack button = icons.get(icon);
		button.setAmount(amt);
		ItemMeta im = button.getItemMeta();
		if (!option.equals(""))
			im.setDisplayName(option);
		if (!subtext.equals(""))
			im.setLore(Arrays.asList(subtext));
		button.setItemMeta(im);
		return button;
	}
	
	public static ItemStack makeButton(Icons icon, Interface inter, boolean noble) {
		return makeButton(icon, 1, inter, noble);
	}
	
	public static ItemStack makeButton(Icons icon, int amt, Interface inter, boolean noble) {
		ItemStack button = icons.get(icon);
		button.setAmount(amt);
		ItemMeta im = button.getItemMeta();
		String option, subtext;
		if (noble){
			option = NinterfaceTitle.get(inter);
			subtext = NinterfaceText.get(inter);
		} else {
			option = interfaceTitle.get(inter);
			subtext = interfaceText.get(inter);
		}
		im.setDisplayName(option);
		if(subtext != null){
			im.setLore(Arrays.asList(subtext));
		}
		button.setItemMeta(im);
		return button;
	}
	
	public static ItemStack makeButton(Icons icon, Interface inter, boolean noble, String price) {
		return makeButton(icon, 1, inter, noble, price);
	}
	
	public static ItemStack makeButton(Icons icon, int amt, Interface inter, boolean noble, String price) {
		ItemStack button = icons.get(icon);
		button.setAmount(amt);
		ItemMeta im = button.getItemMeta();
		String option, subtext;
		if (noble){
			option = NinterfaceTitle.get(inter).replace("<PRICE>", price);
			subtext = NinterfaceText.get(inter);
		} else {
			option = interfaceTitle.get(inter).replace("<PRICE>", price);
			subtext = interfaceText.get(inter);
		}
		im.setDisplayName(option);
		if(subtext != null){
			subtext.replace("<PRICE>", price);
			im.setLore(Arrays.asList(subtext));
		}
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
			return ChatColor.translateAlternateColorCodes('&', ("&2" + paid));
		if (player == null)
			return ChatColor.translateAlternateColorCodes('&', ("&9" + economy.format(cost)));
		if (canAfford(player, cost))
			return ChatColor.translateAlternateColorCodes('&', ("&6" + economy.format(cost)));
		return ChatColor.translateAlternateColorCodes('&', ("&c" + economy.format(cost)));
	}

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
	public static boolean charge(NPC npc, Player player, double cost) {
		if (canAfford(player, cost)) {
			economy.withdrawPlayer(player.getName(), cost);
//			if (hasCitiTrader) {
//				if (npc.hasTrait(WalletTrait.class)) {
//					npc.getTrait(WalletTrait.class).deposit(cost);
//				}
//			}
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

	@SuppressWarnings("deprecation")
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
			return "Horse";
		}
		if (vehicle.getType().equals(EntityType.MULE)){
			return "Mule";
		}
		if (vehicle.getType().equals(EntityType.DONKEY)){
			return "Donkey";
		}
		if (vehicle.getType().equals(EntityType.LLAMA)){
			return "Llama";
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

		return colorCodes(input);
	}
	
	private static String colorCodes(String input){
		// Color Codes
		for (int i = 0; i < 24; i++) {
			String temp = "" + i;
			if (i > 9) {
				temp = "" + (char) (87 + i);
			}
			input = input.replace("&" + temp, ChatColor.translateAlternateColorCodes('&', ("&" + temp)));
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
		 * Report the mount was kept.
		 */
		keep,
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
		pay,
		/**
		 * Report that no mounts are for sale.
		 */
		empty
	};
	
	public static enum Interface {
		paid,
		pickup,
		dropoff,
		pay,
		purchase,
		exit,
		opt,
		stable,
		acct,
		shop,
		settle,
		clear,
		acctexit,
		shoppurchase,
		shopexit,
		prevType,
		prevHealth,
		prevJump,
		prevSpeed,
		prevColor,
		prevPattern,
		prevCarpetColor,
		nextType,
		nextHealth,
		nextJump,
		nextSpeed,
		nextColor,
		nextPattern,
		nextCarpetColor
	};
	
	public static enum MountNames {
		pig,
		horse,
		mule,
		donkey,
		llama,
		skeleton,
		zombie
	};
	
	public static enum HorseColors {
		black,
		brown,
		chestnut,
		creamy,
		dark_brown,
		gray,
		white
	};
	
	public static enum HorsePatterns {
		plain,
		black_dots,
		white,
		white_dots,
		whitefield
	};
	
	public static enum LlamaColors {
		brown,
		creamy,
		gray,
		white
	};
	
	public static enum CarpetColors {
		nocarpet,
		black,
		blue,
		brown,
		cyan,
		gray,
		green,
		light_blue,
		lime,
		magenta,
		orange,
		pink,
		purple,
		red,
		silver,
		white,
		yellow
	};
	
	public static enum Icons {
		pickup,
		dropoff,
		pay,
		purchase,
		exit,
		settleDebt,
		clearBooks,
		exitPay,
		button,
		pig,
		horse,
		donkey,
		mule,
		llama,
		skeleton,
		zombie,
		health,
		jump,
		speed,
		black,
		brown,
		chestnut,
		creamy,
		dark_brown,
		gray,
		white,
		plain,
		black_dots,
		white_dots,
		whitefield,
		nocarpet,
		blue,
		cyan,
		green,
		light_blue,
		lime,
		magenta,
		orange,
		pink,
		purple,
		red,
		silver,
		yellow,
		buy,
		exitBuy,
		nil
	}
	
	private static enum Costs {
		pig,
		horse,
		donkey,
		mule,
		llama,
		skeleton,
		zombie,
		boat,
		cart,
		health,
		speed,
		jump,
		carpet,
		noble
	}

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
			this.pampered = lang.getString(this.name() + ".pampered");
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
