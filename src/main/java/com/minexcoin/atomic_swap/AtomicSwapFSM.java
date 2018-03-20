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

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Optional;
import java.util.OptionalInt;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;

import com.minexcoin.atomic_swap.fsm.data.Data;
import com.minexcoin.atomic_swap.fsm.states.TxStatus;
import com.minexcoin.atomic_swap.fsm.states.TxState;
import com.minexcoin.atomic_swap.fsm.states.AbstractTxState;
import com.minexcoin.atomic_swap.workers.Worker.Unit;
import com.minexcoin.fsm.FSM;
import com.minexcoin.fsm.GeneralFSM;

public final class AtomicSwapFSM extends GeneralFSM<TxState<TxStatus>> {

    public static FSM<TxState<TxStatus>> create(
        final Data selfData, final Data partnerData,
        final boolean cache,
        final long eventKeepAliveTime, final TimeUnit eventUnit,
        final long taskKeepAliveTime, final TimeUnit taskUnit,
        final int taskLimit,
        final String eventThreadName, final String taskThreadName
    ) {
        return new AtomicSwapFSM(
            selfData, partnerData,
            cache,
            eventKeepAliveTime, eventUnit,
            taskKeepAliveTime, taskUnit,
            taskLimit, eventThreadName, taskThreadName
        );
    }

    private AtomicSwapFSM(
        final Data selfData, final Data partnerData,
        final boolean cache,
        final long eventKeepAliveTime, final TimeUnit eventUnit,
        final long taskKeepAliveTime, final TimeUnit taskUnit,
        final int taskLimit,
        final String eventThreadName, final String taskThreadName
    ) {
        super(
            new InitState(selfData, partnerData),
            cache ? StreamOpts.Always : StreamOpts.Once,
            eventKeepAliveTime, eventUnit,
            taskKeepAliveTime, taskUnit,
            taskLimit, eventThreadName, taskThreadName
        );
    }



    @Actor
    @Override
    protected void startImpl() {
        trigger(
            InitState.class,
            state -> state.onInit(this)
        );
    }

    @Override
    protected boolean isFinal(final TxState<TxStatus> state) {
        return (state instanceof FinishState) ||
                (state instanceof ErrorState);
    }

    @Override
    protected TxState<TxStatus> errorState(
        final TxState<TxStatus> state, final Throwable exception
    ) {
        return new ErrorState(state);
    }



    private static final class InitState extends AbstractTxState<TxStatus> {

        public InitState(final Data selfData, final Data partnerData) {
            super(selfData, partnerData, TxStatus.Init);
        }

