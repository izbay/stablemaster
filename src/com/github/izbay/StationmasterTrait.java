package com.github.izbay;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.github.izbay.IOManager.Action;
import com.github.izbay.StableMgr.StableAcct;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.trait.Trait;

public class StationmasterTrait extends Trait implements Listener {

	private StablemasterPlugin plugin;

	public StationmasterTrait() {
		super("stationmaster");
		plugin = (StablemasterPlugin) Bukkit.getServer().getPluginManager()
				.getPlugin("Stablemaster");
	}

	private static Map<String, StableMgr.StableAcct> stablemgr = StableMgr.stableMgr;
	private static Map<Player, Boolean> grabbed = new HashMap<Player, Boolean>();
	
	@EventHandler
	public void onRightClick(NPCRightClickEvent event) {
		if (this.npc != event.getNPC()) {
			return;
		}
		
		final Player player = event.getClicker();
		StableAcct acct;
		
		if (!stablemgr.containsKey(player.getName())){
			acct = plugin.sm.new StableAcct();
			stablemgr.put(player.getName(), acct);
		} else {
			acct = stablemgr.get(player.getName());
		}
		
		Entity vehicle = player.getVehicle();
		Location loc = this.npc.getStoredLocation();
		loc.add((player.getLocation().subtract(loc)).multiply(0.2));
		loc.setY(this.npc.getStoredLocation().getY());
		loc.setYaw(player.getLocation().getYaw());
		loc.setPitch(player.getLocation().getPitch());

		if (!player.hasPermission("stablemaster.station")) {
			IOManager.msg(player, Action.invalid, null);
		} else if (player.isInsideVehicle()
				&& player.getVehicle().getType().equals(EntityType.MINECART)) {

			if (acct.hasCartRoom()) {
				Double cost = IOManager.Traits.station.getPriceInit();
				if (player.hasPermission("stablemaster.noble.service")) {
					cost = (double) 0;
				}
				if (IOManager.charge(this.npc, player, cost)) {
					IOManager.msg(player, Action.stow, null);
					StableMgr.placeMap.put(this.npc.getId(),
							StableMgr.serializeLoc(vehicle.getLocation()));
					vehicle.eject();

					player.teleport(loc);
					vehicle.remove();
					acct.setCarts(acct.getCarts() + 1);
				}
			} else if (player.isInsideVehicle()) {
				IOManager.msg(player, Action.invalid, null);
			} else {
				IOManager.msg(player, Action.invalid, null);
			}
		} else {

			if (acct.getCarts() == 0) {
				IOManager.msg(player, Action.nil, null);
			} else if (!grabbed.containsKey(player)
					&& IOManager.Traits.station.getLocLog()
					&& StableMgr.placeMap.containsKey(this.npc.getId())) {
				acct.setCarts(acct.getCarts() - 1);
				Entity cart = player.getWorld().spawnEntity(
						StableMgr.deserializeLoc(StableMgr.placeMap
								.get(this.npc.getId())), EntityType.MINECART);
				grabbed.put(player, true);
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
						new Runnable() {
							public void run() {
								grabbed.remove(player);
							}
						}, (int) 2);
				IOManager.msg(player, Action.give, "cart");
				cart.setPassenger(player);
			} else {
				acct.setCarts(acct.getCarts() - 1);
				player.getWorld().dropItemNaturally(loc,
						new ItemStack(Material.MINECART, 1));
				IOManager.msg(player, Action.give, "cart");
			}
		}
	}
}
