package com.github.izbay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.citizensnpcs.api.CitizensAPI;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.izbay.StableMgr.Mount;
import com.github.izbay.StableMgr.StableAcct;
import com.google.common.io.Files;

/**
 * @author izbay
 * @version 1.09
 */
public class StablemasterPlugin extends JavaPlugin {

	public static StablemasterPlugin plugin;
	public FileConfiguration config;
	public Economy economy;
	public boolean hasCitiTrader = false;
	
	@Override
	public void onDisable() {
		saveStables();
		this.saveConfig();
	}
	
	@Override
	public void onEnable() {				
				
		// Load config
		this.saveDefaultConfig();
		config = this.getConfig();
		
		// If configured to do so, check the latest version on BukkitDEV and alert if user is out of date.
		if(config.getBoolean("check-update")){
			new Thread(new CheckUpdate(this)).start();
		}		
		
		File stable = new File(getDataFolder() + File.separator + "stable.bin");
		
		if(stable.exists()){
			try {
				StablemasterTrait.StableMgr = SLAPI.load(getDataFolder() + File.separator + "stable.bin");
				if(new File(getDataFolder() + File.separator + "placemap.bin").exists()){
					StableMgr.placeMap = SLAPI.load(getDataFolder() + File.separator + "placemap.bin");
				}
				config = this.getConfig();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			updateFileStructure();
		}
				
		// Check for Cititrader
		if (getServer().getPluginManager().getPlugin("CitiTrader") != null) {
			hasCitiTrader = true;
		}
				
		// Setup Vault
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null){
			economy = economyProvider.getProvider();
		} else {
					
			// Disable if no economy plug-in was found
			getLogger().log(Level.SEVERE, "Failed to load an economy plugin. Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		//	Register the traits!
		CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(StablemasterTrait.class).withName("stablemaster"));
		CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(WharfmasterTrait.class).withName("wharfmaster"));
		CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(StationmasterTrait.class).withName("stationmaster"));
		
		// Boot up the IOManager
		IOManager.init(this);
		
		// Save the stables on a repeating thread.
		long saveInterval = (long)(config.getInt("auto-save") * 1200);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable(){
			public void run(){
				saveStables();
			}
		}, saveInterval, saveInterval);
	}
	
	/**
	 * Attempts to save the current state of all player stables.
	 */
	private void saveStables(){
		getLogger().log(Level.INFO, "Saving player stables.");
		try {
			SLAPI.save(StablemasterTrait.StableMgr,getDataFolder() + File.separator + "stable.bin");
			SLAPI.save(StableMgr.placeMap, getDataFolder() + File.separator + "placemap.bin");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Updates the data save structure to new 1.1 format
	 * 
	 * A new data structure was required to support muliple mounts (of different types) 
	 * per player. This takes care of updating your current saves to the new format. 
	 * 
	 */
	private void updateFileStructure(){
		File stabled = new File(getDataFolder() + File.separator + "stabled.bin");
		File debt = new File(getDataFolder() + File.separator + "debt.bin");
		File cooldown = new File(getDataFolder() + File.separator + "cooldown.bin");
		File mounted = new File(getDataFolder() + File.separator + "mounted.bin");
		File haspig = new File(getDataFolder() + File.separator + "haspig.bin");
			
		if (stabled.exists() || debt.exists() || cooldown.exists() || mounted.exists() || haspig.exists()){
			
			// Old maps for conversion.

			Map<String, Long> hasStabled;
			Map<String, Integer> hasDebt;
			try {
				hasStabled = SLAPI.load(getDataFolder() + File.separator + "stabled.bin");
				hasDebt = SLAPI.load(getDataFolder() + File.separator + "debt.bin");
				StablemasterTrait.StableMgr = convert(hasStabled, hasDebt);
				
				// Move the old files.
				String outdatedPath = getDataFolder() + File.separator + "Outdated";
				File outdated = new File(outdatedPath);
				File configFile = new File(getDataFolder() + File.separator + "config.yml");
				outdated.mkdir();
				try {
					Files.move(configFile, new File(outdatedPath + File.separator + "config.yml"));
					Files.move(stabled, new File(outdatedPath + File.separator + "stabled.bin"));
					Files.move(cooldown, new File(outdatedPath + File.separator + "cooldown.bin"));
					Files.move(debt, new File(outdatedPath + File.separator + "debt.bin"));
					Files.move(mounted, new File(outdatedPath + File.separator + "mounted.bin"));
					Files.move(haspig, new File(outdatedPath + File.separator + "haspig.bin"));
				} catch (FileNotFoundException e){ }
				SLAPI.save(StablemasterTrait.StableMgr, getDataFolder() + File.separator + "stable.bin");
			} catch (Exception e) {
				e.printStackTrace();
			}
			getLogger().log(Level.SEVERE, "============IMPORTANT===========");
			getLogger().log(Level.SEVERE, "CONFIG FILE HAS BEEN REGENERATED.");
			getLogger().log(Level.SEVERE, "CURRENTLY USING DEFAULT VALUES.");
			getLogger().log(Level.SEVERE, "================================");
		}
	}
	
	/**
	 * Converts maps of old data structures into the new data structure.
	 * @param stabled Old data structure linking players to single pig.
	 * @param hasDebt Old data structure linking players to integer representation of debt.
	 * @return New data structure which holds all stable data for players. Allows for multiple mounts of different types.
	 */
	private Map<String, StableAcct> convert( Map<String, Long> stabled, Map<String, Integer> hasDebt){
		Map<String, StableAcct> result = new HashMap<String, StableAcct>();
		StableAcct temp;
		
		StableMgr sm = new StableMgr();
		
		if(!hasDebt.isEmpty()){
			for (Entry<String, Integer> entry : hasDebt.entrySet()){
				temp = sm.new StableAcct((double)((Integer)entry.getValue()));
				String newKey = entry.getKey().replace("CraftPlayer{name=", "").replace("}", "");
				result.put(newKey, temp);
			}
		}
		if(!stabled.isEmpty()){
			for (Map.Entry<String, Long> entry : stabled.entrySet()){
				String newKey = entry.getKey().replace("CraftPlayer{name=", "").replace("}", "");
				if(!(result.containsKey(entry.getKey()))){
					temp = sm.new StableAcct();
				} else {
					temp = result.get(newKey);
				}
				Mount piggie = sm.new Mount("Pig", EntityType.PIG, entry.getValue());
				temp.addMount(piggie);
				result.put(newKey, temp);
			}
		}
		return result;
	}
	
	/**	@author Tomsik68	*/
	public static class SLAPI {
		/**
		 * @param obj object which will be saved.
		 * @param path path to where the file will be created.
		 * @throws Exception if ObjectOutputStream fails.
		 */
		public static <T extends Object> void save(T obj, String path) throws Exception {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(obj);
			oos.flush();
			oos.close();
		}
		/**
		 * @param path path to the file that will be loaded from.
		 * @return object loaded from the file.
		 * @throws Exception if FileInputStream fails.
		 */
		public static <T extends Object> T load(String path) throws Exception {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			@SuppressWarnings("unchecked")
			T result = (T) ois.readObject();
			ois.close();
			return result;
		}
	}
}

