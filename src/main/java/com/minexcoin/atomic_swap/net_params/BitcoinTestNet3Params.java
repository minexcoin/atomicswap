package com.minexcoin.atomic_swap.net_params;

import org.bitcoinj.params.TestNet3Params;

public class BitcoinTestNet3Params extends TestNet3Params {
    private final int rpcPort;
    private final int powTargetSpacing;

    public BitcoinTestNet3Params() {
        super();
        rpcPort = 18332;
        powTargetSpacing = 10 * 60;
    }

    private static BitcoinTestNet3Params instance;
    public static synchronized BitcoinTestNet3Params get() {
        if (instance == null) {
            instance = new BitcoinTestNet3Params();
        }
        return instance;
    }

    /**
     * Get Bitcoin's node RPC port number.
     *
     * @return Integer
     * */
    public int getRpcPort() {
        return rpcPort;
    }

    /**
     * Get POW target spacing.
     * */
    public int getPowTargetSpacing() {
        return powTargetSpacing;
    }
}
