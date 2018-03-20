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

public final class SimpleData implements Data {

    @Override
    public InetSocketAddress inetAddress() {
        return inetAddress_;
    }

    @Override
    public InetSocketAddress nodeAddress() {
        return nodeAddress_;
    }

    @Override
    public String nodeLogin() {
        return nodeLogin_;
    }

    @Override
    public String nodePassword() {
        return nodePassword_;
    }

    @Override
    public int notificationPort() {
        return notificationPort_;
    }

    @Override
    public Coin amount() {
        return amount_;
    }

    @Override
    public ECKey myKey() {
        return myKey_;
    }

    @Override
    public ECKey otherKey() {
        return otherKey_;
    }

    @Override
    public Worker worker() {
        return worker_;
    }

    @Override
    public Sha256Hash secret() {
        return secret_;
    }

    @Override
    public Sha256Hash secretHash() {
        return secretHash_;
    }

    @Override
    public int confirmations() {
        return confirmations_;
    }

    @Override
    public int csv() {
        return csv_;
    }

    @Override
    public NetworkParameters netParams() {
        return netParams_;
    }

    @Override
    public TransactionOutPoint txOutPoint() {
        return txOutPoint_;
    }

    @Override
    public Sha256Hash closeTx() {
        return closeTx_;
    }

    @Override
    public void inetAddress(final InetSocketAddress address) {
        inetAddress_ = address;
    }

    @Override
    public void nodeAddress(final InetSocketAddress address) {
        nodeAddress_ = address;
    }

    @Override
    public void nodeLogin(final String login) {
        nodeLogin_ = login;
    }

    @Override
    public void nodePassword(final String password) {
        nodePassword_ = password;
    }

    @Override
    public void notificationPort(final int port) {
        notificationPort_ = port;
    }

    @Override
    public void amount(final Coin amount) {
        amount_ = amount;
    }

    @Override
    public void myKey(final ECKey key) {
        myKey_ = key;
    }

    @Override
    public void otherKey(final ECKey key) {
        otherKey_ = key;
    }

    @Override
    public void worker(final Worker worker) {
        worker_ = worker;
    }

    @Override
    public void secret(final Sha256Hash secret) {
        secret_ = secret;
        secretHash_ = Sha256Hash.of(secret_.getBytes());
    }

    @Override
    public void secretHash(final Sha256Hash hash) {
        secret_ = null;
        secretHash_ = hash;
    }

    @Override
    public void confirmations(final int confirmations) {
        confirmations_ = confirmations;
    }

    @Override
    public void csv(final int csv) {
        csv_ = csv;
    }

    @Override
    public void netParams(final NetworkParameters netParams) {
        netParams_ = netParams;
    }

    @Override
    public void txOutPoint(final TransactionOutPoint point) {
        txOutPoint_ = point;
    }

    @Override
    public void closeTx(final Sha256Hash closeTx) {
        closeTx_ = closeTx;
    }

    @Override
    public SimpleData clone() {
        try {
            return (SimpleData)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Must be cloneable");
        }
    }

    private InetSocketAddress inetAddress_;
    private InetSocketAddress nodeAddress_;
    private String nodeLogin_;
    private String nodePassword_;
    private int notificationPort_;
    private Coin amount_;
    private ECKey myKey_;
    private ECKey otherKey_;
    private Worker worker_;
    private Sha256Hash secret_;
    private Sha256Hash secretHash_;
    private int confirmations_;
    private int csv_;
    private NetworkParameters netParams_;
    private TransactionOutPoint txOutPoint_;
    private Sha256Hash closeTx_;

}
