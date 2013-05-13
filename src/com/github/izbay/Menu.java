package com.github.izbay;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
 * @author izbay
 * The bulk of stablemaster logic is handled here. By using inventory to build an interface, navigation of interactions is more intuitive.
 */
public class Menu implements Listener{
	// Who is looking at the stable menu.
	private static HashMap<Player, PlayerMenu> menuMap = new HashMap<Player, PlayerMenu>();
	// Who is looking at the cash menu.
	private static HashMap<Player, CashMenu> cashMap = new HashMap<Player, CashMenu>();
	// For every player viewing a menu, which stablemaster did they speak to?
	private static Map<Player, NPC> npcMap = new HashMap<Player, NPC>();
	// stableMgr imported from the trait. All stable data for every player.
	private static Map<String, StableMgr.StableAcct> stableMgr;
	// Used to handle dynamically built menu of mounts for sale.
	private static Map<Integer, EntityType> buttonList = new HashMap<Integer, EntityType>();
	
	
	private static StableMgr sm = new StableMgr();
	private static Plugin plugin;
	private static Menu menu;
	private static Inventory rootMenu;

	// Enforce static class with private constructor
	private Menu(){
		Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);
		rootMenu.setItem(0, makeButton(Material.SADDLE, "§aPickup", "§fI'd like to pick up my mount!"));
		rootMenu.setItem(1, makeButton(Material.BED, "§aDropoff ("+IOManager.econFormat(null, IOManager.Traits.stable.getPriceInit())+"§a)", "§fWould you look after my mount?"));
		rootMenu.setItem(2, makeButton(Material.GOLD_INGOT, "§aPay", "§fI'd like to settle my balance."));

		//rootMenu.setItem(3, makeButton(Material.IRON_CHESTPLATE, "§aRemove Horse's Armor", "§fWould you help me with this?"));
		
