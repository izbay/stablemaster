package com.github.izbay;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
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

public class WharfmasterTrait extends Trait implements Listener {

	private StablemasterPlugin plugin;
	
	public WharfmasterTrait() {
		super("wharfmaster");
		plugin = (StablemasterPlugin) Bukkit.getServer().getPluginManager()
				.getPlugin("Stablemaster");
	}

	private static Map<String, StableMgr.StableAcct> Stablemgr = StablemasterTrait.StableMgr;
	private static Map<Player, Boolean> grabbed = new HashMap<Player, Boolean>();
	
	@EventHandler
	public void onRightClick(NPCRightClickEvent event) {
		
		if (this.npc != event.getNPC()){
			return;
		}
		final Player player = event.getClicker();
		StableAcct acct = Stablemgr.get(player.getName());
		Entity vehicle = player.getVehicle();
		Location loc = this.npc.getBukkitEntity().getLocation();
		loc.add((player.getLocation().subtract(loc)).multiply(0.2));
		loc.setY(this.npc.getBukkitEntity().getLocation().getY());
		loc.setYaw(player.getLocation().getYaw());
		loc.setPitch(player.getLocation().getPitch());
		
		if (!player.hasPermission("stablemaster.wharf")) {
			IOManager.msg(player, Action.invalid, null);
		} else if (player.isInsideVehicle() && player.getVehicle() instanceof Boat) {
			
			if (acct.hasBoatRoom()) {
				if (IOManager.charge(this.npc, player, IOManager.Traits.wharf.getPriceInit())) {
					IOManager.msg(player, Action.stow, null);
					StableMgr.placeMap.put(this.npc.getId(), StableMgr.serializeLoc(vehicle.getLocation()));
					vehicle.eject();

					player.teleport(loc);
					vehicle.remove();
					acct.setBoats(acct.getBoats() + 1);
				}
			} else {
				IOManager.msg(player, Action.full, null);
			}
		} else if (player.isInsideVehicle()) {
			IOManager.msg(player, Action.invalid, null);
		} else {
				
			if (acct.getBoats() == 0) {
				IOManager.msg(player, Action.nil, null);
			} else if (!grabbed.containsKey(player) && IOManager.Traits.wharf.getLocLog() && StableMgr.placeMap.containsKey(this.npc.getId())){
				acct.setBoats(acct.getBoats() - 1);
				Entity boat = player.getWorld().spawnEntity(StableMgr.deserializeLoc(StableMgr.placeMap.get(this.npc.getId())), EntityType.BOAT);
				grabbed.put(player, true);
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						grabbed.remove(player);
					}
				}, (int) 2);
				IOManager.msg(player, Action.give, boat);
				boat.setPassenger(player);
			} else {
				acct.setBoats(acct.getBoats() - 1);
				player.getWorld().dropItemNaturally(loc, new ItemStack(Material.BOAT, 1));
				IOManager.msg(player, Action.give, "boat");
			}
		}
	}
}

