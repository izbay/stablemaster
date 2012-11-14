package com.github.izbay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.tehbeard.cititrader.WalletTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.izbay.util.Settings;




public class StablemasterPlugin extends JavaPlugin {
	public static StablemasterPlugin plugin;
	public Economy economy;
	private boolean hasCititrader = false;
	
	// Logger.
	public final Logger logger = Logger.getLogger("Minecraft");

	@Override
	public void onDisable() {
		try {
			SLAPI.save(StablemasterTrait.hasStabled, getDataFolder() + File.separator + "stabled.bin");
			SLAPI.save(StablemasterTrait.nobleCooldown, getDataFolder() + File.separator + "cooldown.bin");
			SLAPI.save(StablemasterTrait.hasDebt, getDataFolder() + File.separator + "debt.bin");
		} catch (Exception e) {
			e.printStackTrace();
		}
		getLogger().log(Level.INFO,
				" v" + getDescription().getVersion() + " disabled.");
	}

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		Settings config = new Settings(this);
		config.load();
		
		getServer().getPluginManager().registerEvents(new MountListener(null),this); 
		
		File s = new File(getDataFolder() + File.separator + "stabled.bin");
		File c = new File(getDataFolder() + File.separator + "cooldown.bin");
		File d = new File(getDataFolder() + File.separator + "debt.bin");
		if (s.exists() && c.exists() && d.exists()){
			try{
				StablemasterTrait.hasStabled = SLAPI.load(getDataFolder() + File.separator + "stabled.bin");
				StablemasterTrait.nobleCooldown = SLAPI.load(getDataFolder() + File.separator + "cooldown.bin");
				StablemasterTrait.hasDebt = SLAPI.load(getDataFolder() + File.separator + "debt.bin");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try{
			s.createNewFile();
			c.createNewFile();
			d.createNewFile();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		getServer().getPluginManager().registerEvents(new MountListener(null),
				this);

		// Check for Cititrader
		if (getServer().getPluginManager().getPlugin("CitiTrader") != null) {
			hasCititrader = true;
		}
		// Setup Vault
		RegisteredServiceProvider<Economy> economyProvider = getServer()
				.getServicesManager().getRegistration(Economy.class);
		if (economyProvider != null)
			economy = economyProvider.getProvider();
		else {
			// Disable if no economy plug-in was found
			getServer().getLogger().log(Level.SEVERE,
					"Failed to load an economy plugin. Disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		CitizensAPI.getTraitFactory().registerTrait(
				net.citizensnpcs.api.trait.TraitInfo.create(
						StablemasterTrait.class).withName("stablemaster"));

		getLogger().log(Level.INFO,
				" v" + getDescription().getVersion() + " enabled.");
	}

	public StablemasterTrait getStablemaster(NPC npc) {

		if (npc != null && npc.hasTrait(StablemasterTrait.class)) {
			return npc.getTrait(StablemasterTrait.class);
		}

		return null;
	}

	public boolean canAfford(Player player, Integer cost) {
		return economy.getBalance(player.getName()) >= cost;
	}

	public String econFormat(Integer cost) {
		return economy.format(cost);
	}

	public void charge(NPC npc, Player player, Integer cost) {
		economy.withdrawPlayer(player.getName(), cost);
		if (hasCititrader) {
			if (npc.hasTrait(WalletTrait.class)) {
				npc.getTrait(WalletTrait.class).deposit(cost);
			}
		}
	}

	public static class SLAPI {
		public static <T extends Object> void save(T obj, String path)
				throws Exception {
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream(path));
			oos.writeObject(obj);
			oos.flush();
			oos.close();
		}

		public static <T extends Object> T load(String path) throws Exception {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
					path));
			@SuppressWarnings("unchecked")
			T result = (T) ois.readObject();
			ois.close();
			return result;
		}
	}
}
