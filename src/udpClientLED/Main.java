package udpClientLED;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public final class Main extends JavaPlugin implements Listener {
	
	private static short local = 0;
	private static short notLocal = 0;
	private static final String localHost = "127.0.0.1";
	private static InetAddress recentPlayerIP = null;
	private static InetAddress routerIP = null;
	private static String osName = null;
	// Not initialised as these are set by file or config defaults
	private static String IPAddressString;
	private static InetAddress IPAddress;
	private static int timeout;
	private static int shortTimeout;
	 
	@Override
    public void onEnable() {
		// register listener
		getServer().getPluginManager().registerEvents(this, this);
		// Cache osName for later, only call if not set
		if(osName == null) { 
	    	  osName = checkOS(); 
	    }
		routerIP = getRouter(osName);
		getLogger().info("Server is running on " + osName);
		// Load or initialise configuration file
		loadConfiguration();
		//getLogger().info("Returned from loadConfiguration()");
		getLogger().info("[onEnable]IPAddress is set to " + IPAddress);
		getLogger().info("[onEnable]timeout is set to " + timeout);
		getLogger().info("[onEnable]shortTimeout is set to " + shortTimeout);
		// Indicate that plugin has started with a light display
		udpTransmit ("Funky_Disco", IPAddress);
		getLogger().info("[onEnable]udpClientLED is switched on"); 
		udpTransmit ("Red_On", IPAddress);
		//TODO Check if any players are already logged in and adjust local count accordingly
	}
 
    @Override
    public void onDisable() {
        //Switch off all LEDs
    	udpTransmit ("All_Off", IPAddress);
        getLogger().info("[onDisable]udpClientLED has been extinguished");
    }
    
    // Someone joins server
    @EventHandler
    public void onLogin(PlayerJoinEvent event) throws UnknownHostException {
    	// Check whether internal or external IP address
    	// recentPlayerIP = event.getPlayer().getAddress().getHostString();
    	recentPlayerIP = event.getPlayer().getAddress().getAddress();      	
    	if(isLocal(recentPlayerIP)) {
    		local++;
    		//event.getPlayer().sendMessage("IP address is " + event.getPlayer().getAddress().getAddress());
    	} else {
    		notLocal++;
    		//event.getPlayer().sendMessage("IP address is " + event.getPlayer().getAddress().getAddress());
    	}
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
    	    			sender.sendMessage("Trying to set IP address is set to " + args[0] + " and is not reachable.");
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
    		// Check correct number of arguments
    		if (checkArguments(args.length, sender, 1)) {
    			// Ensure args[0] is an integer then set timeout
    			if (isInteger(args[0])) {
    				sender.sendMessage("Timeout set to " + args[0]);
    				// Sets the value to loaded config file.
    				timeout = Integer.parseInt(args[0]);
    				this.getConfig().set("LEDIPAddress.timeout", timeout);
    				// Write this value to config file on disc
    				saveConfig();
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
    	} else if (cmd.getName().equalsIgnoreCase("setUDPShortTimeout")) {
    		// Check correct number of arguments
    		if (checkArguments(args.length, sender, 1)) {
    			// Ensure args[0] is an integer then set timeout
    			if (isInteger(args[0])) {
    				sender.sendMessage("Shorttimeout set to " + args[0]);
    				// Sets the value to loaded config file.
    				shortTimeout = Integer.parseInt(args[0]);
    				this.getConfig().set("LEDIPAddress.shortTimeout", shortTimeout);
    				// Write this value to config file on disc
    				saveConfig();
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
    //TODO Change this so it accepts an IP address in addition to transmission string.
    /*
     * This will enable an IP address to be tested without being committed as udpIPAddress
     */
    
	// public void udpTransmit(String message) {
    public String udpTransmit(String message, InetAddress udpIPAddress) {		
    	getLogger().info("[udpTransmit] is starting");
    	getLogger().info("[udpTransmit]IPAddress is " + udpIPAddress);
    	getLogger().info("[udpTransmit][DEBUG]getHostAddress is " + udpIPAddress.getHostAddress());
    	if (!udpIPAddress.getHostAddress().startsWith("127")) {
    		byte[] receiveData = new byte[30];
    		String returnMessage;
    		try {
    			DatagramSocket clientSocket = new DatagramSocket();
    			clientSocket.setSoTimeout(timeout);
    			getLogger().info("[udpTransmit]Socket is defined");
    			if (udpIPAddress.isReachable(shortTimeout)) {
    				getLogger().info("[udpTransmit]" + udpIPAddress + " is reachable");
    				byte[] sendData = message.getBytes();
    				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, udpIPAddress, 9876);
    				clientSocket.send(sendPacket);
    				getLogger().info("[udpTransmit]Sending packet from udpTransmit, length =" + sendData.length);
    				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    				getLogger().info("[udpTransmit]Set to receive packet from within udpTransmit");
    				//Hangs here if reply not received
    				try {
    					clientSocket.receive(receivePacket);
    					InetAddress IPAddressRec = receivePacket.getAddress();
    					int port = receivePacket.getPort();
    					String modifiedSentence = new String(receivePacket.getData());
    					modifiedSentence = modifiedSentence.replaceAll("[^\\p{Print}]", "");
    					getLogger().info("[udpTransmit]Receiving packet from udpTransmit, length =" + receiveData.length);
    					getLogger().info("[udpTransmit]Got this from " + IPAddressRec + " @ port " + port);
    					if (modifiedSentence.equals("Oi_Oi_Oi")) {
							returnMessage= "Oi_Oi_Oi";
    					} else {
    						getLogger().info("[udpTransmit]FROM SERVER:" + modifiedSentence);
    						getLogger().info("[udpTransmit]udpIPAddress = " + udpIPAddress);
    						returnMessage = "Success";
    					}
    				} catch (SocketTimeoutException e) {
    					//Timeout waiting for reply
    					getLogger().info("[udpTransmit]Didn't get a reply within timeout");
    					returnMessage = "Timeout";
    				}
					clientSocket.close();
					return(returnMessage);
    			} else {
    				getLogger().info("[udpTransmit]" + udpIPAddress + " is not reachable");
    				clientSocket.close();
    				return("Not reachable");						
					}
				} catch (Exception e) {
					//TODO Better error handling than this lazy cop out
					e.printStackTrace();
					return("Error");
				}
    		} else {
    			getLogger().info("[udpTransmit]Loopback address of " + udpIPAddress + " so not transmitting");
    			return("loopback");
    		}
	}
	
	// Determine whether the player is local on login/logout
	private Boolean isLocal(InetAddress addr) {
		//getLogger().info("Address passed to getRouter is " + addr);
		//getLogger().info("routerIP passed to getRouter is " + routerIP);
    	// Count players coming from router's address as external
		if (addr.equals(routerIP)) {
			getLogger().info("[isLocal] decided player came via router.");
			return false;
		} else if (addr.isSiteLocalAddress()) {
			getLogger().info("[isLocal] decided player was local.");
    		return true;
		} else {
			getLogger().info("[isLocal] decided player was notlocal!");
			return false;
    	}
	}
	
    // Initialise LED after IP Address change
    private void reinitialiseLED() {
		udpTransmit ("Red_On", IPAddress);
		udpTransmit ("Amber_Off", IPAddress);
		udpTransmit ("Green_Off", IPAddress);		
		updateLED();
    }
    
    // Update player LED status
    private void updateLED() {
    	if (local > 0) {
    		udpTransmit ("Amber_On", IPAddress);
    		}
    	else {
    		udpTransmit ("Amber_Off", IPAddress);
    		}
    	if (notLocal > 0) {
    		udpTransmit ("Green_On", IPAddress);		
    		}
    	else {
    		udpTransmit ("Green_Off", IPAddress);		
    		}	
    }
    
    /*
     * This reads from changed values fine (IPAddress)
     */
    public void loadConfiguration() { 
		// Create virtual config file
    	getLogger().info("[loadConfiguration] is starting");
    	File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			getLogger().info("[loadConfiguration]Plugin hasn't been configured so creating config");
			this.getConfig().options().copyDefaults(true);
			configFile.getParentFile().mkdirs();
			copy(getResource("config.yml"), configFile);
			//Assign IP address directly as plugin default loopback IP so safe
			String IPAddressString = this.getConfig().getString("LEDIPAddress.IPAddress");
			try {
				IPAddress = InetAddress.getByName(IPAddressString);	
				} catch (Exception e) {
					getLogger().info("[loadConfiguration]Default config file possibly corrupt");	
			    	e.printStackTrace();
				}
			//this.getConfig().set("LEDIPAddress.IPAddress", IPAddress);
			//saveConfig();
			//Similarly assign timeouts from defaults
			timeout = this.getConfig().getInt("LEDIPAddress.timeout");
			shortTimeout = this.getConfig().getInt("LEDIPAddress.shortTimeout");
			getLogger().info("[loadConfiguration]IPAddressString is set to " + IPAddressString);
		} else {
			getLogger().info("[loadConfiguration]config file already exists");	
			// Attempt to read config file
			this.getConfig().options().copyDefaults(false);
			String IPAddressString = this.getConfig().getString("LEDIPAddress.IPAddress");
			getLogger().info("[loadConfiguration]newFromFileIPAddressString is set to " + IPAddressString); 
			//TODO check whether these fail if values aren't integers
			Integer newTimeout = this.getConfig().getInt("LEDIPAddress.timeout");
			Integer newShortTimeout = this.getConfig().getInt("LEDIPAddress.shortTimeout");
			//If all is fine then assign
			timeout = newTimeout;
			shortTimeout = newShortTimeout;
			/* TODO
			 * If values aren't valid then assign defaults
			 * timeout = defaultTimeout;
			 * shortTimeout = defaultShortTimeout;
			 * 
			 */
			try {
				IPAddress = InetAddress.getByName(IPAddressString);
				} catch (Exception e) {
					getLogger().info("[loadConfiguration]InetAddress has probably thrown unknownHost Exception with " + IPAddressString);	
					//TODO load config file loopback default.
					IPAddressString = localHost;
					saveConfig();
					try {
						IPAddress = InetAddress.getByName(IPAddressString);	
						} catch (Exception e2) {
							getLogger().info("[loadConfiguration]localHost variable definition invalid");	
					    	e2.printStackTrace();
						}
					this.getConfig().set("LEDIPAddress.IPAddress", IPAddress);
					saveConfig();
		    	e.printStackTrace();
				}					
			getLogger().info("[loadConfiguration]IPAddress is set to " + IPAddress);
			String testIPResult = checkIPAddress(IPAddress);
			getLogger().info("[loadConfiguration]checkIPAddress() returned " + testIPResult);
			switch (testIPResult) {
				case "running":	
					getLogger().info("[loadConfiguration]Server is connecting correctly");	
					getLogger().info("[loadConfiguration]IPAddress is set to " + IPAddress);
					break;
				case "stopped":	
					getLogger().info("[loadConfiguration]Server is connected but application not running");	
					getLogger().info("[loadConfiguration]IPAddress is set to " + IPAddress);
					break;
				case "unreachable":	
					getLogger().info("[loadConfiguration]Server is not connecting correctly");	
					getLogger().info("[loadConfiguration]IPAddress is set to " + IPAddress);
					break;
				case "loopback":	
					getLogger().info("[loadConfiguration]Server is not configured or using loopback address");	
					getLogger().info("[loadConfiguration]IPAddress is set to " + IPAddress);
					break;
				case "error":	
					getLogger().info("[loadConfiguration]Something has gone wrong");
					getLogger().info("[loadConfiguration]IPAddress is set to " + IPAddress);
					break;
			}
				/*if (IPAddress.isReachable(timeout)) {
					//TODO Confirm that udpServerLED is running on proposed target
					udpTransmit ("Oggy_Oggy_Oggy", tempIPAddress);
					//Check if response is Oi_Oi_Oi
					getLogger().info(tempIPAddress + " is reachable");
					IPAddressString = newIPAddress;
					getLogger().info("IPAddress is now set to " + IPAddressString);
				} else {
					getLogger().info(tempIPAddress + " is not reachable");
					getLogger().info("Reverting to loopback default of 127.0.0.1");
					IPAddressString = defaultIPAddress;
					saveConfig();
				}
				*/		
		}
		getLogger().info("[loadConfiguration]IPAddress is  " + IPAddress); 
    }
    
    /* Loads config.yml from disc and updates fields
     * 
     */
    private void updateConfig() {
    	//Load config from file to check values
    	reloadConfig();
    	String AddressProposed = this.getConfig().getString("LEDIPAddress.IPAddress");
    	getLogger().info("updateConfigIPAddressTemp is " + AddressProposed);
    	Integer timeoutProposed = this.getConfig().getInt("LEDIPAddress.timeout");
    	getLogger().info("updateConfigTimeoutTemp is " + timeoutProposed);
    	Integer shortTimeoutProposed = this.getConfig().getInt("LEDIPAddress.shortTimeout");
    	getLogger().info("updateConfigShortTimeoutTemp is " + shortTimeoutProposed);
		try {
			InetAddress IPAddressProposed = InetAddress.getByName(AddressProposed);
			if (IPAddressProposed.isReachable(timeout)) {
				//TODO Confirm that udpServerLED is running on proposed target
				udpTransmit("Oggy_Oggy_Oggy", IPAddressProposed);
				//Check if response is Oi_Oi_Oi
				getLogger().info("proposedIPAddress is set to " + AddressProposed);
				IPAddressString = AddressProposed;
				this.getConfig().set("LEDIPAddress.IPAddress", AddressProposed);
				saveConfig();
				reinitialiseLED();
			} else {
	    		getLogger().info("IP address not working, resorting to previous");	
	    		this.getConfig().set("LEDIPAddress.IPAddress", IPAddressString);
	    	}
			saveConfig();
			reinitialiseLED();
		} catch (Exception e) {
        	//TODO Clarify this lazy cop out
            e.printStackTrace();
    	}
		//TODO Check if timeout values are valid		
		/*
		 * if (timeout values are valid) {
		 * timeout = timeoutProposed;
		 * } else {
		 * getLogger().info("timeout not a valid integer, resorting to previous");	
	     * this.getConfig().set("LEDIPAddress.timeout, timeout);
	     * }
	     * 
		 * 
		 * 
		 * if (shortTimeout values are valid) {
		 * shortTimeout = shortTimeoutProposed;
		 * } else {
		 * getLogger().info("shortTimeout not a valid integer, resorting to previous");	
	    		this.getConfig().set("LEDIPAddress.timeout, shortTimeout);
		 * saveConfig();
		 * reinitialiseLED();

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
    
    //TODO Test method to check if a proposed new IP address is valid.
    // Work out how best to do this.
    private String checkIPAddress (InetAddress proposedIP) {
    	getLogger().info("[checkIPAddress]Starting with " + proposedIP);
		if (!(proposedIP.getHostAddress().startsWith("127"))) {
			getLogger().info("[checkIPAddress]Not loopback address");
    		try {
    			getLogger().info("[checkIPAddress]timeout is " + timeout);
    			if (proposedIP.isReachable(timeout)) {
    				getLogger().info("[checkIPAddress]Reachable");
    				String testTransmit = udpTransmit("Oggy_Oggy_Oggy", proposedIP);
    				getLogger().info("[checkIPAddress]testTransmit = " + testTransmit);
    				if (testTransmit.equals("Oi_Oi_Oi")) {
    					//Server running at this IP address
    					return("running");
    				} else {
    					
    					//IP address valid but no server
    					return("stopped");
    				}
    			} else {
    			getLogger().info("[checkIPAddress]Not reachable");
    			//IP address not reachable
    			return("unreachable");
    			}
    		} catch (Exception e) {
    			//TODO Clarify this lazy cop out
    			/*
    			 * Caused by either network error or negative timeout value
    			 */
    			getLogger().info("[checkIPAddress]Pear shaped");
    			e.printStackTrace();
    			return("error");
    		}
		} else {
			// Part of the loopback range
			getLogger().info("[checkIPAddress]Loopback");
			return("loopback");
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
    
    private String checkOS() {
    	String name = System.getProperty("os.name").toLowerCase();
    	if (name.contains("windows")) {
    		return "Windows";
    	} else if (name.contains("linux")) {
    		return "Linux";
    	} else if (name.contains("mac")) {
    		return "Mac";
        // Currently untested
    	} else if (name.contains("sunos")) {
    		return "Solaris";	    	
		} else if (name.contains("bsd")) {
	    		return "FreeBSD";    		
    	} else {
    		getLogger().info(name + " is currently unsupported.");
    		getLogger().info("If you would like to help get " + name + " supported.");
    		getLogger().info("Please raise a ticket for the developer on BukkitDev.");
    		getLogger().info("Use os.name ='" + name + "' is unsupported as the title.");    		
    		getLogger().info("Run the terminal/command line command 'netstat -rn'");
    		getLogger().info("Include the ouptut in your ticket description.");
    		return "Unknown";   		
    	}    	
   }
    
   private InetAddress getRouter(String OS) {
	   String gateway = "";
	   try {
		   // Ignore unknown OS case as can't handle
		   if (OS.equals("Unknown")) {
			   getLogger().info("Unsupported OS so setting router as loopback address.");
			   return InetAddress.getByName(localHost);
		   }
		   Process result = Runtime.getRuntime().exec("netstat -rn");	   
		   BufferedReader output = new BufferedReader
				    (new InputStreamReader(result.getInputStream()));
				    String line = output.readLine();
				    while(line != null){
				    if (line.startsWith("default") || line.startsWith("0.0.0.0"))
				        break;      
				    line = output.readLine();
				    }
				    //getLogger().info("Captured line is '" + line + "'");
				    StringTokenizer st = new StringTokenizer( line );
				    // Case for Mac/Linux/Solaris/FreeBSD, 2nd token is gateway
				    if (OS.equals("Mac") || OS.equals("Linux")|| OS.equals("Solaris") || OS.equals("FreeBSD")) { 
				    	st.nextToken();
				    	gateway = st.nextToken();
				    	st.nextToken();
				    	st.nextToken();
				    	st.nextToken();
				    /*
				     * If other cases added then change next line to
				     * } else if (OS.equals("Windows"))) {
				     */
					// Must be Windows otherwise, 3rd token is gateway
				    } else {
				    	st.nextToken();
				    	/* 
				    	 * Example line for debugging a new test case
				    	 * String one = st.nextToken();
				    	 * getLogger().info("Token 1 is '" + one + "'");
				    	*/
				    	st.nextToken();
					    gateway = st.nextToken();
					    st.nextToken();
				    	st.nextToken();    	
				    } 
			InetAddress routerIP = InetAddress.getByName(gateway);
			// getLogger().info("Gateway is set to " + gateway);
			return routerIP;
	   } catch (Exception e ) { 
		   getLogger().info(e.toString());
	   } 
	   return routerIP;
   }
}