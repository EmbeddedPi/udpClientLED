udpClientLED
============

A status indicator plugin for a Minecraft server. The Minecraft server can be any machine running a java based server such as Bukkit or Spigot. Status messages are sent from this plugin to a RaspberryPi running the udpServerLED executable jar that needs to be paired with this software.

The combined application displays the status of a Minecraft server on external LEDs using Raspberry Pi GPIO pins.


Usage
=====
This repository is a Maven project based on source code built in Eclipse. If you just want to build it without delving into the code then 
just download the completed plugin .jar from the latest release.

udpServerLED runs on the RaspberryPi and needs  manually invoking, example below
sudo java -jar udpServerLED-0.1.jar
This step is usually done first as the Minecraft plugin requires that the Raspberry code is running when selecting the IP address. If the Raspberry is to be logged out of then screen can be used to leave the programme running.

udpClientLED is copied into the Minecraft plugins folder and runs as a standard Minecraft plugin.

Once loaded the IP address of the Raspberry will need to be configured. This can be done two ways.

The first is to edit the config.yml file in the plugins\udpClientLED folder that will be created when the plugin is first run. Follow this by executing the updateLEDIPAddress command (requires op permissions). Take care that although simply reloading the plugin will have the same effect, in this case the plugin will be unable to revert back to last known configuration if the newly set address cannot be reached.
The second is via the setLEDIPAddress [ipAddress] command. Both of these methods will revert to the previously set IP address if the new one is not reachable in a set time out (5 seconds).

Finally, the listLEDIPAddress command will show the currently set IP address

TBC
[https://github.com/EmbeddedPi/udpClientLED/releases](https://github.com/EmbeddedPi/udpClientLED/releases)


Current status
==============
udpServerLED is tested and working. udpClientLED is tested and working.


Hardware
========
Schematic and physical hardware are the same as my previous all-in-one piLED project which can be seen on the project site.

[http://embeddedpi.github.io/piLED/](http://embeddedpi.github.io/piLED/)
