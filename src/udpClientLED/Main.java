package udpClientLED;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.io.FileOutputStream;
//import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
//import org.bukkit.configuration.file.FileConfiguration;
//import org.bukkit.configuration.file.YamlConfiguration;

public final class Main extends JavaPlugin implements Listener {
	
	private short local = 0;
	private short notLocal = 0;
	// private boolean recentJoin = false;
	InetAddress recentPlayerIP = null;
	// private String recentPlayerIP = "";
	// Don't initialise as this is set by file or config defaults
	private static String udpIPAddress;
	// Default timeout for checking new udp connection
	private static int timeout;
	// Default timeout for checking known good connections
	private static int shortTimeout;
	 
	@Override
    public void onEnable() {
		// register listener
		getServer().getPluginManager().registerEvents(this, this);
		// Load or initialise configuration file
		loadConfiguration();
		getLogger().info("Returned from loadConfiguration()");
		String IPAddressLoaded = this.getConfig().getString("LEDIPAddress.IPAddress");
		getLogger().info("loadedIPAddress is set to " + IPAddressLoaded);
		// Indicate that plugin has started with a light display
		udpTransmit ("Funky Disco");
		getLogger().info("udpClientLED is switched on."); 
		udpTransmit ("Red On");
		//TODO Check if any players are already logged in and adjust local count accordingly
	}
 
    @Override
    public void onDisable() {
        //Switch off all LEDs
    	udpTransmit ("All Off");
        getLogger().info("udpClientLED has been extinguished.");
    }
    
    // Someone joins server
    @EventHandler
    public void onLogin(PlayerJoinEvent event) throws UnknownHostException {
    	// Check whether internal or external IP address
    	// recentPlayerIP = event.getPlayer().getAddress().getHostString();
    	recentPlayerIP = event.getPlayer().getAddress().getAddress();   	
    	if(isLocal(recentPlayerIP)) {
    		local++;
    		event.getPlayer().sendMessage(event.getPlayer().getName() +" is local.");
    	} else {
    		notLocal++;
    		event.getPlayer().sendMessage(event.getPlayer().getName() +" is not local.");
    	}
    	//recentJoin = true;
    	// isLocal();
    	// Update local/notLocal LED status according
    	updateLED();
    }
    
