# flashwifi

Share remaining data from your android phone as a hotspot and get paid in iota.

[WiFiota promotion website](https://tobywoerthle.github.io/flashWiFiSite/)

# How does it work?

## Iota setup

The app creates a seed for every user and stores it with a password in SharedEncryptedPreferences.
There is a withdraw and deposit view which can be used to interact with the seed.

So far everything runs on a testnode but a live node can be set.

## Discovery

Happens with WiFi P2P. The consumer selects a peer manually so far (-> in the future automatic service discovery)
Two devices then open a wifi direct group and start the **Negotiation Protocol**

## Negotiation Protocol

The devices talk about each others, how much data they want to share or consume and
whether they could agree on a price and time.

* If so the future hotspot generates a random SSID like "Iotidy-<random number>".
* And generates a random number which is used as the password
* The hotspot sends it to the consumer and now they start the Billing Protocol

ToDo: Add full protocol

## Billing Protocol

The hotspot is created and the device proving the hotspot starts a new ServerSocket.
Once the consumer is connected to the hotspot he tries to connect to the gateway and
start the Billing Protocol.

* "Hello I am Client and I am in state NOT_PAIRED"
* "Hello, Client! I am Hotspot and I am in state NOT_PAIRED"
...

(ToDo: add full protocol)

### Payment

Every minute a bill is exchanged which contains information about used megabytes and durances.
A **IOTA FLASH CHANNEL** is established between the two (-> in the future more than two parties).
Every minute can be associated with a token transfer in the channel.

* Signing a transfer signifies agreement to the the bill
* The funding of the channel results in shared risk on both parties so they have no incentive to leave earlier
* Time guards track whether timing constraints weren't fulfilled (channel not funded, no peers...)


### End

Every bill can contain a "closeAfterwards" flag. Which ends the whole process with a short
exchange of data for the consumer.
The hotspot still has to attach the flash channel to the tangle.

# Good to know
The WiFi P2P roles in a WiFi group are not fixed. So each time either the future hotspot or consumer act as server and client.

We wrapped the iota.flash.js with additional javascript bridges and connect them to the **Jota - iota java lib**. The javascript code for the flash channel is executed within J2V8.


# Next big steps

* [ ] Implement an automatic Wifiota hotspot discovery
* [ ] Implement iota.flash.js in Java
* [ ] Build a hardware prototype for Wifiota hotspot (Raspy + data plan + battery)
* [ ] Extend the protocol to multiple parties, endless roaming and backoffice functions to close conflicts after physical contacts isn't possible anymore



Total: 17d

