# flashwifi

Share remaining data from your android phone as a hotspot and get paid in iota.

[WiFiota Promotional Website](https://tobywoerthle.github.io/flashWiFiSite/)

# How does it work?

## Iota setup

The app creates a seed for a user and stores it with a password in SharedEncryptedPreferences.
There are withdraw and deposit views, which can be used to interact with the seed (send and receive funds).
The balance of the wallet can be viewed and is cached.

A private testnode is used by default but a mainnet (live) node can be set.

When using the testnet, 2000i can be generated and automatically sent to the wallet for testing purposes. See Settings > Add 2000i testnet.

## Discovery

This uses WiFi P2P. The consumer selects a peer manually (-> in the future there will be automatic service discovery).
Two devices then open a wifi direct group and start the **Negotiation Protocol**

## Negotiation Protocol

The devices communicate with each other: how much data they want to share or consume and
whether they can agree upon a price and duration.

* If they come to an agreement, the future hotspot generates a random SSID like "Iotidy-<random number>".
* The future hostpot device also generates a random number which is used as the WiFi password
* The hotspot sends the password to the consumer and now they start the Billing Protocol

ToDo: Add full protocol

## Billing Protocol

The hotspot is created and the device providing the hotspot starts a new ServerSocket.
Once the consumer is connected to the hotspot, they try to connect to the gateway and
start the Billing Protocol.

* "Hello I am Client and I am in state NOT_PAIRED"
* "Hello, Client! I am Hotspot and I am in state NOT_PAIRED"
...

(ToDo: add full protocol)

### Payment

Every minute a bill is exchanged which contains information about used megabytes and duration(s).
A **IOTA FLASH CHANNEL** is established between the two (-> in the future more than two parties will be supported).
Every minute can be associated with a token transfer in the channel.

* Signing a transfer signifies agreement to the the bill
* The funding of the channel results in shared risk between both parties so they have no incentive to leave early
* Time guards track whether timing constraints weren't fulfilled (channel not funded, no peers...)

### End

Every bill can contain a "closeAfterwards" flag, which ends the whole process with a short exchange of data for the consumer.
The hotspot still has to attach the flash channel to the tangle.

# Good to know
The WiFi P2P roles in a WiFi group are not fixed. Therefore, each time, either the future hotspot or consumer can act as server or client.

We wrapped the iota.flash.js with additional javascript bridges and connected them to the **Jota - iota java lib**. The javascript code for the flash channel is executed within J2V8.


# Next big steps

* [ ] Implement an automatic Wifiota hotspot discovery
* [ ] Implement iota.flash.js in Java
* [ ] Build a hardware prototype for Wifiota hotspot (Raspy + data plan + battery)
* [ ] Extend the protocol to multiple parties, endless roaming and backoffice functions to close conflicts after physical contacts isn't possible anymore



Total: 17d
