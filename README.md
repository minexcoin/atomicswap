# Minexcoin cross-chain atomic swap

Atomic swap is a feature to make exchange of cryptocurrencies across different blockchains without trusted third party. Atomic isn't mean instant exchange at one moment but continues process with finished two-sides transactions.

Here is source code of application for automated atomic swap between Minexcoin and Bitcoin blockchains in this repo. All actions of the application are performed automatically with no manual actions. The main thing is configuration needed to run a process.

**Keep in mind, atomic swap currently goes on testnet of each cryptocurrency. We are not responsible for running atomic swap on any mainnet!**

Follow [this link](GUI.md) for GUI version manual or [this link](Lib.md) for library version.

## Additional requirements

Before using atomic swap be sure that you has JRE 8 (Java Runtime Environment) or later. If you don't know is any JRE present on your system or what version it is, type in terminal `java -version`.

If no JRE is present you may download it from [official page](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) and install it to your system.