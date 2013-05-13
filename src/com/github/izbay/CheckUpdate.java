package com.github.izbay;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
 
public class CheckUpdate implements Runnable {
	
	private Logger logger;
	private String version;
	
	/** Constructor.
	 * @param plugin The instance of this plugin.
	 */
	public CheckUpdate(StablemasterPlugin plugin){
		logger = plugin.getLogger();
		version = plugin.getDescription().getVersion();
	}
	
	/**
	 * Check for an update in a new thread. This is to prevent slowdown at boot while parsing HTML.
	 * Promts the user if a new version exists by checking the page on BukkitDEV.
	 */
	public void run(){
		URL url;
		URLConnection yc;
		Scanner in = null;
		
		// Attempt to connect to the BukkitDev site.
		try {
			url = new URL("http://dev.bukkit.org/server-mods/stablemaster/files/");
			yc = url.openConnection();
			in = new Scanner(new InputStreamReader(yc.getInputStream()));
		} catch (IOException e) {
			logger.log(Level.SEVERE, "**************************************");
			logger.log(Level.SEVERE, "UNABLE TO CONFIRM PLUGIN IS UP TO DATE");
			logger.log(Level.SEVERE, "PESTER IZBAY ON BUKKITDEV ABOUT THIS!");
			logger.log(Level.SEVERE, "**************************************");
			return;
		}
    	
    	// Scan for the first instance of stablemaster version on the page.
		String inputLine = null;
	    while(in.hasNext()){    
	    	inputLine = in.next();
	    	if (inputLine != null && inputLine.contains("stablemaster-v")){
	    		break;
	    	}
	    }
	    in.close();
	    
	    // Format the string for conversion to a float.
	    String breakdown[] = inputLine.split("-");
	    String formattedVersion = breakdown[3].substring(1) + "." + breakdown[4].substring(0, 2);
	    
		Float thisVersion = Float.parseFloat(version);
	    Float currVersion = Float.parseFloat(formattedVersion);
	    
		if (currVersion > thisVersion){
			logger.log(Level.SEVERE, "*****************************************");
			logger.log(Level.SEVERE, "YOUR VERSION IS NO LONGER THE MOST RECENT");
			logger.log(Level.SEVERE, "GET THE LATEST BUILD, " + currVersion + ", ON BUKKITDEV!");
			logger.log(Level.SEVERE, "bit.ly/stablemaster");
			logger.log(Level.SEVERE, "*****************************************");
		}
	}
}