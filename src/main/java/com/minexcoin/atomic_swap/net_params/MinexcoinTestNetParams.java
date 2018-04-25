package com.minexcoin.atomic_swap.net_params;

import org.bitcoinj.params.TestNet3Params;

public class MinexcoinTestNetParams extends TestNet3Params {
    private final int rpcPort;
    private final int powTargetSpacing;

    public MinexcoinTestNetParams() {
        super();
        rpcPort = 17788;
        powTargetSpacing = 3 * 60;
    }

    private static MinexcoinTestNetParams instance;
    public static synchronized MinexcoinTestNetParams get() {
        if (instance == null) {
            instance = new MinexcoinTestNetParams();
        }
        return instance;
    }

    /**
     * Get Minexcoin's node RPC port number.
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