        @Event
        public void onInit(final AtomicSwapFSM fsm) throws Throwable {
            final int cmp = (
                selfData().inetAddress().getAddress().getHostAddress() + ":" +
                selfData().inetAddress().getPort()
            ).compareTo(
                partnerData().inetAddress().getAddress().getHostAddress() + ":" +
                partnerData().inetAddress().getPort()
            );

            if (cmp == 0) {
                throw new IllegalArgumentException(
                    "Self and partner inet address are equal: " +
                    selfData().inetAddress().getAddress().getHostAddress() + ":" +
                    selfData().inetAddress().getPort()
                );
            }

            if (selfData().notificationPort() <= 0) {
                throw new IllegalArgumentException(
                    "Self notification port is " + selfData().notificationPort()
                );
            }

            if (partnerData().notificationPort() <= 0) {
                throw new IllegalArgumentException(
                    "Partner notification port is " + partnerData().notificationPort()
                );
            }

            if (selfData().amount().value <= 0) {
                throw new IllegalArgumentException(
                    "Self amount is " + selfData().amount().value
                );
            }

            if (partnerData().amount().value <= 0) {
                throw new IllegalArgumentException(
                    "Partner amount is " + partnerData().amount().value
                );
            }

            if (selfData().amount().value <= TxFee.value) {
                throw new IllegalArgumentException(
                    "Self amount " + selfData().amount().value  +
                    " is less then fee " + TxFee.value
                );
            }

            if (partnerData().amount().value <= TxFee.value) {
                throw new IllegalArgumentException(
                    "Partner amount " + partnerData().amount().value  +
                    " is less then fee " + TxFee.value
                );
            }
            if (selfData().confirmations() < 0) {
                throw new IllegalArgumentException(
                    "Self confirmations is " + selfData().confirmations()
                );
            }

            if (partnerData().confirmations() < 0) {
                throw new IllegalArgumentException(
                    "Partner confirmations is " + partnerData().confirmations()
                );
            }

            if (selfData().csv() <= 0) {
                throw new IllegalArgumentException(
                    "Self CSV is " + selfData().csv()
                );
            }

            if (partnerData().csv() <= 0) {
                throw new IllegalArgumentException(
                    "Partner CSV is " + partnerData().csv()
                );
            }

            if (selfData().confirmations() >= selfData().csv()) {
                throw new IllegalArgumentException(
                    "Self confirmations: " + selfData().confirmations() +
                    " is bigger than csv: " + selfData().csv()
                );
            }

            if (partnerData().confirmations() >= partnerData().csv()) {
                throw new IllegalArgumentException(
                    "Partner confirmations: " + partnerData().confirmations() +
                    " is bigger than csv: " + partnerData().csv()
                );
            }

            fsm.transit(
                fsm.new HandShakeState(this),
                HandShakeState::doHandShake
            );
        }

        @Override
        public InitState copy() {
            return new InitState(selfData(), partnerData());
        }

    }



    private final class HandShakeState extends AbstractTxState<TxStatus> {

        public HandShakeState(final TxState<TxStatus> state) {
            super(state, TxStatus.HandShake);

            amIServer_ = (
                selfData().inetAddress().getAddress().getHostAddress() + ":" +
                selfData().inetAddress().getPort()
            ).compareTo(
                partnerData().inetAddress().getAddress().getHostAddress() + ":" +
                partnerData().inetAddress().getPort()
            ) < 0;
        }

        @Actor
        public void doHandShake() throws Throwable {
            try (
                final Socket socket = connect();
                final InputStream input = socket.getInputStream();
                final OutputStream output = socket.getOutputStream();
            ) {
                checkRequisites(input, output);
                tossCoin(input, output);
            }
        }

        @Event
        public void onISeller() throws Throwable {
            transit(
                new SellerState(this),
                SellerState::doCreateSellerFundingTx
            );
        }

        @Event
        public void onIBuyer() throws Throwable {
            transit(
                new BuyerState(this),
                BuyerState::doWaitSellerFundingTx
            );
        }

        @Override
        public HandShakeState copy() {
            return new HandShakeState(this);
        }



        private Socket connect() throws Exception {
            return amIServer() ?
            createServerSocket(selfData().inetAddress().getPort(), SocketMillisecTimeout) :
            createClientSocket(partnerData().inetAddress());
        }

        private byte[] txRequisitesHash() {
            final Function<Data, String> requisites = (data) -> {
                return Stream.concat(
                    Stream.of(
                        data.inetAddress().toString(),
                        Long.toString(data.amount().value),
                        data.worker().ticker(),
                        Integer.toString(data.confirmations()),
                        Integer.toString(data.csv()),
                        data.netParams().getId()
                    ), (
                        amIServer() ?
                        Stream.of(data.myKey(), data.otherKey()) :
                        Stream.of(data.otherKey(), data.myKey())
                    ).map(ECKey::getPublicKeyAsHex)
                ).collect(Collectors.joining(WordsSeparator));
            };

            final String selfRequisites = requisites.apply(selfData());
            final String partnerRequisites = requisites.apply(partnerData());

            final String totalRequisites = amIServer() ?
            (selfRequisites + partnerRequisites) :
            (partnerRequisites + selfRequisites);

            return Sha256Hash.hash(totalRequisites.getBytes());
        }

