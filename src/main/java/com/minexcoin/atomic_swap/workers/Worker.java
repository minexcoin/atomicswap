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

package com.minexcoin.atomic_swap.workers;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.script.Script;

public interface Worker {

    public static class Unit {

        private static final Unit Instance = new Unit();

        private Unit() {}

        public static Unit unit() {
            return Instance;
        }

        @Override
        public String toString() {
            return "unit";
        }

    }

    TransactionOutPoint sendToAddress(
        final Address address, final Coin amount,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException;

    void sendTx(
        final Transaction transaction,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException;

    RunnableFuture<Optional<Unit>> waitTxMature(
        final int listeners,
        final Sha256Hash txHash, final int confirmations,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) throws MalformedURLException;

    RunnableFuture<Optional<TransactionOutPoint>> waitPartnerTx(
        final int listeners,
        final Address address, final Coin amount,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) throws MalformedURLException;

    RunnableFuture<Optional<TransactionOutPoint>> waitPartnerTx(
        final int listeners, final Sha256Hash txHash,
        final Address address, final Coin amount,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) throws MalformedURLException;

    RunnableFuture<Optional<Sha256Hash>> waitTxSecret(
        final int listeners,
        final TransactionOutPoint txOutPoint,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) throws MalformedURLException;

    OptionalInt auditTx(
        final Sha256Hash txHash, final Address address,
        final Coin amount, final int confirmations,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException;

    Optional<Sha256Hash> extractSecret(
        final Sha256Hash txHash,
        final TransactionOutPoint txOutPoint,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException;

    Script createFundingScript(final ECKey keyForHash, final ECKey keyForCSV,
        final Sha256Hash secretHash, final int csv);

    Address createP2SHAddress(final Script script, final NetworkParameters params);

    default Address createP2SHAddress(final ECKey keyForHash, final ECKey keyForCSV,
        final Sha256Hash secretHash, final int csv, final NetworkParameters params) {
        return createP2SHAddress(createFundingScript(keyForHash, keyForCSV, secretHash, csv), params);
    }

    Transaction createSpendingTx(
        final TransactionOutPoint txOutPoint,
        final ECKey recipient, final Coin amount, final NetworkParameters params,
        final ECKey key, final Script script, final Sha256Hash secret);

    Transaction createRefundingTx(
        final TransactionOutPoint txOutPoint,
        final ECKey recipient, final Coin amount, final NetworkParameters params,
        final ECKey key, final Script script, final int csv);

    String name();

    String ticker();

}