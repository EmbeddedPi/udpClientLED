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
	//TODO Delete this line after testing
	//private static final String udpServerIP = "192.168.1.34";
	//TODO Make this line default after testing
	private static String udpIPAddress;
	// Default timeout for checking udp connection
	// TODO Add these to the config file.
	private static final int timeout=5000;
	private static final int shortTimeout=500;
	 
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
   
    //TODO Sort this out
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {  	
    	if (cmd.getName().equalsIgnoreCase("setLEDIPAddress")) { 
    		// Check a single argument for IPAddress
    		if (checkArguments(args.length, sender)) {
    	    	// Check whether args[0] is a valid IP address
    	    	try {
    	    	InetAddress IPAddress = InetAddress.getByName(args[0]);
    	    	if (IPAddress.isReachable(timeout)) {
    	    		sender.sendMessage("TEST IP address is set to " + args[0] + " and is reachable");
    	    		// Set new address and reload plugin
    	    		this.getConfig().set("LEDIPAddress.IPAddress", args[0]);
    	    		saveConfig(); 
    	    		reinitialiseLED();
    	    		return true;
    	    	} else {
    	    		// IP Address doesn't work
    	    		sender.sendMessage("TEST IP address is set to " + args[0] + " and is not reachable");
    	    		return false;
    	    	}
    	    	} catch (Exception e){
    	    		//TODO Better error handling than this lazy cop out
    	    		e.printStackTrace();
    	    		return false;
    	    	}
    	    }
    	} else if (cmd.getName().equalsIgnoreCase("listConfig")) {
    		String IPAddressListed = this.getConfig().getString("LEDIPAddress.IPAddress");
    		String timeoutListed = this.getConfig().getString("LEDIPAddress.timeout");
    		String shortTimeoutListed = this.getConfig().getString("LEDIPAddress.shortTimeout");    		
    		sender.sendMessage("loadedIPAddress is set to " + IPAddressListed);
    		sender.sendMessage("timeout is set to " + timeoutListed);
    		sender.sendMessage("shortTimeout is set to " + shortTimeoutListed);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("updateConfig")) {
    		updateConfig();
    		reinitialiseLED();
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("setTimeout")) {
    		if (checkArguments(args.length, sender)) {
    			sender.sendMessage("You tried to set timeout to " + args[0]);
    			//Check if args[0] is an integer then set timeout
    			return true;
    			//If not message sender and return false
    		}
    	} else if (cmd.getName().equalsIgnoreCase("setShortTimeout")) {
    		if (checkArguments(args.length, sender)) {
    			sender.sendMessage("You tried to set shorttimeout to " + args[0]);
    			//Check if args[0] is an integer then set timeout
    			return true;
    			//If not message sender and return false
    	} 
    	sender.sendMessage("Gibberish or a typo, either way it ain't happening");
        return false;
    	}
    	return false;
    }

	public void udpTransmit(String message) {		
	getLogger().info("Starting udpTransmit()");
	// Ignore if loopback address
	/* Make Live after testing
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
				System.out.println("Attempting to transmit from within udpTransmit");
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
				clientSocket.send(sendPacket);
				System.out.println("Transmitted from within udpTransmit");
				//TODO Hangs here if reply not received
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				System.out.println("Set to receive packet from within udpTransmit");
				//TODO Hangs here if reply not received
				try {
					clientSocket.receive(receivePacket);
					System.out.println("Received from within udpTransmit");
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
		// Check config exists or set up if it doesn't
		if (!configFile.exists()) {
			getLogger().info("Plugin hasn't been configured so creating config");
			this.getConfig().options().copyDefaults(true);
			//TODO Look at fixing ownership properly
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
			//Assign IP address directly as plugin default loopback IP so safe
			String udpIPAddress = this.getConfig().getString("LEDIPAddress.IPAddress");
			getLogger().info("freshIPAddress is set to " + udpIPAddress);
		} else {
			getLogger().info("config file already exists");	
			// Attempt to read config file
			this.getConfig().options().copyDefaults(false);
			String newIPAddress = this.getConfig().getString("LEDIPAddress.IPAddress");
			getLogger().info("newFromFileIPAddress is set to " + newIPAddress);
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
					//TODO make this reload from config defaults
					udpIPAddress = "127.0.0.1";
					saveConfig();
					//TODO Force reload defaults from config if previous also fails	
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
}