        private void checkRequisites(
            final InputStream input, final OutputStream output
        ) throws IOException {
            final byte[] selfTxRequisitesHash = txRequisitesHash();
            output.write(selfTxRequisitesHash);
            output.flush();

            final byte[] partnerTxRequisitesHash = new byte[Sha256Hash.LENGTH];
            input.read(partnerTxRequisitesHash);

            if (!Arrays.equals(selfTxRequisitesHash, partnerTxRequisitesHash)) {
                throw new IllegalArgumentException("Diffrent transaction requisites");
            }
        }

        private void tossCoin(
            final InputStream input, final OutputStream output
        ) throws IOException {
            final byte[] selfSecret = new byte[Sha256Hash.LENGTH];
            new Random().nextBytes(selfSecret);
            output.write(Sha256Hash.hash(selfSecret));
            output.flush();

            final byte[] partnerHash = new byte[Sha256Hash.LENGTH];
            input.read(partnerHash);

            output.write(selfSecret);
            output.flush();

            final byte[] partnerSecret = new byte[Sha256Hash.LENGTH];
            input.read(partnerSecret);

            if (!Arrays.equals(partnerHash, Sha256Hash.hash(partnerSecret))) {
                throw new IllegalArgumentException("Partner secret and hash are not paired");
            }

            // Zip two arrays with xor in one byte
            // and then zip bits with xor in one bit.
            // Result is even that bit or not.
            // If it's even then I am seller otherwise partner is seller.
            final boolean isServerWin = (
            BitSet.valueOf(new byte[] {
                    (byte)IntStream.range(0, Sha256Hash.LENGTH).
                    map(index -> (selfSecret[index] & 0xFF) ^ (partnerSecret[index] & 0xFF)).
                    reduce((lhs, rhs) -> lhs ^ rhs).getAsInt()
                }).
                stream().
                reduce((lhs, rhs) -> lhs ^ rhs).getAsInt() & 0x01
            ) != 1;

            trigger(
                HandShakeState.class,
                (amIServer() ? isServerWin : !isServerWin) ?
                HandShakeState::onISeller :
                HandShakeState::onIBuyer
            );
        }

        private boolean amIServer() {
            return amIServer_;
        }

        private final boolean amIServer_;

    }



    private final class SellerState extends AbstractTxState<TxStatus> {

        public SellerState(final TxState<TxStatus> state) {
            super(state, TxStatus.Seller);
        }

        @Actor
        public void doCreateSellerFundingTx() throws Throwable {
            final byte[] rawSecret = new byte[Sha256Hash.LENGTH];
            new Random().nextBytes(rawSecret);
            selfData().secret(Sha256Hash.wrap(rawSecret));
            partnerData().secret(selfData().secret());

            selfData().txOutPoint(selfData().worker().sendToAddress(
                selfData().worker().createP2SHAddress(
                    selfData().otherKey(),
                    selfData().myKey(),
                    selfData().secretHash(),
                    selfData().csv() * SellerCSVFactor,
                    selfData().netParams()
                ),
                selfData().amount(),
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword()
            ));

            trigger(
                SellerState.class,
                SellerState::onCreatedSellerFundingTx
            );
        }

        @Event
        public void onCreatedSellerFundingTx() throws Throwable {
            transit(
                new SellerTxState(this),
                SellerTxState::doWaitSellerTxMature
            );
        }

        @Override
        public SellerState copy() {
            return new SellerState(this);
        }

    }



    private final class SellerTxState extends AbstractTxState<TxStatus> {

        public SellerTxState(final TxState<TxStatus> state) {
            super(state, TxStatus.SellerTx);
        }

