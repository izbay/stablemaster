package com.github.izbay.stablemaster;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.github.izbay.stablemaster.IOManager.Action;
import com.github.izbay.stablemaster.StableMgr.StableAcct;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;

/**
 * @author izbay
 * Handles the Stablemaster Trait. Most behavior is handled through Menu and interaction through IOManager.
 */
public class StablemasterTrait extends Trait implements Listener {
	
	private StablemasterPlugin plugin;
	private StableMgr sm = new StableMgr();
	
	public StablemasterTrait() {
		super("stablemaster");
		plugin = (StablemasterPlugin)Bukkit.getServer().getPluginManager().getPlugin("Stablemaster");
	}
	
	public static Map<String, StableMgr.StableAcct> stableMgr = StableMgr.stableMgr;
	
	// Tracks if you've talked to a stablemaster recently (to prevent spam).
	public static Map<Player, NPC> talked = new HashMap<Player, NPC>();
	
	@EventHandler
	public void onRightClick(NPCRightClickEvent event){
		if (this.npc != event.getNPC())
			return;
		final Player player = event.getClicker();
		
		if(!player.hasPermission("stablemaster.stable")){
			IOManager.msg(player, Action.invalid, null);
		} else {
			if(!stableMgr.containsKey(player.getName())){
				StableAcct acct = sm.new StableAcct();
				stableMgr.put(player.getName(), acct);
			}
			if(!talked.containsKey(player)){
				talked.put(player, this.npc);
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
					public void run(){
						talked.remove(player);
					}
				}, 300L);
				IOManager.msg(player, Action.greet, null);
			}
			Menu.openRoot(player, stableMgr, plugin, event.getNPC());
		}
	}
}
