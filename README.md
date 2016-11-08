udpClientLED
============

A status indicator plugin for a Minecraft server. The Minecraft server can be any machine running a java based server such as Bukkit or Spigot. Status messages are sent from this plugin to a RaspberryPi running the udpServerLED executable jar that needs to be paired with this software.

The combined application displays the status of a Minecraft server on external LEDs using Raspberry Pi GPIO pins.


Usage
=====
This repository is a Maven project based on source code built in Eclipse. If you just want to build it without delving into the code then just download the completed plugin .jar from the latest release.

udpServerLED runs on the RaspberryPi and needs  manually invoking, example below
sudo java -jar udpServerLED-0.1.jar
This step is usually done first as the Minecraft plugin requires that the Raspberry code is running when selecting the IP address. If the Raspberry is to be logged out of then screen can be used to leave the programme running.

udpClientLED is copied into the Minecraft plugins folder and runs as a standard Minecraft plugin.

Once loaded the IP address of the Raspberry will need to be configured. This can be done two ways.

The first is to edit the config.yml file in the plugins\udpClientLED folder that will be created when the plugin is first run. Follow this by executing the updateConfig command (requires op permissions). Take care that although simply reloading the plugin will have the same effect, in this case the plugin will be unable to revert back to last known configuration if the newly set address cannot be reached and will revert to the loopback default of 127.0.0.1.

The second is via the setUDPIPAddress [ipAddress] command. This method will revert to the previously set IP address if the new one is not reachable in a set time out (default is 5 seconds).

There are two configurable timeout settings. These can be altered to fine tune response times for the network.

setUDPTimeout [integer] (default 5000) is used when contacting a newly set IP address for the first time.
setUDPShortTimeout [integer] (default 500) is used when communicating with a known contactable IP address.

Finally, the listUDPConfig command will show the current settings.


Releases
--------
[https://github.com/EmbeddedPi/udpClientLED/releases](https://github.com/EmbeddedPi/udpClientLED/releases)


Current status
==============
udpServerLED is tested and working. udpClientLED is currently migrating to using a config file and under testing.
The plugin assumes that the local network addresses are of the form 192.168.1.x


Hardware
========
Schematic and physical hardware are the same as my previous all-in-one piLED project which can be seen on the project site.

[http://embeddedpi.github.io/piLED/](http://embeddedpi.github.io/piLED/)