        @Actor
        public void doWaitSellerTxMature() throws Throwable {
            final RunnableFuture<Optional<Unit>> waitTxMatureCompleter =
            selfData().worker().waitTxMature(
                1, selfData().txOutPoint().getHash(),
                selfData().confirmations(),
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword(),
                selfData().notificationPort(),
                0, TimeUnit.MILLISECONDS
            );

            waitTxMatureCompleter.run();
            waitTxMatureCompleter.get();

            try (
                final OutputStream output = createClientSocket(
                    partnerData().inetAddress()
                ).getOutputStream()
            ) {
                output.write(selfData().secretHash().getBytes());
                output.write(selfData().txOutPoint().getHash().getBytes());

                trigger(
                    SellerTxState.class,
                    state -> state.onSellerTxMature(true)
                );
            } catch (final Throwable exception) {
                trigger(
                    SellerTxState.class,
                    state -> state.onSellerTxMature(false)
                );
            }
        }

        @Event
        public void onSellerTxMature(final boolean isSentFundingTx) throws Throwable {
            final AtomicBoolean flag = new AtomicBoolean();

            final RunnableFuture<Optional<TransactionOutPoint>> partnerTxCompleter =
            partnerData().worker().waitPartnerTx(
                Runtime.getRuntime().availableProcessors(),
                partnerData().worker().createP2SHAddress(
                    partnerData().myKey(),
                    partnerData().otherKey(),
                    partnerData().secretHash(),
                    partnerData().csv(),
                    partnerData().netParams()
                ),
                partnerData().amount(),
                partnerData().nodeAddress(),
                partnerData().nodeLogin(), partnerData().nodePassword(),
                partnerData().notificationPort(),
                0, TimeUnit.MILLISECONDS
            );

            final RunnableFuture<Optional<Unit>> txTimeoutCompleter =
            selfData().worker().waitTxMature(
                1, selfData().txOutPoint().getHash(),
                selfData().csv() * 2,
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword(),
                selfData().notificationPort(),
                0, TimeUnit.MILLISECONDS
            );

            transit(
                new SellerMatureState(this, isSentFundingTx),
                Arrays.asList(
                    state -> state.doWaitSellerPartnerTx(
                        flag,
                        partnerTxCompleter,
                        txTimeoutCompleter
                    ),
                    state -> state.doWaitSellerTxTimeout(
                        flag,
                        partnerTxCompleter,
                        txTimeoutCompleter
                    )
                ),
                false
            );
        }

        @Override
        public SellerTxState copy() {
            return new SellerTxState(this);
        }

    }



    private final class SellerMatureState extends AbstractTxState<TxStatus> {

        public SellerMatureState(
            final TxState<TxStatus> state, final boolean isSentFundingTx
        ) {
            super(
                state,
                isSentFundingTx ? TxStatus.SellerMatureSent : TxStatus.SellerMatureUnsent
            );
        }

        public SellerMatureState(final SellerMatureState state) {
            super(state, state.status());
        }

        @Actor
        public void doWaitSellerPartnerTx(
            final AtomicBoolean flag,
            final RunnableFuture<Optional<TransactionOutPoint>> partnerTxCompleter,
            final RunnableFuture<Optional<Unit>> txTimeoutCompleter
        ) throws Throwable {
            partnerTxCompleter.run();
            final TransactionOutPoint txOutPoint = partnerTxCompleter.get().get();

            final RunnableFuture<Optional<Unit>> waitTxMatureCompleter =
            partnerData().worker().waitTxMature(
                1, txOutPoint.getHash(),
                partnerData().confirmations(),
                partnerData().nodeAddress(),
                partnerData().nodeLogin(), partnerData().nodePassword(),
                partnerData().notificationPort(),
                0, TimeUnit.MILLISECONDS
            );

            waitTxMatureCompleter.run();
            waitTxMatureCompleter.get();

            if (!flag.compareAndSet(false, true)) {
                return;
            }

            txTimeoutCompleter.cancel(true);

            partnerData().txOutPoint(txOutPoint);

            trigger(
                SellerMatureState.class,
                SellerMatureState::onSellerSpendingTx
            );
        }

