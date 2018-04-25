package com.minexcoin.atomic_swap.net_params;

import org.bitcoinj.params.TestNet3Params;

public class LitecoinTestNet3Params extends TestNet3Params {
    private final int rpcPort;
    private final int powTargetSpacing;

    public LitecoinTestNet3Params() {
        super();
        p2shHeader = 58;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        rpcPort = 19332;
        powTargetSpacing = (int) 2.5 * 60;
    }

    private static LitecoinTestNet3Params instance;
    public static synchronized LitecoinTestNet3Params get() {
        if (instance == null) {
            instance = new LitecoinTestNet3Params();
        }
        return instance;
    }

    /**
     * Get Litecoin's node RPC port number.
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
