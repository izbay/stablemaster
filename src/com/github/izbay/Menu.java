package com.github.izbay;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
 * @author izbay
 * The bulk of stablemaster logic is handled here. By using inventory to build an interface, navigation of interactions is more intuitive.
 */
public class Menu implements Listener{
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
	//private static Map<Integer, EntityType> buttonList = new HashMap<Integer, EntityType>();
	
	
	private static StableMgr sm = new StableMgr();
	private static Plugin plugin;
	private static Menu menu;
	private static Inventory rootMenu;

	// Enforce static class with private constructor
	private Menu(){
		Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);
		rootMenu.setItem(0, IOManager.makeButton(Material.SADDLE, "§aPickup", "§fI'd like to pick up my mount!"));
		rootMenu.setItem(1, IOManager.makeButton(Material.BED, "§aDropoff ("+IOManager.econFormat(null, IOManager.Traits.stable.getPriceInit())+"§a)", "§fWould you look after my mount?"));
		rootMenu.setItem(2, IOManager.makeButton(Material.GOLD_INGOT, "§aPay", "§fI'd like to settle my balance."));
		rootMenu.setItem(3, IOManager.makeButton(Material.PAPER, "§aPurchase", "§fHave you any mounts for sale?"));
		
		getSales(); // 4-7 will be buy options.
		
		rootMenu.setItem(8, IOManager.makeButton(Material.WOOD_DOOR, "§aExit", "§fNothing today, thanks."));
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
	 * Goes through the values in mountPrice and places valid (non-negative price and living entity) mount types on the menu for sale.
	 */
	private void getSales(){
//		int i = 7;
//		for (Entry<EntityType, Integer> e: IOManager.mountPrice.entrySet()){
//			if (e.getValue() >= 0 && e.getKey().isAlive()){
//				switch(e.getKey()){
//					case PIG:	rootMenu.setItem(i, IOManager.makeButton(Material.CARROT_ITEM, "§aBuy Pig ("+IOManager.econFormat(null, e.getValue())+"§a)", "§fI'm looking to buy a pig."));
//								buttonList.put(i, EntityType.PIG);
//								break;
//					//case HORSE:
//					default:	break;
//				}
//				i--;
//			}
//		}
	}
	/**
	 * @param acct The account being queried.
	 * @return Returns true if enough time has elapsed since their last free mount for a Noble to be awarded another mount.
	 */
	@SuppressWarnings("unused")
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
		location.add((player.getLocation().subtract(location)).multiply(0.8));
		location.setPitch(player.getLocation().getYaw()+180);
		location.setYaw(0);
				
		LivingEntity newMount = (LivingEntity) player.getWorld().spawnEntity(location, mount.getType());
		newMount.setRemoveWhenFarAway(false);
		if(!mount.getName().equals(newMount.getClass().getSimpleName().replace("Craft", "")) && !mount.getName().equals("Donkey") && !mount.getName().equals("Mule")){
			newMount.setCustomName(mount.getName());
			newMount.setCustomNameVisible(true);
		}
		
