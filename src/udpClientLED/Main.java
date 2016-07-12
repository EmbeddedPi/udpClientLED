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

public final class Main extends JavaPlugin implements Listener {
	
	private short local = 0;
	private short notLocal = 0;
	private boolean recentJoin = false;
	private String recentPlayerIP = "";
	private static final String udpServerIP = "192.168.1.34";
	
	@Override
    public void onEnable() {
		// register listener
		getServer().getPluginManager().registerEvents(this, this);
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
    		// output label to check it's OK
    		getLogger().info("label is " + label); 
    		getLogger().info("args[0] is " + args[0]); 
    		getLogger().info("Sent by " + sender);
    		// Check whether label is a valid IP address
    		// Check whether IP address works
    		// doSomething
    		return true;}
    	} else if (cmd.getName().equalsIgnoreCase("listLEDIPAddress")) {
    		getLogger().info("UDP server IP is " + udpServerIP);
    		return true;
    	} else {
    		getLogger().info("Gibberish or a typo, eith way it ain't happening");
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