		getSales(); // 4-7 will be buy options.
		rootMenu.setItem(8, makeButton(Material.WOOD_DOOR, "§aExit", "§fNothing today, thanks."));
	}
	
	/**
	 * 
	 * @param player Which player is opening the root menu.
	 * @param stablemgr StableMgr map from the Stablemaster Trait (all stable data).
	 * @param sm Instance of StableMgr class (not to be confused with the map of data).
	 * @param npc Which npc the player spoke to to open this menu.
	 */
	public static void openRoot(Player player, Map<String, StableMgr.StableAcct> stablemgr, Plugin sm, NPC npc){
		stableMgr = stablemgr;
		plugin = sm;
		if (rootMenu == null){
			
			rootMenu = Bukkit.createInventory(player, 9, "Stable Options");
			menu = new Menu();
		}
		npcMap.put(player, npc);
		player.openInventory(Menu.rootMenu);
	}
	/**
	 * This method opens the Stable GUI for the player and adds them to the viewer map.
	 * @param player The player opening Stable GUI
	 * @param stableAcct The StableAcct for the player.
	 */
	private static void openStableGUI(Player player, StableAcct stableAcct){
		menuMap.put(player, menu.new PlayerMenu(player, stableAcct));
	}
	/** This method removes the player from the viewer map.
	 * @param player The player being removed from Stable GUI.
	 */
	private static void closeStableGUI(Player player){
		menuMap.remove(player);
	}
	/**
	 * Creates a button for the GUI with a given Icon, title, and description.
	 * @param icon The material to be used for this button's icon.
	 * @param option The title of this button.
	 * @param subtext Any description to appear below the title of this button.
	 * @return Returns an ItemStack which can be used in the inventory as a button. Has option as the item name and subtext as the lore.
	 */
	private ItemStack makeButton(Material icon, String option, String subtext){
		ItemStack button = new ItemStack(icon, 1);
		ItemMeta im = button.getItemMeta();
		im.setDisplayName(option);
		im.setLore(Arrays.asList(subtext));
		button.setItemMeta(im);
		return button;
	}
	/**
	 * Goes through the values in mountPrice and places valid (non-negative price and living entity) mount types on the menu for sale.
	 */
	private void getSales(){
		int i = 7;
		for (Entry<EntityType, Integer> e: IOManager.mountPrice.entrySet()){
			if (e.getValue() >= 0 && e.getKey().isAlive()){
				switch(e.getKey()){
					case PIG:	rootMenu.setItem(i, makeButton(Material.CARROT_ITEM, "§aBuy Pig ("+IOManager.econFormat(null, e.getValue())+"§a)", "§fI'm looking to buy a pig."));
								buttonList.put(i, EntityType.PIG);
								break;
					//case HORSE:	
				}
				i--;
			}
		}
	}
	/**
	 * @param acct The account being queried.
	 * @return Returns true if enough time has elapsed since their last free mount for a Noble to be awarded another mount.
	 */
	private boolean checkCooldown(StableAcct acct){
		long elapsedTime = System.currentTimeMillis() - acct.getCooldown();
		return elapsedTime >= IOManager.Traits.stable.getCooldown() * (3600 * 1000);
	}
	/**
	 * Spawns a new mount halfway between the player and npc.
	 * @param player Player who is to recieve a mount.
	 * @param mount The mount object (name/type) that will be generated.
	 * @param npc The npc who is offering the mount.
	 */
    private void spawnMount(Player player, Mount mount, NPC npc){
		// Get the midpoint.
		Location location = npc.getBukkitEntity().getLocation();
		location.add(player.getLocation());
		location.multiply(0.5);
				
		LivingEntity newMount = (LivingEntity) player.getWorld().spawnEntity(location, mount.getType());
		if(!mount.getName().equals(newMount.getClass().getSimpleName().replace("Craft", ""))){
			newMount.setCustomName(mount.getName());
			newMount.setCustomNameVisible(true);
		}
		//temp
		((Pig)((Entity)newMount)).setSaddle(true);
	}

	@EventHandler
	void onInventoryClick(InventoryClickEvent event){
		boolean validSlot = (event.getRawSlot() <= 8 && event.getRawSlot() >= 0);
		if (event.getInventory().getTitle().equalsIgnoreCase("Stable Options") && validSlot){
			event.setCancelled(true);
			final Player player = (Player) event.getWhoClicked();
			
			switch (event.getRawSlot()){
				// Go to Stable
				case 0:	
						if (stableMgr.get(player.getName()).getNumMounts() > 0){
							Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
								public void run() {
									Menu.openStableGUI(player, stableMgr.get(player.getName()));
								}
							}, (long) .005);
						} else {
							IOManager.msg(player, Action.nil, null);
							player.closeInventory();
							npcMap.remove(player);
						}
						break;
				
				// Drop off mount
				case 1:	player.closeInventory();
						if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity){
							if (stableMgr.get(player.getName()).getDebt() > 0){
								StableAcct acct = stableMgr.get(player.getName());
								IOManager.msg(player,  Action.debt, acct);
							} else {
								LivingEntity vehicle = (LivingEntity) player.getVehicle();
								String name = IOManager.getVehicleName(player);
								StableAcct acct = stableMgr.get(player.getName());
								if (acct.hasRoom()){
									if(IOManager.charge(npcMap.get(player), player, IOManager.Traits.stable.getPriceInit())){
										IOManager.msg(player, Action.stow, null);
										vehicle.remove();
										acct.addMount(sm.new Mount(name, vehicle.getType(), System.currentTimeMillis()));
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
				case 2: Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
							public void run() {
								cashMap.put(player, new CashMenu(player, stableMgr.get(player.getName())));
							}
						}, (long) .005);
						break;	
						
				// Exit
				case 8: player.closeInventory();
						npcMap.remove(player);
						break;
						
				// Else: Buy something
				default: if(buttonList.containsKey(event.getRawSlot())){
							EntityType thisEntity = buttonList.get(event.getRawSlot());
							int price = IOManager.mountPrice.get(thisEntity);
							NPC npc = npcMap.get(player);
							StableAcct acct = stableMgr.get(player.getName());
							if (player.hasPermission("stablemaster.noble") && checkCooldown(acct)){
								Mount thisMount = sm.new Mount(IOManager.getSomeName(), thisEntity, System.currentTimeMillis());
								spawnMount(player, thisMount, npc);
								IOManager.msg(player, Action.free, thisMount);
								acct.setCooldown(System.currentTimeMillis());
							} else if(IOManager.charge(npc, player, price)){
								Mount thisMount = sm.new Mount(IOManager.getSomeName(), thisEntity, System.currentTimeMillis());
								spawnMount(player, thisMount, npc);
								IOManager.msg(player, Action.give, thisMount);
							}
							npcMap.remove(player);
							player.closeInventory();
							break;
				}
			}
		}
	}
	
	/**
	 * @author izbay
	 * Used to traverse player stables
	 */
	private class PlayerMenu implements Listener {
		private String name;
		private int size;
		private StableAcct acct;
		/**
		 * Stable GUI constructor.
		 * @param player Player who is viewing this stable.
		 * @param stableAcct Cooresponding account of the player.
		 */
		public PlayerMenu(Player player, StableAcct stableAcct){
			// Correct use of apostrophe for the name.
			if(player.getName().toLowerCase().endsWith("s")){
				name = player.getName() + "' Stable";
			} else {
				name = player.getName() + "'s Stable";
			}
			
			// Number of mounts
			int maxSize = (int)(Math.ceil((double)(IOManager.Traits.stable.getMaxMounts()) / 9) * 9);
			int currSize = (int)(Math.ceil((double)(stableAcct.getNumMounts()) / 9) * 9);
			if(maxSize == 0 || currSize < maxSize){
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
		 * @param player Player who is viewing this stable.
		 */
		private void open(Player player){
			Inventory menu = Bukkit.createInventory(player, size, name);
			for (int i = 0; i < acct.getNumMounts(); i++){
				Mount thisMount = acct.getMount(i);
				menu.setItem(i, buildItem(player, thisMount));
			}
			player.openInventory(menu);
		}
		/**
		 * Creates a button representing a mount in the Stable GUI
		 * @param player Player who's Stable GUI this is being generated for.
		 * @param mount Mount which this button represents.
		 * @return Returns an ItemStack which represents a mount for the Stable GUI. Displays name of mount and how much is owed to reacquire it.
		 */
		private ItemStack buildItem(Player player, Mount mount){
			
			ItemStack button = new ItemStack(Material.EGG, 1);
			
			if (mount.getType().equals(EntityType.PIG)){
				button = new ItemStack(Material.CARROT_ITEM, 1);
			}
//			else if (mount.getType().equals(EntityType.HORSE)){
//				button = new ItemStack(Material.SADDLE, 1);
//			} else if (mount.getType().equals(EntityType.MULE)){
//				button = new ItemStack(Material.CHEST, 1);
//			} else if (mount.getType().equals(EntityType.DONKEY)){
//				button = new ItemStack(Material.APPLE, 1);
//			}
			
			ItemMeta im = button.getItemMeta();
			im.setDisplayName(mount.getName());
			im.setLore(Arrays.asList(IOManager.econFormat(player, IOManager.getCost(player, mount))));
			button.setItemMeta(im);
			return button;
		}
	    /**
	     * Self destructs this menu and removes player from npcMap.
	     * @param player Player who is exiting this menu.
	     */
	    private void killMenu(final Player player){
	    	Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    player.closeInventory();
                    npcMap.remove(player);
                    Menu.closeStableGUI(player);
                }
            }, 1);
	    	HandlerList.unregisterAll(this);
	    }
	    @SuppressWarnings("unused")
		@EventHandler
	    void onInventoryClick(InventoryClickEvent event){
	    	boolean validSlot = (event.getRawSlot() < acct.getNumMounts() && event.getRawSlot() >= 0);
	    	
	    	if (event.getInventory().getTitle().equals(name) && validSlot){
	    		event.setCancelled(true);
	    		
	    		Player player = (Player) event.getWhoClicked();
	    		StableAcct acct = stableMgr.get(player.getName());
	    		Mount selected = acct.getMount(event.getRawSlot());
	    		IOManager.pickUpCharge(npcMap.get(player), player, acct, IOManager.getCost(player, selected), selected);
	    		spawnMount(player, selected, npcMap.get(player));
	    		
	    		acct.removeMount(event.getRawSlot());
	    		
	    		killMenu(player);
	    	}
	    }
	    
	    @SuppressWarnings("unused")
		@EventHandler
	    void onInventoryClose(InventoryCloseEvent event){
	    	if (event.getInventory().getTitle().equals(name)){
	    		Player player = (Player) event.getPlayer();
	    		killMenu(player);
	    	}
	    }
	}

	/**
	 * @author izbay
	 * Used to show users their balances and allow them to pay.
	 */
	private class CashMenu implements Listener {
		private String name;
		private StableAcct acct;
		/**
		 * Constructor for CashMenu GUI.
		 * @param player Player who is viewing the CashMenu.
		 * @param stableAcct Cooresponding StableAcct.
		 */
		public CashMenu(Player player, StableAcct stableAcct){
			// Correct use of apostrophe for the name.
			if(player.getName().toLowerCase().endsWith("s")){
				name = player.getName() + "' Account";
			} else {
				name = player.getName() + "'s Account";
			}
			acct = stableAcct;
			Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);
			
			Inventory cashMenu = Bukkit.createInventory(player, 9, name);
			cashMenu.setItem(0, makeButton(Material.FEATHER, "§aSettle Debt ("+IOManager.econFormat(player, acct.getDebt())+"§a)", "§fI make good on my word!"));
			cashMenu.setItem(1, makeButton(Material.BOOK, "§aClear the books ("+IOManager.printTotalCost(player, acct)+"§a)", "§fI'd like to pay early."));
			cashMenu.setItem(8, makeButton(Material.WOOD_DOOR, "§aExit", "§fNevermind!"));
			player.openInventory(cashMenu);
		}
	    /**
	     * Self destructs this menu and removes player from npcMap.
	     * @param player Player who is exiting this menu.
	     */
		private void killMenu(final Player player){
	    	Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run() {
                    Menu.openRoot(player, stableMgr, plugin, npcMap.get(player));
                }
            }, 1);
	    	HandlerList.unregisterAll(this);
	    }
	    
	    @SuppressWarnings("unused")
		@EventHandler
		void onInventoryClick(InventoryClickEvent event){
			boolean validSlot = (event.getRawSlot() <= 8 && event.getRawSlot() >= 0);
			if (event.getInventory().getTitle().contains("Account") && validSlot){
				event.setCancelled(true);
				final Player player = (Player) event.getWhoClicked();
				
				switch (event.getRawSlot()){
					
					// Pay Debt
					case 0: if(IOManager.charge(npcMap.get(player), player, acct.getDebt())){
								IOManager.msg(player, Action.paid, null);
								acct.setDebt(0);
							}
							killMenu(player);
							break;
						
					// Pay Balance
					case 1: if(IOManager.charge(npcMap.get(player), player, IOManager.getTotalCost(player, acct))){
								IOManager.msg(player, Action.paid, null);
								long newTime = System.currentTimeMillis();
								for(int i=0; i<acct.getNumMounts(); i++){
									if (newTime - acct.getMount(i).getTime() > (86400 * 1000)){
										acct.getMount(i).setTime(newTime);
									}
								}
							}
							killMenu(player);
							break;
							
					// Exit
					case 8: killMenu(player);
							break;
				}
			}
	    }
	}
}
