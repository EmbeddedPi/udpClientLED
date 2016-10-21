package udpClientLED;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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
	private static final String udpServerIP = "192.168.1.34";
	//TODO Make this line default after testing
	//private String udpIPAddress;
	// Default timeout for checking udp connection
	private static final int timeout=5000;
	private static final int shortTimeout=500;
	 
	@Override
    public void onEnable() {
		// register listener
		getServer().getPluginManager().registerEvents(this, this);
		// Load or initialise configuration file
		loadConfiguration();
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
    		if (args.length < 1) {
    			sender.sendMessage("Should have an argument!");
    	        return false;
    	    } else if (args.length >1) {
    	    	sender.sendMessage("Calm down, too many arguments!");
 	           	return false;
    	    } else {
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
    	} else if (cmd.getName().equalsIgnoreCase("listLEDIPAddress")) {
    		String IPAddressListed = this.getConfig().getString("LEDIPAddress.IPAddress");
    		getLogger().info("loadedIPAddress is set to " + IPAddressListed);
    		return true;
    	} else if (cmd.getName().equalsIgnoreCase("updateLEDIPAddress")) {
    		updateLEDIPAddress();
    		reinitialiseLED();
    		return true;
    	} else {
    		sender.sendMessage("Gibberish or a typo, either way it ain't happening");
    	return false; 
    	}
    }

	public static void udpTransmit(String message) {
    try {
       //BufferedReader inFromUser =
       //  new BufferedReader(new InputStreamReader(System.in));
       DatagramSocket clientSocket = new DatagramSocket();
       InetAddress IPAddress = InetAddress.getByName(udpServerIP);
       byte[] sendData = new byte[16];
       byte[] receiveData = new byte[16];
       // String sentence = inFromUser.readLine();
       if (IPAddress.isReachable(shortTimeout)) {
    	   System.out.println(udpServerIP + " is reachable");
	       sendData = message.getBytes();
	       DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
	       clientSocket.send(sendPacket);
	       DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	       clientSocket.receive(receivePacket);
	       InetAddress IPAddressRec = receivePacket.getAddress();
	       int port = receivePacket.getPort();
	       System.out.println("Got this from " + IPAddressRec + " @ port " + port);
	       String modifiedSentence = new String(receivePacket.getData());
	       System.out.println("FROM SERVER:" + modifiedSentence);
	       System.out.println("IPAddress = " + IPAddress);
       	clientSocket.close();
       } else {
		   System.out.println(udpServerIP + " is not reachable");
	   }
    	}
    catch (Exception e) {
    	//TODO Better error handling than this lazy cop out
    	e.printStackTrace();
    	}
    }
    
    // Determine player location
    private void isLocal() {
    	// Set local variables and count
    	if (recentPlayerIP.equals("192.168.1.1")) {
    		if (recentJoin) {
    			notLocal++;
    			}
    			else {
    			notLocal--;
    			}
    		} 	
    	else if (recentPlayerIP.startsWith("192.168.1")) {
    		if (recentJoin) {
    			local++;
    			}
    		else {
    		local--;
    			}
    		}
    	else {
    		if (recentJoin) {
    			notLocal++;
    			}
    			else {
    			notLocal--;
    			}
    		}
    	}

    // Initialise LEd adfter IP Address change
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
    	File configFile = new File(getDataFolder(), "config.yml");
		String IPAddressVirtual = this.getConfig().getString("LEDIPAddress.IPAddress");
		getLogger().info("VirtualIPAddress is set to " + IPAddressVirtual);
		// Check config exists or set up if it doesn't
		if (!configFile.exists()) {
			getLogger().info("Plugin hasn't been configured so creating config");
			this.getConfig().options().copyDefaults(true);
			//TODO Look at fixing ownership properly
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
			String IPAddressFresh = this.getConfig().getString("LEDIPAddress.IPAddress");
			getLogger().info("freshIPAddress is set to " + IPAddressFresh);
		} else {
			//TODO Check that IPAddress is valid as could have been changed to an invalid value
			getLogger().info("config file already exists");	
			// Attempt to read config file
			this.getConfig().options().copyDefaults(false);
			String IPAddressExisting = this.getConfig().getString("LEDIPAddress.IPAddress");
			getLogger().info("existingIPAddress is set to " + IPAddressExisting);
		}
    	// Command to save once changed
    	//saveConfig();   	
    	// Command to reload
    	//reloadConfig();
    }
    
    /* Loads config.yml from disc and updates fields
     * 
     */
    private void updateLEDIPAddress() {
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
}