        @Actor
        public void doWaitSellerTxTimeout(
            final AtomicBoolean flag,
            final RunnableFuture<Optional<TransactionOutPoint>> partnerTxCompleter,
            final RunnableFuture<Optional<Unit>> txTimeoutCompleter
        ) throws Throwable {
            txTimeoutCompleter.run();
            txTimeoutCompleter.get();

            if (!flag.compareAndSet(false, true)) {
                return;
            }

            partnerTxCompleter.cancel(true);

            trigger(
                SellerMatureState.class,
                SellerMatureState::onSellerRefundingTx
            );
        }

        @Event
        public void onSellerSpendingTx() throws Throwable {
            transit(
                new CloseSellerTxState(this, true),
                CloseSellerTxState::doSellerSpendingTx
            );
        }

        @Event
        public void onSellerRefundingTx() throws Throwable {
            transit(
                new CloseSellerTxState(this, false),
                CloseSellerTxState::doSellerRefundingTx
            );
        }

        @Override
        public SellerMatureState copy() {
            return new SellerMatureState(this);
        }

    }



    private final class CloseSellerTxState extends AbstractTxState<TxStatus> {

        public CloseSellerTxState(
            final TxState<TxStatus> state, final boolean isSpandable
        ) {
            super(
                state,
                isSpandable ? TxStatus.SellerSpending : TxStatus.SellerRefunding
            );
        }

        public CloseSellerTxState(final CloseSellerTxState state) {
            super(state, state.status());
        }

        @Actor
        public void doSellerSpendingTx() throws Throwable {
            final Transaction transaction = partnerData().worker().createSpendingTx(
                partnerData().txOutPoint(),
                partnerData().myKey(),
                partnerData().amount().minus(TxFee),
                partnerData().netParams(),
                partnerData().myKey(),
                partnerData().worker().createFundingScript(
                    partnerData().myKey(),
                    partnerData().otherKey(),
                    partnerData().secretHash(),
                    partnerData().csv()
                ),
                partnerData().secret()
            );

            partnerData().worker().sendTx(
                transaction,
                partnerData().nodeAddress(),
                partnerData().nodeLogin(), partnerData().nodePassword()
            );

            partnerData().closeTx(transaction.getHash());

            trigger(
                CloseSellerTxState.class,
                CloseSellerTxState::onFinish
            );
        }

        @Actor
        public void doSellerRefundingTx() throws Throwable {
            final Transaction transaction = selfData().worker().createRefundingTx(
                selfData().txOutPoint(),
                selfData().myKey(),
                selfData().amount().minus(TxFee),
                selfData().netParams(),
                selfData().myKey(),
                selfData().worker().createFundingScript(
                    selfData().otherKey(),
                    selfData().myKey(),
                    selfData().secretHash(),
                    selfData().csv() * SellerCSVFactor
                ),
                selfData().csv() * SellerCSVFactor
            );

            selfData().worker().sendTx(
                transaction,
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword()
            );

            selfData().closeTx(transaction.getHash());

            trigger(
                CloseSellerTxState.class,
                CloseSellerTxState::onFinish
            );
        }

        @Event
        public void onFinish() throws Throwable {
            transit(new FinishState(this));
        }

        @Override
        public CloseSellerTxState copy() {
            return new CloseSellerTxState(this);
        }

    }



    private final class BuyerState extends AbstractTxState<TxStatus> {

        public BuyerState(final TxState<TxStatus> state) {
            super(state, TxStatus.Buyer);
        }

