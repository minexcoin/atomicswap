# Node configuration

To start atomic swap process, Minexcoin and Bitcoin nodes should be configured in right way. This page contains configuration manual for nodes. This configuration is applicable for both cryptocurrencies.

#### Testnet

First of all, **testnet** should be activated. Add `testnet=1` to conf file both of nodes. Or type it into command line while starting the nodes, e.g. `minexcoind -testnet`

#### RPC credentials

Add RPC credentials to conf files like this:

```
rpcuser=my_user_name
rpcpassword=my_awesome_password
```

##### or

Type it to command line like this:

`minexcoind ... -rpcuser=my_user_name -rpcpassword=my_awesome_password`

Where `my_user_name` and `my_awesome_password` are your own username and password.

#### ZMQ

Add ZMQ host and port number for notifications from node in file:

```
zmqpubhashblock=tcp://127.0.0.1:dddd
zmqpubhashtx=tcp://127.0.0.1:dddd
```

##### or

Command line:

`minexcoind ... -zmqpubhashblock=tcp://127.0.0.1:dddd -zmqpubhashtx=tcp://127.0.0.1:dddd`

Where `dddd` is a port number (Numbers from 1024 to 65535 except numbers from [this table](https://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers)).

#### Starting as a daemon

It's optional item. Enable it if you want start node as a daemon.

```
daemon=1
```

#### Conclusion

If all configurations are set your conf file should be similar to this:

```
daemon=1
testnet=1
rpcuser=my_user_name
rpcpassword=my_awesome_password
zmqpubhashblock=tcp://127.0.0.1:dddd
zmqpubhashtx=tcp://127.0.0.1:dddd
```

Or command line like this:

```
minexcoind -testnet -rpcuser=my_user_name -rpcpassword=my_awesome_password \
-zmqpubhashblock=tcp://127.0.0.1:dddd -zmqpubhashtx=tcp://127.0.0.1:dddd
```