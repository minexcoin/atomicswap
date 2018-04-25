// Copyright (c) 2018 MinexSystems Limited
// Distributed under the MIT software license, see the accompanying
// file COPYING or http://www.opensource.org/licenses/mit-license.php.
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the
// Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject
// to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
// ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
// THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.minexcoin.atomic_swap;

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.utils.BriefLogFormatter;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import com.minexcoin.fsm.FSM;
import com.minexcoin.atomic_swap.fsm.data.Data;
import com.minexcoin.atomic_swap.fsm.data.SimpleData;
import com.minexcoin.atomic_swap.fsm.states.TxState;
import com.minexcoin.atomic_swap.fsm.states.TxStatus;
import com.minexcoin.atomic_swap.net_params.LitecoinTestNet3Params;
import com.minexcoin.atomic_swap.net_params.MinexcoinTestNetParams;
import com.minexcoin.atomic_swap.workers.MinexCoinWorker;
import com.minexcoin.atomic_swap.workers.BitcoinWorker;

public class App
{
	/**
	 * Run predefined test on one PC or custom test with partner.
	 * */
    public static void main(String[] args) throws Exception {
        init();
        // Run predefined test.
        // execute();

        // Set up the needed case and specify it on starting the app.
        // Use only one case during running swap process.
        final Data selfData = new SimpleData();
        final Data partnerData = new SimpleData();
        switch(args[0]) {
            case "user1": {
                selfData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5556));
                selfData.nodeAddress(new InetSocketAddress("127.0.0.1", MinexcoinTestNetParams.get().getRpcPort()));
                selfData.nodeLogin("user");
                selfData.nodePassword("password");
                selfData.notificationPort(7778);
                selfData.amount(Coin.valueOf(200000));
                selfData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
                selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
                selfData.worker(MinexCoinWorker.instance());
                selfData.confirmations(0);
                selfData.csv(6);
                selfData.netParams(MinexcoinTestNetParams.get());

                partnerData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5555));
                // partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", BitcoinTestNet3Params.get().getRpcPort())); // For Bitcoin.
                partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", LitecoinTestNet3Params.get().getRpcPort())); // For Litecoin.
                partnerData.nodeLogin("user");
                partnerData.nodePassword("password");
                partnerData.notificationPort(7777);
                partnerData.amount(Coin.valueOf(100000));
                partnerData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
                partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
                partnerData.worker(BitcoinWorker.instance());
                partnerData.confirmations(0);
                partnerData.csv(1);
                // partnerData.netParams(BitcoinTestNet3Params.get()); // For Bitcoin.
                partnerData.netParams(LitecoinTestNet3Params.get()); // For Litecoin.

                break;
            }

            case "user2": {
                selfData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5555));
                // selfData.nodeAddress(new InetSocketAddress("127.0.0.1", BitcoinTestNet3Params.get().getRpcPort())); // For Bitcoin.
                selfData.nodeAddress(new InetSocketAddress("127.0.0.1", LitecoinTestNet3Params.get().getRpcPort())); // For Litecoin.
                selfData.nodeLogin("user");
                selfData.nodePassword("password");
                selfData.notificationPort(7777);
                selfData.amount(Coin.valueOf(100000));
                selfData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
                selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
                selfData.worker(BitcoinWorker.instance());
                selfData.confirmations(0);
                selfData.csv(1);
                // selfData.netParams(BitcoinTestNet3Params.get()); // For Bitcoin.
                selfData.netParams(LitecoinTestNet3Params.get()); // For Litecoin.

                partnerData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5556));
                partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", MinexcoinTestNetParams.get().getRpcPort()));
                partnerData.nodeLogin("user");
                partnerData.nodePassword("password");
                partnerData.notificationPort(7778);
                partnerData.amount(Coin.valueOf(200000));
                partnerData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
                partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
                partnerData.worker(MinexCoinWorker.instance());
                partnerData.confirmations(0);
                partnerData.csv(6);
                partnerData.netParams(MinexcoinTestNetParams.get());

                break;
            }

            default: throw new RuntimeException("Undefined member");
        }

        final FSM<TxState<TxStatus>> fsm = AtomicSwapFSM.create(
            selfData, partnerData,
            true,
            5, TimeUnit.SECONDS,
            5, TimeUnit.SECONDS,
            1000, "AtomicSwapFSM Event", "AtomicSwapFSM-Task-%d"
        );

        fsm.states().
        subscribe(
            state -> {
                System.out.println(state.status());

                switch(state.status()) {
                    case SellerTx: {
                        System.out.println("- Secret: " + state.selfData().secretHash());
                        System.out.println("- Funding TX out point: " + state.selfData().txOutPoint());
                        break;
                    }
                    case BuyerTx: {
                        System.out.println("- Funding TX out point: " + state.selfData().txOutPoint());
                        break;
                    }
                    case Finish: {
                        System.out.println("- Close TX hash: " + (
                            state.selfData().closeTx() != null ?
                                state.selfData().closeTx() :
                                state.partnerData().closeTx()
                        ));
                        break;
                    }
                default:
                    break;
                }
            },
            Throwable::printStackTrace
        );

        fsm.start();

    }

    /**
     * Init the logger.
     * */
    private static void init() {
        BriefLogFormatter.init();
        // Logger log = LoggerFactory.getLogger(App.class);
        // log.info("Example");
    }

    /**
     * Predefined test. Uses separated threads for both participants.
     *
     * @throws Exception
     * */
    private static void execute() throws Exception {

        new Thread(() -> {
            final Data selfData = new SimpleData();
            selfData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5555));
            selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 17791));
            selfData.nodeLogin("user");
            selfData.nodePassword("password");
            selfData.notificationPort(7777);
            selfData.amount(Coin.valueOf(100000000));
            selfData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
            selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
            selfData.worker(MinexCoinWorker.instance());
            selfData.confirmations(2);
            selfData.csv(3);
            selfData.netParams(TestNet3Params.get());

            final Data partnerData = new SimpleData();
            partnerData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5556));
            partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 17792));
            partnerData.nodeLogin("user");
            partnerData.nodePassword("password");
            partnerData.notificationPort(7778);
            partnerData.amount(Coin.valueOf(100000000));
            partnerData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
            partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
            partnerData.worker(MinexCoinWorker.instance());
            partnerData.confirmations(2);
            partnerData.csv(3);
            partnerData.netParams(TestNet3Params.get());

            final FSM<TxState<TxStatus>> fsm = AtomicSwapFSM.create(
                selfData, partnerData,
                true,
                5, TimeUnit.SECONDS,
                5, TimeUnit.SECONDS,
                1000, "AtomicSwapFSM Event", "AtomicSwapFSM-Task-%d"
            );

            fsm.states().
            subscribe(
                state -> {
                    System.out.println(1 + " " + state.status());
                },
                Throwable::printStackTrace
                //error -> {System.out.println(2 + " " + error.getMessage());}
            );

            fsm.start();
        }).start();

        new Thread(() -> {
            final Data selfData = new SimpleData();
            selfData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5556));
            selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 17792));
            selfData.nodeLogin("user");
            selfData.nodePassword("password");
            selfData.notificationPort(7778);
            selfData.amount(Coin.valueOf(100000000));
            selfData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
            selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
            selfData.worker(MinexCoinWorker.instance());
            selfData.confirmations(2);
            selfData.csv(3);
            selfData.netParams(TestNet3Params.get());

            final Data partnerData = new SimpleData();
            partnerData.inetAddress(new InetSocketAddress("ddd.ddd.ddd.ddd", 5555));
            partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 17791));
            partnerData.nodeLogin("user");
            partnerData.nodePassword("password");
            partnerData.notificationPort(7777);
            partnerData.amount(Coin.valueOf(100000000));
            partnerData.myKey(ECKey.fromPrivate(new BigInteger("<private_key>", 16), false));
            partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("<public_key>", 16).toByteArray()));
            partnerData.worker(MinexCoinWorker.instance());
            partnerData.confirmations(2);
            partnerData.csv(3);
            partnerData.netParams(TestNet3Params.get());

            final FSM<TxState<TxStatus>> fsm = AtomicSwapFSM.create(
                selfData, partnerData,
                true,
                5, TimeUnit.SECONDS,
                5, TimeUnit.SECONDS,
                1000, "AtomicSwapFSM Event", "AtomicSwapFSM-Task-%d"
            );

            fsm.states().
            subscribe(
                state -> {
                    System.out.println(2 + " " + state.status());
                },
                Throwable::printStackTrace
                // error -> {System.out.println(1 + " " + error.getMessage());}
            );

            fsm.start();
        }).start();
    }
}
