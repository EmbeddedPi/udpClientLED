package udpClientLED;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

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
	private boolean recentJoin = false;
	private String recentPlayerIP = "";
	//private static final String udpServerIP = "192.168.1.34";
	private static String udpIPAddress;
	// Default timeout for checking udp connection
	private static int timeout;
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
	}
 
    @Override
    public void onDisable() {
        //Switch off all LEDs
    	udpTransmit ("All Off");
        getLogger().info("udpClientLED has been extinguished.");
    }
    
    // Someone joins server
    @EventHandler
    public void onLogin(PlayerJoinEvent event) {
    	// Check whether internal or external IP address
    	recentPlayerIP = event.getPlayer().getAddress().getHostString();
    	recentJoin = true;
    	isLocal();
    	// Update local/notLocal LED status according
    	updateLED();
    }
    
    // Someone leaves server
    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
    	// Check whether internal or external IP address
    	recentPlayerIP = event.getPlayer().getAddress().getHostString();
    	recentJoin = false;
    	isLocal();
    	// Update local/notLocal LED status according
    	updateLED();
    }
   
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {  	
    	if (cmd.getName().equalsIgnoreCase("setUDPIPAddress")) { 
    		// Check a single argument for IPAddress
    		if (checkArguments(args.length, sender)) {
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
    	} else if (cmd.getName().equalsIgnoreCase("setUDPTimeout")) {
    		//Ensure args[0] is an integer then set timeout
    		if (checkArguments(args.length, sender) && isInteger(args[0])) {
    			sender.sendMessage("Timeout set to " + args[0]);
    			this.getConfig().set("LEDIPAddress.timeout", args[0]);
    			return true;
    			//If not message sender and return false
    		} else {
    			sender.sendMessage(args[0] + " isn't a valid integer so ignoring.");
    			return false;
    		}
    	} else if (cmd.getName().equalsIgnoreCase("setUDPShortTimeout")) {
    		//Ensure args[0] is an integer then set shorttimeout
    		if (checkArguments(args.length, sender) && isInteger(args[0])) {
    			sender.sendMessage("Shorttimeout set to " + args[0]);
    			this.getConfig().set("LEDIPAddress.shortTimeout", args[0]);
    			return true;
    			//If not message sender and return false
    		} else {
    			sender.sendMessage(args[0] + " isn't a valid integer so ignoring.");
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
	if (!udpIPAddress.equals("127.0.0.1")) {
	 * 
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
		//System.out.println("IP Address is still set to default of 127.0.0.1. Not transmitting.");
		System.out.println("IudpTransmit is ignoring this");
	}
	*/
	}
    // Determine player location
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
    	else if (recentPlayerIP.startsWith("192.168.1")) {
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

    // Initialise LEd after IP Address change
    private void reinitialiseLED() {
		udpTransmit ("Red On");
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
			/*
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
    	Integer shortTimeoutTemp = this.getConfig().getInt("LEDIPAddress.shortTimeout");
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
    
    private Boolean checkArguments (Integer args, CommandSender sender) {
    	if (args < 1) {
			sender.sendMessage("Should have an argument!");
	        return false;
	    } else if (args >1) {
	    	sender.sendMessage("Calm down, too many arguments!");
	        return false;
	    } else {
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