package udpClientLED;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
//import java.io.File;
//import java.io.FileWriter;

//TODO Check which bits of this are essential
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import java.io.*;
//import java.net.*;


public final class Main extends JavaPlugin implements Listener {
	
	private short local = 0;
	private short notLocal = 0;
	private boolean recentJoin = false;
	private String recentPlayerIP = "";
	private static final String udpServerIP = "192.168.1.34";
	/*
	private static final String gpioPath="/sys/class/gpio";
	private static final String exportPath= gpioPath + "/export";
	private static final String unexportPath= gpioPath + "/unexport";
	private static final String devicePath= gpioPath + "/gpio%d";
	private static final String directionPath= devicePath + "/direction";
	private static final String valuePath= devicePath + "/value";
	private static final String gpioOut = "out";
	private static final String gpioOn = "1";
	private static final String gpioOff = "0";
	private static final int[] gpioChannel = {18,23,24};
	*/
	
	@Override
    public void onEnable() {
		// register listener
		getServer().getPluginManager().registerEvents(this, this);
		/*		
		// Open file handles for GPIO unexport and export
		try {
			FileWriter unexportFile = new FileWriter(unexportPath);
			FileWriter exportFile = new FileWriter(exportPath);
		
			// Initialise GPIO settings
			for (Integer channel : gpioChannel) {
				File exportFileCheck = new File(getDevicePath(channel));
				if (exportFileCheck.exists()) {
					unexportFile.write(channel.toString());
					unexportFile.flush();	
					}
				// Set port for use
				exportFile.write(channel.toString());
				exportFile.flush();
				//Set direction file
				FileWriter directionFile = new FileWriter(getDirectionPath(channel));
				directionFile.write(gpioOut);
				directionFile.flush();
				directionFile.close();
				}
					
			unexportFile.close();
			exportFile.close();
			}
		catch (Exception exception) {
			exception.printStackTrace();
			}
		
		// Switch on server LED
		writeLED (gpioChannel[0], gpioOn);
		*/
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

/*
    // Variable setting for device path
    private static String getDevicePath(int pinNumber) {
 	   return String.format(devicePath, pinNumber);
    }
    
    // Variable setting for direction path
    private static String getDirectionPath(int pinNumber) {
 	   return String.format(directionPath, pinNumber);
    }

    // Variable setting for value path
    private static String getValuePath(int pinNumber) {
 	   return String.format(valuePath, pinNumber);
    }

*/    
    
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
    /*
    // LED IO 
    private void writeLED (int channel, String status) {
    	try {
     		FileWriter commandFile = new FileWriter(getValuePath(channel));
     		commandFile.write(status);
     		commandFile.flush();
     		commandFile.close();
        }
        catch (Exception exception) {
        	exception.printStackTrace();
        }
    }
    */
}