		if(mount.getType().equals(EntityType.PIG)){
			((Pig) newMount).setSaddle(true);
    	
		} else if(mount.getType().equals(EntityType.HORSE)){
			((Horse) newMount).setTamed(true);
			((Horse) newMount).setVariant(mount.getVariant());
			((Horse) newMount).setColor(mount.getColor());
			((Horse) newMount).setStyle(mount.getStyle());
			((Horse) newMount).setMaxHealth(mount.getHealth());
			((Horse) newMount).setJumpStrength(mount.getJumpstr());
			((Horse) newMount).setCarryingChest(mount.HasChest());
			if(mount.HasChest()){
				((Horse) newMount).getInventory().setContents(mount.getInventory());
			}
			((Horse) newMount).getInventory().setSaddle(new ItemStack(Material.SADDLE));
			((Horse) newMount).getInventory().setArmor(mount.getArmor());
			((Horse) newMount).setTamed(true);
		}
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
										if (vehicle.getType().equals(EntityType.HORSE)){
											acct.addMount(sm.new Mount(name, vehicle.getType(), System.currentTimeMillis(), (Horse)vehicle));
										} else {
											acct.addMount(sm.new Mount(name, vehicle.getType(), System.currentTimeMillis()));
										}
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
				// Buy a mount
				case 3: Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
							public void run() {
								buyMap.put(player, new BuyMenu(player, stableMgr.get(player.getName()), npcMap.get(player)));
							}
						}, (long) .005);
						break;
				// Exit
				case 8: player.closeInventory();
						npcMap.remove(player);
						break;
				default: break;		
				/** Else: Buy something
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
				} */
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
		 * @param stableAcct Corresponding account of the player.
		 */
		public PlayerMenu(Player player, StableAcct stableAcct){
				name = player.getName() + "'s Stable";
			
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
			
			} else if (mount.getType().equals(EntityType.HORSE)){
				
				if(mount.getVariant().equals(Horse.Variant.HORSE)){
					button = new ItemStack(Material.SADDLE, 1);
				
				} else if(mount.getVariant().equals(Horse.Variant.MULE)){
					button = new ItemStack(Material.CHEST, 1);
				
				} else if(mount.getVariant().equals(Horse.Variant.DONKEY)){
					button = new ItemStack(Material.APPLE, 1);

				} else if(mount.getVariant().equals(Horse.Variant.SKELETON_HORSE)){
					button = new ItemStack(Material.BONE, 1);
				
				} else {//It's a zombie.
					button = new ItemStack(Material.ROTTEN_FLESH, 1);
				}
			}
			
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
		 * @param stableAcct Corresponding StableAcct.
		 */
		public CashMenu(Player player, StableAcct stableAcct){
			name = player.getName() + "'s Account";
			acct = stableAcct;
			Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);
			
			Inventory cashMenu = Bukkit.createInventory(player, 9, name);
			cashMenu.setItem(0, IOManager.makeButton(Material.FEATHER, "§aSettle Debt ("+IOManager.econFormat(player, acct.getDebt())+"§a)", "§fI make good on my word!"));
			cashMenu.setItem(1, IOManager.makeButton(Material.BOOK, "§aClear the books ("+IOManager.printTotalCost(player, acct)+"§a)", "§fI'd like to pay early."));
			cashMenu.setItem(8, IOManager.makeButton(Material.WOOD_DOOR, "§aExit", "§fNevermind!"));
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
	
	/**
	 * @author izbay
	 * Used to design mounts and purchase them.
	 */
	@SuppressWarnings("unused")
	private class BuyMenu implements Listener {
		private String name;
		private StableAcct acct;
		private NPC npc;
		
		//private Mount mount = sm.new Mount();
		// As much as I'd rather use enums, I need to use modulus, so these are ints.
		private int type = 0;
		private int armor = 0;
		private int jump = 0;
		private int speed = 0;
		private int color = 0;
		private int style = 0;
		private Inventory menu;
		
		/**
		 * Constructor for BuyMenu GUI.
		 * @param player Player who is viewing the CashMenu.
		 * @param stableAcct Corresponding StableAcct.
		 * @param npc The NPC that was being spoken to.
		 */
		public BuyMenu(Player player, StableAcct stableAcct, NPC npc){
			name = "Purchase a Mount";
			acct = stableAcct;
			this.npc = npc;
			Menu.plugin.getServer().getPluginManager().registerEvents(this, Menu.plugin);
			menu = buildMenu(player);
			player.openInventory(menu);
		}
		
		private Inventory buildMenu(Player player){
			Inventory cashMenu = Bukkit.createInventory(player, 27, name);
			cashMenu.setItem(0, IOManager.makeButton(Material.STONE_BUTTON, "§aPrevious", "§fType"));
			cashMenu.setItem(4, IOManager.makeButton(Material.STONE_BUTTON, "§aPrevious", "§fEquip"));
			cashMenu.setItem(18, IOManager.makeButton(Material.STONE_BUTTON, "§aPrevious", "§fJump"));
			cashMenu.setItem(22, IOManager.makeButton(Material.STONE_BUTTON, "§aPrevious", "§fSpeed"));
			cashMenu.setItem(9, IOManager.makeButton(Material.STONE_BUTTON, "§aPrevious", "§fColor"));
			cashMenu.setItem(13, IOManager.makeButton(Material.STONE_BUTTON, "§aPrevious", "§fPattern"));
			
			cashMenu.setItem(2, IOManager.makeButton(Material.STONE_BUTTON, "§aNext", "§fType"));
			cashMenu.setItem(6, IOManager.makeButton(Material.STONE_BUTTON, "§aNext", "§fEquip"));
			cashMenu.setItem(20, IOManager.makeButton(Material.STONE_BUTTON, "§aNext", "§fJump"));
			cashMenu.setItem(24, IOManager.makeButton(Material.STONE_BUTTON, "§aNext", "§fSpeed"));
			cashMenu.setItem(11, IOManager.makeButton(Material.STONE_BUTTON, "§aNext", "§fColor"));
			cashMenu.setItem(15, IOManager.makeButton(Material.STONE_BUTTON, "§aNext", "§fPattern"));
			
			// Now the indicators
			cashMenu.setItem(1, IOManager.makeButton(Material.CARROT_ITEM, 1, "§aPig", "§fPrice"));
			cashMenu.setItem(5, IOManager.makeButton(Material.GLASS, 1, "§aNone",""));
			cashMenu.setItem(10, IOManager.makeButton(Material.QUARTZ_BLOCK, 1, "§aWhite",""));
			cashMenu.setItem(14, IOManager.makeButton(Material.GLASS, 1, "§aPlain",""));
			return cashMenu;
		}
		
		private void updateType(){
			switch(type){
			case 0: menu.setItem(1, IOManager.makeButton(Material.CARROT_ITEM, 1, "§aPig", "§fPrice"));
					break;
			case 1: menu.setItem(1, IOManager.makeButton(Material.SADDLE, 2, "§aHorse", "§fPrice"));
					break;
			case 2: menu.setItem(1, IOManager.makeButton(Material.CHEST, 3, "§aMule", "§fPrice"));
					break;
			case 3: menu.setItem(1, IOManager.makeButton(Material.APPLE, 4, "§aDonkey", "§fPrice"));
					break;
			}
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
	    
		@EventHandler
		void onInventoryClick(InventoryClickEvent event){
			boolean validSlot = (event.getRawSlot() <= 26 && event.getRawSlot() >= 0);
			if (event.getInventory().getTitle().contains("Purchase") && validSlot){
				event.setCancelled(true);
				final Player player = (Player) event.getWhoClicked();
				
				switch (event.getRawSlot()){
					
					// Previous mount type
					case 0: 
							type = ((((type - 1) % 4)+4)%4);
							if(type==0){
								jump = 0;
								speed = 0;
							} else if(type!=1){
								armor = 0;
								color = 0;
								style = 0;
							}
							updateType();
							break;
					// Next mount type
					case 2: type = (type + 1) % 4;
							if(type==0){
								jump = 0;
								speed = 0;
							} else if(type!=1){
								armor = 0;
								color = 0;
								style = 0;
							}
							updateType();
							break;
					// Previous armor
					case 4: if(type==1){
								armor = ((((armor - 1)%4)+4)%4);
							} else if(type!=0) {
								armor = (armor - 1)%2;
							}
							break;
					// Next armor
					case 6: if(type==1){
								armor = (armor + 1)%4;
							} else if(type!=0) {
								armor = (armor + 1)%2;
							}
							break;
					// Previous jump
					case 9: if(type!=0){
								jump = ((((jump - 1)%30)+30)%30);
							}
							break;
					// Next jump
					case 11: if(type !=0){
								jump = (jump + 1)%30;
							}
							break;
					// Previous speed
					case 13: if(type!=0){
								speed = ((((speed - 1)%30)+30)%30);
							}
							break;
					// Next speed
					case 15: if(type !=0){
								speed = (speed + 1)%30;
							}
							break;	
					// Previous color
					case 18: if(type==1){
								color = ((((color - 1)%7)+7)%7);
							}
							break;
					// Next color
					case 20: if(type==1){
								color = (color + 1)%7;
							}
							break;
					// Previous style
					case 22: if(type==1){
								style = ((((style - 1)%5)+5)%5);
							}
							break;
					// Next style
					case 24: if(type==1){
								style = (style + 1)%5;
							}
							break;

				}
				// refresh
			}
	    }
	}
}