        @Actor
        public void doWaitSellerFundingTx() throws Throwable {
            final byte[] rawSecretHash = new byte[Sha256Hash.LENGTH];
            final byte[] rawTxHash = new byte[Sha256Hash.LENGTH];
            try (
                final InputStream input = createServerSocket(
                    selfData().inetAddress().getPort(), WaitTxMillisecTimeout
                ).getInputStream()
            ) {
                input.read(rawSecretHash);
                input.read(rawTxHash);
            }

            final Sha256Hash secretHash = Sha256Hash.wrap(rawSecretHash);
            final Sha256Hash txHash = Sha256Hash.wrap(rawTxHash);

            final RunnableFuture<Optional<TransactionOutPoint>> partnerTxCompleter =
            partnerData().worker().waitPartnerTx(
                Runtime.getRuntime().availableProcessors(),
                txHash,
                partnerData().worker().createP2SHAddress(
                    partnerData().myKey(),
                    partnerData().otherKey(),
                    secretHash,
                    partnerData().csv() * SellerCSVFactor,
                    partnerData().netParams()
                ),
                partnerData().amount(),
                partnerData().nodeAddress(),
                partnerData().nodeLogin(), partnerData().nodePassword(),
                partnerData().notificationPort(),
                WaitTxConfirmedMillisecTimeout, TimeUnit.MILLISECONDS
            );

            partnerTxCompleter.run();
            partnerTxCompleter.get();

            final OptionalInt index = partnerData().worker().auditTx(
                txHash,
                partnerData().worker().createP2SHAddress(
                    partnerData().myKey(),
                    partnerData().otherKey(),
                    secretHash,
                    partnerData().csv() * SellerCSVFactor,
                    partnerData().netParams()
                ),
                partnerData().amount(), 0,
                partnerData().nodeAddress(),
                partnerData().nodeLogin(), partnerData().nodePassword()
            );

            index.orElseThrow(() -> new IllegalArgumentException(
                "Bad partner transaction " + txHash
            ));

            final RunnableFuture<Optional<Unit>> waitTxMatureCompleter =
            partnerData().worker().waitTxMature(
                1, txHash,
                partnerData().confirmations(),
                partnerData().nodeAddress(),
                partnerData().nodeLogin(), partnerData().nodePassword(),
                partnerData().notificationPort(),
                WaitTxConfirmedMillisecTimeout, TimeUnit.MILLISECONDS
            );

            waitTxMatureCompleter.run();
            waitTxMatureCompleter.get().orElseThrow(() -> new IllegalArgumentException(
                "Transaction " + txHash + " is not mature"
            ));

            partnerData().secretHash(secretHash);
            selfData().secretHash(partnerData().secretHash());

            partnerData().txOutPoint(
                new TransactionOutPoint(
                    partnerData().netParams(), index.getAsInt(), txHash
                )
            );

            trigger(
                BuyerState.class,
                BuyerState::onGetSellerFundingTx
            );
        }

        @Event
        public void onGetSellerFundingTx() throws Throwable {
            transit(
                new BuyerHasSellerFundingTxState(this),
                BuyerHasSellerFundingTxState::doCreateBuyerFundingTx
            );
        }

        @Override
        public BuyerState copy() {
            return new BuyerState(this);
        }

    }



    private final class BuyerHasSellerFundingTxState extends AbstractTxState<TxStatus> {

        public BuyerHasSellerFundingTxState(final TxState<TxStatus> state) {
            super(state, TxStatus.BuyerHasSellerTx);
        }

        @Actor
        public void doCreateBuyerFundingTx() throws Throwable {
            selfData().txOutPoint(selfData().worker().sendToAddress(
                selfData().worker().createP2SHAddress(
                    selfData().otherKey(),
                    selfData().myKey(),
                    selfData().secretHash(),
                    selfData().csv(),
                    selfData().netParams()
                ),
                selfData().amount(),
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword()
            ));

            trigger(
                BuyerHasSellerFundingTxState.class,
                BuyerHasSellerFundingTxState::onCreatedBuyerFundingTx
            );
        }

        @Event
        public void onCreatedBuyerFundingTx() throws Throwable {
            transit(
                new BuyerTxState(this),
                BuyerTxState::doWaitBuyerTxMature
            );
        }

        @Override
        public BuyerHasSellerFundingTxState copy() {
            return new BuyerHasSellerFundingTxState(this);
        }

    }



    private final class BuyerTxState extends AbstractTxState<TxStatus> {