    // Someone leaves server
    @EventHandler
    public void onLogout(PlayerQuitEvent event) throws UnknownHostException {
    	// Check whether internal or external IP address
    	// recentPlayerIP = event.getPlayer().getAddress().getHostString();
    	recentPlayerIP = event.getPlayer().getAddress().getAddress();   	
    	if(isLocal(recentPlayerIP)) {
    		local--;
    		
    	} else {
    		notLocal--;
    	}
    	//recentJoin = false;
    	//isLocal();
    	// Update local/notLocal LED status according
    	updateLED();
    }
   
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	/* TODO
    	 * This writes to file but doesn't update loaded config
    	 */
    	if (cmd.getName().equalsIgnoreCase("setUDPIPAddress")) { 
    		// Check a single argument for IPAddress
    		if (checkArguments(args.length, sender, 1)) {
    	    	try {
    	    		// Check whether args[0] is a valid IP address
    	    		InetAddress IPAddress = InetAddress.getByName(args[0]);
    	    		if (IPAddress.isReachable(timeout)) {
    	    			sender.sendMessage("IP address is set to " + args[0] + " and is reachable");
    	    			// Set new address and reload plugin
    	    			this.getConfig().set("LEDIPAddress.IPAddress", args[0]);
    	    			saveConfig(); 
    	    			reinitialiseLED();
    	    			return true;
    	    		} else {
    	    			// IP Address doesn't work
    	    			sender.sendMessage("Trying to set IP address is set to " + args[0] + " and is not reachable");
    	    			sender.sendMessage("This is not reachable so settings won't be updated.");
    	    			// Don't bother changing address
    	    			return false;
    	    		}
    	    	} catch (Exception e){
    	    		//TODO Better error handling than this lazy cop out
    	    		e.printStackTrace();
    	    		return false;
    	    	}
    		} else {
    			return false;
    	    }
    	} else if (cmd.getName().equalsIgnoreCase("listUDPConfig")) {
    		String IPAddressListed = this.getConfig().getString("LEDIPAddress.IPAddress");
    		String timeoutListed = this.getConfig().getString("LEDIPAddress.timeout");
    		String shortTimeoutListed = this.getConfig().getString("LEDIPAddress.shortTimeout");    		
    		sender.sendMessage("IPAddress is set to " + IPAddressListed);
    		sender.sendMessage("timeout is set to " + timeoutListed);
    		sender.sendMessage("shortTimeout is set to " + shortTimeoutListed);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("updateUDPConfig")) {
    		updateConfig();
    		reinitialiseLED();
    		return true;
    	/* TODO
    	 * This works but doesn't write to file and updateConfig resets to default
    	 */
    	} else if (cmd.getName().equalsIgnoreCase("setUDPTimeout")) {
    		// Check correct number of arguments
    		if (checkArguments(args.length, sender, 1)) {
    			// Ensure args[0] is an integer then set timeout
    			if (isInteger(args[0])) {
    				sender.sendMessage("Timeout set to " + args[0]);
    				this.getConfig().set("LEDIPAddress.timeout", args[0]);
    				return true;
    				} else {
    					//If not message sender and return false
    					sender.sendMessage(args[0] + " isn't a valid integer so ignoring.");
    					return false;
    				}
    		} else { 
    			// No need for message as already sent by checkArguments
    			return false;
    		}
    	/* TODO
    	 * This works but doesn't write to file and updateConfig resets to default
    	 */
    	} else if (cmd.getName().equalsIgnoreCase("setUDPShortTimeout")) {
    		// Check correct number of arguments
    		if (checkArguments(args.length, sender, 1)) {
    			// Ensure args[0] is an integer then set timeout
    			if (isInteger(args[0])) {
    				sender.sendMessage("Shorttimeout set to " + args[0]);
    				this.getConfig().set("LEDIPAddress.shortTimeout", args[0]);
    				return true;
    			} else {
    				//If not message sender and return false
    				sender.sendMessage(args[0] + " isn't a valid integer so ignoring.");
        			return false;
    			}
    		} else {
    			// No need for message as already sent by checkArguments
    			return false;
    		}
    	} else {
    	sender.sendMessage("Gibberish or a typo, either way it ain't happening");
        return false;
    	}
    }

	public void udpTransmit(String message) {		
	getLogger().info("Starting udpTransmit()");
	// Ignore if loopback address
	/* Make live after testing
	if (!udpIPAddress.startsWith("127")) {
	 * 
	 * Note that 127.x.x.x is also reachable but not valid
	 */
		byte[] sendData = new byte[16];
		byte[] receiveData = new byte[16];
		// String sentence = inFromUser.readLine();
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(timeout);
			InetAddress IPAddress = InetAddress.getByName(udpIPAddress);
			if (IPAddress.isReachable(shortTimeout)) {
				System.out.println(udpIPAddress + " is reachable");
				sendData = message.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
				clientSocket.send(sendPacket);
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				System.out.println("Set to receive packet from within udpTransmit");
				//Hangs here if reply not received
				try {
					clientSocket.receive(receivePacket);
					InetAddress IPAddressRec = receivePacket.getAddress();
					int port = receivePacket.getPort();
					System.out.println("Got this from " + IPAddressRec + " @ port " + port);
					String modifiedSentence = new String(receivePacket.getData());
					System.out.println("FROM SERVER:" + modifiedSentence);
					System.out.println("IPAddress = " + IPAddress);
				} catch (SocketTimeoutException e) {
					//Timeout waiting for reply
					System.out.println("Didn't get a reply within timeout");
				}
       		clientSocket.close();
       } else {
		   System.out.println(udpIPAddress + " is not reachable");
	   }
    	}
    catch (Exception e) {
    	//TODO Better error handling than this lazy cop out
    	e.printStackTrace();
    	}
	// If loopback address
    /* Make live after testing
	//} else {
		//Loopback default of 127.0.0.1 so don't bother
		System.out.println("IP Address is still set to default of 127.0.0.1. Not transmitting.");
	}
	*/
	}
	
	/*
    // Determine player location
	//TODO convert this to return count type and receiveIPAddrees or return boolean
    private void isLocal() {
    	// Set local variables and count
    	// Count players coming from router's address as external
    	if (recentPlayerIP.equals("192.168.1.1")) {
    		if (recentJoin) {
    			notLocal++;
    			}
    			else {
    			notLocal--;
    			}
    		}
    	//Any other addresses starting with 192.168.1 are internal
    	//TODO Check the 172.16.x.x~172.31.x.x IP range
    	else if (recentPlayerIP.startsWith("192.168") || recentPlayerIP.startsWith("10")) {
    	//TODO Update to receive IP address (possibly return count type later)
    	// else if (InetAddress.isSiteLocalAddress(recentPlayerIP)) {
    		if (recentJoin) {
    			local++;
    			}
    		else {
    		local--;
    			}
    		}
    	// Anything else is external
    	else {
    		if (recentJoin) {
    			notLocal++;
    			}
    			else {
    			notLocal--;
    			}
    		}
    	}
	*/
	
	// TODO test this shows external IP addresses properly
	private Boolean isLocal(InetAddress addr) {
    	// Set local variables and count
    	// Count players coming from router's address as external
		if (addr.isSiteLocalAddress()) {
    		return true;
		} else {
			return false;
    	}
	}
	
    // Initialise LED after IP Address change
    private void reinitialiseLED() {
		udpTransmit ("Red On");
		udpTransmit ("Amber Off");
		udpTransmit ("Green Off");		
		updateLED();
    }
    
    // Update player LED status
    private void updateLED() {
    	if (local > 0) {
    		udpTransmit ("Amber On");
    		}
    	else {
    		udpTransmit ("Amber Off");
    		}
    	if (notLocal > 0) {
    		udpTransmit ("Green On");		
    		}
    	else {
    		udpTransmit ("Green Off");		
    		}	
    }
    
    /*
     * This reads from changed values fine (IPAddress)
     */
    public void loadConfiguration() { 
		// Create virtual config file
    	getLogger().info("Starting loadConfiguration()");
    	File configFile = new File(getDataFolder(), "config.yml");
    	// Set defaults as variables that can be reverted to if necessary
    	String defaultIPAddress = this.getConfig().getString("LEDIPAddress.IPAddress");
    	//TODO Check what happens if these don't return integers
    	Integer defaultTimeout = this.getConfig().getInt("LEDIPAddress.timeout");
    	Integer defaultShortTimeout = this.getConfig().getInt("LEDIPAddress.shortTimeout");
		// Check config exists or set up if it doesn't
		if (!configFile.exists()) {
			getLogger().info("Plugin hasn't been configured so creating config");
			this.getConfig().options().copyDefaults(true);
			//TODO Look at fixing ownership properly
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
			//Assign IP address directly as plugin default loopback IP so safe
			String udpIPAddress = this.getConfig().getString("LEDIPAddress.IPAddress");
			//Similarly assign timeouts from defaults
			timeout = defaultTimeout;
			shortTimeout = defaultShortTimeout;
			getLogger().info("freshIPAddress is set to " + udpIPAddress);
		} else {
			getLogger().info("config file already exists");	
			// Attempt to read config file
			this.getConfig().options().copyDefaults(false);
			String newIPAddress = this.getConfig().getString("LEDIPAddress.IPAddress");
			getLogger().info("newFromFileIPAddress is set to " + newIPAddress);
			//TDODO check whether these fail if values aren't integers
			Integer newTimeout = this.getConfig().getInt("LEDIPAddress.timeout");
			Integer newShortTimeout = this.getConfig().getInt("LEDIPAddress.shortTimeout");
			//TODO If all is fine then assign
			timeout = newTimeout;
			shortTimeout = newShortTimeout;
			/* TODO
			 * If values aren't valid then assign defaults
			 * timeout = defaultTimeout;
			 * shortTimeout = defaultShortTimeout;
			 * 
			 */
			try {
				//Test this new address first as tempIPAddress
				InetAddress tempIPAddress = InetAddress.getByName(newIPAddress);
				if (tempIPAddress.isReachable(timeout)) {
					System.out.println(tempIPAddress + " is reachable");
					udpIPAddress = newIPAddress;
					getLogger().info("IPAddress is now set to " + udpIPAddress);
				} else {
					System.out.println(tempIPAddress + " is not reachable");
					System.out.println("Reverting to loopback default of 127.0.0.1");
					udpIPAddress = defaultIPAddress;
					saveConfig();
				}
			} catch (Exception e) {
				    	//TODO Better error handling than this lazy cop out
				    	e.printStackTrace();
			}		
		}
    }
    
    /* Loads config.yml from disc and updates fields
     * 
     */
    private void updateConfig() {
    	String IPAddressTemp = this.getConfig().getString("LEDIPAddress.IPAddress");
    	Integer timeoutTemp = this.getConfig().getInt("LEDIPAddress.timeout");
    	System.out.println("updateConfigTimeoutTemp is " + timeoutTemp);
    	Integer shortTimeoutTemp = this.getConfig().getInt("LEDIPAddress.shortTimeout");
    	System.out.println("updateConfigShortTimeoutTemp is " + shortTimeoutTemp);
    	reloadConfig();
		String Proposed = this.getConfig().getString("LEDIPAddress.IPAddress");
		try {
			InetAddress IPAddressProposed = InetAddress.getByName(Proposed);
			if (IPAddressProposed.isReachable(timeout)) {
		    	this.getConfig().options().copyDefaults(false);
		    	String IPAddressUpdated = this.getConfig().getString("LEDIPAddress.IPAddress");
				getLogger().info("updatedIPAddress is set to " + IPAddressUpdated);
				saveConfig();
				reinitialiseLED();
			} else {
	    		getLogger().info("IP address not working, keeping to previous");	
	    		this.getConfig().set("LEDIPAddress.IPAddress", IPAddressTemp);
	    		saveConfig();  
	    	}
		} catch (Exception e) {
        	//TODO Clarify this lazy cop out
            e.printStackTrace();
    	}
		//TODO Check if timeout values are valid		
		/*
		 * if (timeout values are valid) {
		 * timeout = timeoutUpdated;
		 * shortTimeout = shortTimeoutUpdated;
		 * } else {
		 * timeout = timeoutTemp;
		 * shortTimeout = shortTimeoutTemp;
		 * }
		 */
    }
    
    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0) {
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
        	//TODO Clarify this lazy cop out
            e.printStackTrace();
        }
    }
    
    private Boolean checkArguments (Integer args, CommandSender sender, Integer correctArgs) {
    	if (args < correctArgs) {
			sender.sendMessage("Not enough arguments!");
	        return false;
	    } else if (args > correctArgs) {
	    	sender.sendMessage("Calm down, too many arguments!");
	        return false;
	    } else {
	    	sender.sendMessage("Correct number of arguments!");
	    	return true;
	    } 	
    }
    
    private static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}