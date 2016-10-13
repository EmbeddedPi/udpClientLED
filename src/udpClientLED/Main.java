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
import java.io.IOException;

public final class Main extends JavaPlugin implements Listener {
	
	private short local = 0;
	private short notLocal = 0;
	private boolean recentJoin = false;
	private String recentPlayerIP = "";
	private static final String udpServerIP = "192.168.1.34";
	// Test line to initialise using loopback interface
	// private static final String udpServerIP = "127.0.0.1";
	
	@Override
    public void onEnable() {
		// register listener
		getServer().getPluginManager().registerEvents(this, this);
		// Check config exists or set up if it doesn't
		File yml = new File("plugins//udpClientLED//config.yml");
		// File yml = new File("plugins/udpClientLED/config.yml");
		if (!yml.exists()) {
			getLogger().info("Plugin hasn't been configured so creating config");
			try {
				yml.getParentFile().mkdirs();
				yml.createNewFile();
				//TODO TLook at fixing ownership properly
				//TODO Write default IP address into file
			} catch (IOException e) {
				// Can't get at plugins folder for some reason
				e.printStackTrace();
			} 
		} else {
			getLogger().info("config file already exists");	
			// Attempt to read config file
		}	
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
    			sender.sendMessage("Should just be one argument!");
    	        return false;
    	    } else if (args.length >1) {
    	    	sender.sendMessage("Calm down, too many arguments!");
 	           	return false;
    	    } else {
    		// Check whether args[0] is a valid IP address
    		// Check whether IP address works
    		sender.sendMessage("TEST IP address is set to " + args[0]);
    		// Set new address and reload plugin
    		return true;}
    	} else if (cmd.getName().equalsIgnoreCase("listLEDIPAddress")) {
    		sender.sendMessage("UDP server IP is " + udpServerIP);
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
    	}
    catch (Exception e) {
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
}

