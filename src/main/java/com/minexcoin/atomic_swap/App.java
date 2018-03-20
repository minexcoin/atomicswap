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
import com.minexcoin.atomic_swap.workers.MinexCoinWorker;
import com.minexcoin.atomic_swap.workers.BitcoinWorker;

public class App
{

    public static void main(String[] args) throws Exception {
        init();
//        execute();

        final Data selfData = new SimpleData();
        final Data partnerData = new SimpleData();
        switch(args[0]) {
            case "user1": {
                selfData.inetAddress(new InetSocketAddress("139.59.154.41", 5556));
                selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 17788));
                selfData.nodeLogin("user");
                selfData.nodePassword("password");
                selfData.notificationPort(7778);
                selfData.amount(Coin.valueOf(1000000));
                selfData.myKey(ECKey.fromPrivate(new BigInteger("2C071FC555A7F4801575813283D2136CA3364C10EDC7BE97B5E66F04C51D039E", 16), false));
                selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("0485152F0A268B00743D9456A8E6C0CC2C00F65D8BB8FFAE880DD129F14A21A772F11DE4173A02E277C8A42B323690E5F6EFD5289E0F0CA7C37A46CD6CBD784913", 16).toByteArray()));
                selfData.worker(MinexCoinWorker.instance());
                selfData.confirmations(0);
                selfData.csv(6);
                selfData.netParams(TestNet3Params.get());

                partnerData.inetAddress(new InetSocketAddress("138.197.90.246", 5555));
                partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 18332));
                partnerData.nodeLogin("user");
                partnerData.nodePassword("password");
                partnerData.notificationPort(7777);
                partnerData.amount(Coin.valueOf(1000000));
                partnerData.myKey(ECKey.fromPrivate(new BigInteger("54B11C48CDF32EA5F4F066590C2A23DD0D80EBAD3DFC42C02F400E3709B8E2AB", 16), false));
                partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("04EF29BE874235B47C05365DB56C27A2AAB26DD67DB9A092A0A9A7A74BA162D7F1B9C309F2DACB5FA726A19BEFA28B9A2A9436698BE2551323066690FB605660CD", 16).toByteArray()));
                partnerData.worker(BitcoinWorker.instance());
                partnerData.confirmations(0);
                partnerData.csv(1);
                partnerData.netParams(TestNet3Params.get());

                break;
            }

            case "user2": {
                selfData.inetAddress(new InetSocketAddress("138.197.90.246", 5555));
                selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 18332));
                selfData.nodeLogin("user");
                selfData.nodePassword("password");
                selfData.notificationPort(7777);
                selfData.amount(Coin.valueOf(1000000));
                selfData.myKey(ECKey.fromPrivate(new BigInteger("49B7ADB77E3678E784E96691589C40E287ADE91B4725C4B7EFABE6CEEC615FAB", 16), false));
                selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("04391D2BD9FDAEDF7EAB8150A08C6ECCA3C08AAAD81D7FA0A11C5DEE1D7EFB27752B73A6F26DA0861173AEB4C36F7787B38F2ED7804E2A7E9D8B25729049DE991F", 16).toByteArray()));
                selfData.worker(BitcoinWorker.instance());
                selfData.confirmations(0);
                selfData.csv(1);
                selfData.netParams(TestNet3Params.get());

                partnerData.inetAddress(new InetSocketAddress("139.59.154.41", 5556));
                partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 17788));
                partnerData.nodeLogin("user");
                partnerData.nodePassword("password");
                partnerData.notificationPort(7778);
                partnerData.amount(Coin.valueOf(1000000));
                partnerData.myKey(ECKey.fromPrivate(new BigInteger("044FE27D721ACCDFBCF568E27FE69F1912FCFDDD307B73182D70E25A6DBA36CE", 16), false));
                partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("04DFE5CC4ADAEAE339F9EC34A8E457DFE78FB90301E0800535C6728A5A03A3661D9CC93828511332FCD78A92A0BA8EF134AC4D3FB2ADB2D573DEEFBC8518752868", 16).toByteArray()));
                partnerData.worker(MinexCoinWorker.instance());
                partnerData.confirmations(0);
                partnerData.csv(6);
                partnerData.netParams(TestNet3Params.get());

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
                        System.out.println("----secret " + state.selfData().secretHash());
                        System.out.println("----funding tx " + state.selfData().txOutPoint());
                        break;
                    }
                    case BuyerTx: {
                        System.out.println("----funding tx " + state.selfData().txOutPoint());
                        break;
                    }
                    case Finish: {
                        if (state.selfData().closeTx() != null) {
                            System.out.println("----close tx " + state.selfData().closeTx());
                        } else {
                            System.out.println("----close tx " + state.partnerData().closeTx());
                        }
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

    private static void init() {
        BriefLogFormatter.init();
//        Logger log = LoggerFactory.getLogger(App.class);
//        log.info("Example");
    }

    private static void execute() throws Exception {

        new Thread(() -> {
            final Data selfData = new SimpleData();
            selfData.inetAddress(new InetSocketAddress("192.168.0.14", 5555));
            selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 17791));
            selfData.nodeLogin("user");
            selfData.nodePassword("password");
            selfData.notificationPort(7777);
            selfData.amount(Coin.valueOf(100000000));
            selfData.myKey(ECKey.fromPrivate(new BigInteger("49B7ADB77E3678E784E96691589C40E287ADE91B4725C4B7EFABE6CEEC615FAB", 16), false));
            selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("04391D2BD9FDAEDF7EAB8150A08C6ECCA3C08AAAD81D7FA0A11C5DEE1D7EFB27752B73A6F26DA0861173AEB4C36F7787B38F2ED7804E2A7E9D8B25729049DE991F", 16).toByteArray()));
            selfData.worker(MinexCoinWorker.instance());
            selfData.confirmations(2);
            selfData.csv(3);
            selfData.netParams(TestNet3Params.get());

            final Data partnerData = new SimpleData();
            partnerData.inetAddress(new InetSocketAddress("192.168.0.14", 5556));
            partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 17792));
            partnerData.nodeLogin("user");
            partnerData.nodePassword("password");
            partnerData.notificationPort(7778);
            partnerData.amount(Coin.valueOf(100000000));
            partnerData.myKey(ECKey.fromPrivate(new BigInteger("044FE27D721ACCDFBCF568E27FE69F1912FCFDDD307B73182D70E25A6DBA36CE", 16), false));
            partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("04DFE5CC4ADAEAE339F9EC34A8E457DFE78FB90301E0800535C6728A5A03A3661D9CC93828511332FCD78A92A0BA8EF134AC4D3FB2ADB2D573DEEFBC8518752868", 16).toByteArray()));
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
            selfData.inetAddress(new InetSocketAddress("192.168.0.14", 5556));
            selfData.nodeAddress(new InetSocketAddress("127.0.0.1", 17792));
            selfData.nodeLogin("user");
            selfData.nodePassword("password");
            selfData.notificationPort(7778);
            selfData.amount(Coin.valueOf(100000000));
            selfData.myKey(ECKey.fromPrivate(new BigInteger("2C071FC555A7F4801575813283D2136CA3364C10EDC7BE97B5E66F04C51D039E", 16), false));
            selfData.otherKey(ECKey.fromPublicOnly(new BigInteger("0485152F0A268B00743D9456A8E6C0CC2C00F65D8BB8FFAE880DD129F14A21A772F11DE4173A02E277C8A42B323690E5F6EFD5289E0F0CA7C37A46CD6CBD784913", 16).toByteArray()));
            selfData.worker(MinexCoinWorker.instance());
            selfData.confirmations(2);
            selfData.csv(3);
            selfData.netParams(TestNet3Params.get());

            final Data partnerData = new SimpleData();
            partnerData.inetAddress(new InetSocketAddress("192.168.0.14", 5555));
            partnerData.nodeAddress(new InetSocketAddress("127.0.0.1", 17791));
            partnerData.nodeLogin("user");
            partnerData.nodePassword("password");
            partnerData.notificationPort(7777);
            partnerData.amount(Coin.valueOf(100000000));
            partnerData.myKey(ECKey.fromPrivate(new BigInteger("54B11C48CDF32EA5F4F066590C2A23DD0D80EBAD3DFC42C02F400E3709B8E2AB", 16), false));
            partnerData.otherKey(ECKey.fromPublicOnly(new BigInteger("04EF29BE874235B47C05365DB56C27A2AAB26DD67DB9A092A0A9A7A74BA162D7F1B9C309F2DACB5FA726A19BEFA28B9A2A9436698BE2551323066690FB605660CD", 16).toByteArray()));
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
//                error -> {System.out.println(1 + " " + error.getMessage());}
            );

            fsm.start();
        }).start();

    }

}