        public BuyerTxState(final TxState<TxStatus> state) {
            super(state, TxStatus.BuyerTx);
        }

        @Actor
        public void doWaitBuyerTxMature() throws Throwable {
            final RunnableFuture<Optional<Unit>> waitTxMatureCompleter =
            selfData().worker().waitTxMature(
                1, selfData().txOutPoint().getHash(),
                selfData().confirmations(),
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword(),
                selfData().notificationPort(),
                0, TimeUnit.MILLISECONDS
            );

            waitTxMatureCompleter.run();
            waitTxMatureCompleter.get();

            trigger(
                BuyerTxState.class,
                BuyerTxState::onBuyerTxMature
            );
        }

        @Event
        public void onBuyerTxMature() throws Throwable {
            final AtomicBoolean flag = new AtomicBoolean();

            final RunnableFuture<Optional<Sha256Hash>> txSecretCompleter =
            selfData().worker().waitTxSecret(
                Runtime.getRuntime().availableProcessors(), selfData().txOutPoint(),
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword(),
                selfData().notificationPort(),
                0, TimeUnit.MILLISECONDS
            );

            final RunnableFuture<Optional<Unit>> txTimeoutCompleter =
            selfData().worker().waitTxMature(
                1, selfData().txOutPoint().getHash(),
                selfData().csv(),
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword(),
                selfData().notificationPort(),
                0, TimeUnit.MILLISECONDS
            );

            transit(
                new BuyerMatureState(this),
                Arrays.asList(
                    state -> state.doWaitSellerTxSecret(
                        flag, txSecretCompleter, txTimeoutCompleter
                    ),
                    state -> state.doWaitBuyerTxTimeout(
                        flag, txSecretCompleter, txTimeoutCompleter
                    )
                ),
                false
            );
        }

        @Override
        public BuyerTxState copy() {
            return new BuyerTxState(this);
        }

    }



    private final class BuyerMatureState extends AbstractTxState<TxStatus> {

        public BuyerMatureState(final TxState<TxStatus> state) {
            super(state, TxStatus.BuyerMature);
        }

        public BuyerMatureState(final BuyerMatureState state) {
            super(state, state.status());
        }

        @Actor
        public void doWaitSellerTxSecret(
            final AtomicBoolean flag,
            final RunnableFuture<Optional<Sha256Hash>> txSecretCompleter,
            final RunnableFuture<Optional<Unit>> txTimeoutCompleter
        ) throws Throwable {
            txSecretCompleter.run();
            final Sha256Hash secret = txSecretCompleter.get().get();

            if (!flag.compareAndSet(false, true)) {
                return;
            }

            txTimeoutCompleter.cancel(true);

            selfData().secret(secret);
            partnerData().secret(selfData().secret());

            trigger(
                BuyerMatureState.class,
                BuyerMatureState::onBuyerSpendingTx
            );
        }

        @Actor
        public void doWaitBuyerTxTimeout(
            final AtomicBoolean flag,
            final RunnableFuture<Optional<Sha256Hash>> txSecretCompleter,
            final RunnableFuture<Optional<Unit>> txTimeoutCompleter
        ) throws Throwable {
            txTimeoutCompleter.run();
            txTimeoutCompleter.get();

            if (!flag.compareAndSet(false, true)) {
                return;
            }

            txSecretCompleter.cancel(true);

            trigger(
                BuyerMatureState.class,
                BuyerMatureState::onBuyerRefundingTx
            );
        }

        @Event
        public void onBuyerSpendingTx() throws Throwable {
            transit(
                new CloseBuyerTxState(this, true),
                CloseBuyerTxState::doBuyerSpendingTx
            );
        }

        @Event
        public void onBuyerRefundingTx() throws Throwable {
            transit(
                new CloseBuyerTxState(this, false),
                CloseBuyerTxState::doBuyerRefundingTx
            );
        }

