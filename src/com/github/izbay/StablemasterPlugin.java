package com.github.izbay;

import java.util.logging.Level;
import java.util.logging.Logger;

import me.tehbeard.cititrader.WalletTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class StablemasterPlugin extends JavaPlugin {
	public static StablemasterPlugin plugin;
	public Economy economy;
	private boolean hasCititrader = false;

	// Logger.
	public final Logger logger = Logger.getLogger("Minecraft");

	@Override
	public void onDisable() {
		getLogger().log(Level.INFO,
				" v" + getDescription().getVersion() + " disabled.");
	}

	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		
		getServer().getPluginManager().registerEvents(new
		MountListener(null),this); 

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

		CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(StablemasterTrait.class).withName("stablemaster"));

		getLogger().log(Level.INFO,
				" v" + getDescription().getVersion() + " enabled.");
	}

	public StablemasterTrait getStablemaster(NPC npc){

		if (npc !=null && npc.hasTrait(StablemasterTrait.class)){
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
	// Read Config
	public String costMsg = StablemasterPlugin.this.getConfig().getString("text.base");
	public String insufficientFundsMsg = StablemasterPlugin.this.getConfig().getString("text.funds");
	public String invalidMountMsg = StablemasterPlugin.this.getConfig().getString("text.invalid");
	public String alreadyMountMsg = StablemasterPlugin.this.getConfig().getString("text.already");
	public String saleMsg = StablemasterPlugin.this.getConfig().getString("text.sale");
	public String giveMsg = StablemasterPlugin.this.getConfig().getString("text.give");
	public String shortMsg = StablemasterPlugin.this.getConfig().getString("text.short");
	public String debtMsg = StablemasterPlugin.this.getConfig().getString("text.debt");
	public String paidMsg = StablemasterPlugin.this.getConfig().getString("text.paid");
	public String stowMsg = StablemasterPlugin.this.getConfig().getString("text.stow");
	public String takeMsg = StablemasterPlugin.this.getConfig().getString("text.take");
	public String take2Msg = StablemasterPlugin.this.getConfig().getString("text.take2");
	public String nobleOfferMsg = StablemasterPlugin.this.getConfig().getString("noble.offer");
	public String nobleStowMsg = StablemasterPlugin.this.getConfig().getString("noble.stow");
	public String nobleFreeMsg = StablemasterPlugin.this.getConfig().getString("noble.free");
	public String nobleTakeMsg = StablemasterPlugin.this.getConfig().getString("noble.take");
	public String nobleDenyMsg = StablemasterPlugin.this.getConfig().getString("noble.deny");

	public Integer basePrice = StablemasterPlugin.this.getConfig().getInt("values.base");
	public Integer pricePerDay = StablemasterPlugin.this.getConfig().getInt("values.perday");
	public Integer pigBasePrice = StablemasterPlugin.this.getConfig().getInt("values.pig");

	public Double nobleCooldownLen = StablemasterPlugin.this.getConfig().getDouble("values.cooldown");
}
