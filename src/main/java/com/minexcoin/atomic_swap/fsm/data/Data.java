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

package com.minexcoin.atomic_swap.fsm.data;

import java.net.InetSocketAddress;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.TransactionOutPoint;

import com.minexcoin.atomic_swap.workers.Worker;

public interface Data extends Cloneable {

    InetSocketAddress inetAddress();

    InetSocketAddress nodeAddress();

    String nodeLogin();

    String nodePassword();

    int notificationPort();

    Coin amount();

    ECKey myKey();

    ECKey otherKey();

    Worker worker();

    Sha256Hash secret();

    Sha256Hash secretHash();

    int confirmations();

    int csv();

    NetworkParameters netParams();

    TransactionOutPoint txOutPoint();

    Sha256Hash closeTx();

    void inetAddress(final InetSocketAddress inetAddress);

    void nodeAddress(final InetSocketAddress nodeAddress);

    void nodeLogin(final String login);

    void nodePassword(final String password);

    void notificationPort(final int port);

    void amount(final Coin amount);

    void myKey(final ECKey key);

    void otherKey(final ECKey key);

    void worker(final Worker worker);

    void secret(final Sha256Hash secret);

    void secretHash(final Sha256Hash hash);

    void confirmations(int confirmations);

    void csv(final int csv);

    void netParams(final NetworkParameters netParams);

    void txOutPoint(final TransactionOutPoint point);

    void closeTx(final Sha256Hash closeTx);

    Data clone();

}