        @Override
        public BuyerMatureState copy() {
            return new BuyerMatureState(this);
        }

    }



    private final class CloseBuyerTxState extends AbstractTxState<TxStatus> {

        public CloseBuyerTxState(
            final TxState<TxStatus> state, final boolean isSpandable
        ) {
            super(
                state,
                isSpandable ? TxStatus.BuyerSpending : TxStatus.BuyerRefunding
            );
        }

        public CloseBuyerTxState(final CloseBuyerTxState state) {
            super(state, state.status());
        }

        @Actor
        public void doBuyerSpendingTx() throws Throwable {
            final Transaction transaction = partnerData().worker().createSpendingTx(
                partnerData().txOutPoint(),
                partnerData().myKey(),
                partnerData().amount().minus(TxFee),
                partnerData().netParams(),
                partnerData().myKey(),
                partnerData().worker().createFundingScript(
                    partnerData().myKey(),
                    partnerData().otherKey(),
                    partnerData().secretHash(),
                    partnerData().csv() * SellerCSVFactor
                ),
                partnerData().secret()
            );

            partnerData().worker().sendTx(
                transaction,
                partnerData().nodeAddress(),
                partnerData().nodeLogin(), partnerData().nodePassword()
            );

            partnerData().closeTx(transaction.getHash());

            trigger(
                CloseBuyerTxState.class,
                CloseBuyerTxState::onFinish
            );
        }

        @Actor
        public void doBuyerRefundingTx() throws Throwable {
            final Transaction transaction = selfData().worker().createRefundingTx(
                selfData().txOutPoint(),
                selfData().myKey(),
                selfData().amount().minus(TxFee),
                selfData().netParams(),
                selfData().myKey(),
                selfData().worker().createFundingScript(
                    selfData().otherKey(),
                    selfData().myKey(),
                    selfData().secretHash(),
                    selfData().csv()
                ),
                selfData().csv()
            );

            selfData().worker().sendTx(
                transaction,
                selfData().nodeAddress(),
                selfData().nodeLogin(), selfData().nodePassword()
            );

            selfData().closeTx(transaction.getHash());

            trigger(
                CloseBuyerTxState.class,
                CloseBuyerTxState::onFinish
            );
        }

        @Event
        public void onFinish() throws Throwable {
            transit(new FinishState(this));
        }

        @Override
        public CloseBuyerTxState copy() {
            return new CloseBuyerTxState(this);
        }

    }



    private final class ErrorState extends AbstractTxState<TxStatus> {

        public ErrorState(final TxState<TxStatus> state) {
            super(state, TxStatus.Error);
        }

        @Override
        public ErrorState copy() {
            return new ErrorState(this);
        }

    }

    private final class FinishState extends AbstractTxState<TxStatus> {

        public FinishState(final TxState<TxStatus> state) {
            super(state, TxStatus.Finish);
        }

        @Override
        public FinishState copy() {
            return new FinishState(this);
        }

    }



    private static Socket createServerSocket(
        final int port, final int time
    ) throws IOException {
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(time);
            final Socket socket = serverSocket.accept();
            socket.setKeepAlive(true);
            socket.setSoTimeout(SocketMillisecTimeout);
            return socket;
        }
    }

    private static Socket createClientSocket(
        final InetSocketAddress address
    ) throws IOException {
        final Socket socket = new Socket();
        socket.connect(address, SocketMillisecTimeout);
        socket.setKeepAlive(true);
        socket.setSoTimeout(SocketMillisecTimeout);
        return socket;
    }



    private static final String WordsSeparator = "|";
    private static final int SocketMillisecTimeout = 15 * 1000; // 15 seconds
    private static final int WaitTxMillisecTimeout = 24 * 60 * 60 * 1000; // 1 day
    private static final int WaitTxConfirmedMillisecTimeout = 60 * 60 * 1000; // 1 hour
    private static final int SellerCSVFactor = 2;
    private static final Coin TxFee = Coin.valueOf(1000);

}
