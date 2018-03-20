# Minexcoin cross-chain atomic swap

Atomic swap is a feature to make exchange of cryptocurrencies across different blockchains without trusted third party. Atomic isn't mean instant exchange at one moment but continues process with finished two-sides transactions.

Here is source code of application for automated atomic swap between Minexcoin and Bitcoin blockchains in this repo. All actions of the application are performed automatically with no manual actions. The main thing is configuration needed to run a process.

**Keep in mind, atomic swap currently goes on testnet of each cryptocurrency. We are not resposible for running atomic swap on any mainnet!**

## Configure
All configurations appears in main function. Customize it according to your situation and environment.

### Example
Let's imagine that we are making atomic swap between Minexcoin and Bitcoin blockchains. Configuration parameters are listed below. There are two parts of configuration - for you (`A`) and for your partner (`B`). Each of participants should set both but in different order.
In this example `A` sends Minexcoin and receives Bitcoins. `B` is vise versa.

##### Configuration for `A`
`A`'s IP address and port to connect with `B` via socket:
```java
selfData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", dddd));
```

IP address and port of Minexcoin node RPC:
```java
selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 17788));
```

Username and password for Minexcoin node RPC:
```java
selfData.nodeLogin("user");
selfData.nodePassword("password");
```

Port number for ZMQ notification from Minexcoin node:
```java
selfData.notificationPort(1112);
```

Amount of Minexcoins to send (in satoshi's):
```java
selfData.amount(Coin.valueOf(1000000000));
```

`A`'s Minexcoin private key (uncompressed, HEX formatted):
```java
selfData.myKey(ECKey.fromPrivate(new BigInteger("<private_key_here>", 16), false));
```

`B`'s Minexcoin public key (uncompressed, HEX formatted):
```java
selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key_here>", 16).toByteArray()));
```

Minexcoin worker class instance to work with Minexcoin blockchain:
```java
selfData.worker(MinexCoinWorker.instance());
```

Amount of confirmations to assume that Minexcoin transaction is mature and amount of confirmations (CSV) to make refund transaction back:
```java
selfData.confirmations(2);
selfData.csv(3);
```

Partner data configuration is similar except another cryptocurrency blockchain aspects.

##### Configuration for `B`
`B`'s IP address and port to connect with `A` via socket:
```java
partnerData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", dddd));
```

IP address and port of Bitcoin node RPC:
```java
partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 18332));
```

Username and password for Bitcoin node RPC:
```java
partnerData.nodeLogin("user");
partnerData.nodePassword("password");
```

Port number for ZMQ notification from Bitcoin node:
```java
partnerData.notificationPort(1113);
```

Amount of Bitcoins to receive (in satoshi's):
```java
partnerData.amount(Coin.valueOf(1000000000));
```

`A`'s Bitcoin private key (uncompressed, HEX formatted):
```java
partnerData.myKey(ECKey.fromPrivate(new BigInteger("<private_key_here>", 16), false));
```

`B`'s Bitcoin public key (uncompressed, HEX formatted):
```java
partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key_here>", 16).toByteArray()));
```

Bitcoin worker class instance to work with Bitcoin blockchain:
```java
partnerData.worker(BitcoinWorker.instance());
```

Amount of confirmations to assume that Bitcoin transaction is mature and amount of confirmations (CSV) to make refund transaction back:
```java
partnerData.confirmations(2);
partnerData.csv(3);
```