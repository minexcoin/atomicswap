# Atomic Swap GUI explanation

Atomic swap process needs a quantity of parameters should be set. That's quite inconvenient to set it just in code. GUI was developed to make atomic swap process more friendly. However GUI forms has a lot of fields.

Here's an explanation for atomic swap GUI components.

### IP addresses

First of all IP addresses of both participants should be set. For this purpose there are two blocks of fields named **My inet settings** and **Partner inet settings**. These blocks represent yours IP address(named *Host*) and port number(named *Port*) and your partner accordingly.

Port number may be random number, but be sure that it is in range from 1024 to 65535 and don't overlapping any port number from [this table](https://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers).

Provide your IP address and port number to your partner and ask partner to do the same for you. And fill up each field from both blocks.

Tip: *If you don't know your IP address, you may visit any source displaying your IP address, e.g. [ICanHazIP](http://icanhazip.com/)*.

### Minexcoin settings

Block of fields named **Minexcoin settings** should contain data related to your minexcoin node. Description of each field is listed below.

Tip: *Before setting up some of listed fields read the [node configuration](Node-conf.md) manual first to configure your node properly*.

**Login** - RPC access credential, should be equal to "rpcuser" parameter typed in configuration file or in terminal command line on starting the node.

**Password** - RPC access credential, should be equal to "rpcpassword" parameter typed in configuration file or in terminal command line on starting the node.

**Notification port** - is a ZMQ port number to receive event message from node.

**My private key** - your private key (uncompressed, raw) for spending transaction to yourself.

**Partner public key** - partner's public key (uncompressed, raw) for spending transaction to partner.

**Amount** - amount of coins (**in shatoshi**) you want to spend or receive.

**Confirmations** - number of confirmations needed to assume transaction is trusted.

**Expire** - number of confirmations needed to refunding your transaction back to you. Should be greater than **Confirmations'** value (*Be sure that time needed to achieve this number of confirmations on your blockchain is as close to your partner's blockchain time as possible*).

### Bitcoin settings

**Bitcoin settings** block is actually the same as **Mminexcoin settings** but for Bitcoin node.

### Action

You should choose correct action to perform with coins. Select one of two actions - **Sell Minexcoin**/**Buy Minexcoin** in drop down list.

### Run

1. Fill up all fields of window with actual data.
2. Select right action from drop-down list.
3. Press **Exchange** button.
4. Follow on execution.