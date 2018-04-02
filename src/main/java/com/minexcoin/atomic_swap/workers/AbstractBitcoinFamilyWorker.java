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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;

import static org.bitcoinj.script.ScriptOpCodes.*;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.Utils;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.AsyncOnSubscribe;
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.In;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient.RawTransaction.Out;

abstract class AbstractBitcoinFamilyWorker implements Worker {

    @Override
    public TransactionOutPoint sendToAddress(
        final Address address, final Coin amount,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException {
        final Sha256Hash hash = Sha256Hash.wrap(
            getRpcClient(inetAddress, login, password).
            sendToAddress(address.toString(), (double)amount.value / 100000000)
        );

        final OptionalInt index = auditTx(
            hash, address,
            amount, 0,
            inetAddress, login, password
        );

        return new TransactionOutPoint(address.getParameters(), index.getAsInt(), hash);
    }

    @Override
    public void sendTx(
        final Transaction transaction,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException {
        getRpcClient(inetAddress, login, password).
        sendRawTransaction(Utils.HEX.encode(transaction.unsafeBitcoinSerialize()));
    }

    @Override
    public RunnableFuture<Optional<Unit>> waitTxMature(
        final int listeners,
        final Sha256Hash txHash, final int confirmations,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) throws MalformedURLException {
        if (confirmations < 0) {
            throw new IllegalArgumentException(
                "Confirmations is negative value " + confirmations
            );
        }

        final BitcoindRpcClient rpc = getRpcClient(inetAddress, login, password);

        return new WorkerFutureTask<Unit>(
            Observable.merge(
                Observable.just(Unit.unit()).subscribeOn(Schedulers.io()),
                observeBlocks(
                    listeners, inetAddress, notificationPort, timeout, timeUnit
                ).
                map(value -> Unit.unit()).subscribeOn(Schedulers.io())
            ).
            takeFirst(unit -> {
                try {
                    final RawTransaction tx = rpc.getRawTransaction(txHash.toString());
                    if (confirmations == 0) {
                        return true;
                    }

                    return tx.confirmations() >= confirmations;
                } catch (final Throwable exception) {
                    return false;
                }
            }).publish()
        );
    }

    @Override
    public RunnableFuture<Optional<TransactionOutPoint>> waitPartnerTx(
        final int listeners,
        final Address address, final Coin amount,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    )  throws MalformedURLException {
        return waitPartnerTx(
            observeTxs(
                listeners, inetAddress, notificationPort, timeout, timeUnit
            ),
            address, amount, inetAddress, login, password
        );
    }

    @Override
    public RunnableFuture<Optional<TransactionOutPoint>> waitPartnerTx(
        final int listeners, final Sha256Hash txHash,
        final Address address, final Coin amount,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    )  throws MalformedURLException {
    	
        return waitPartnerTx(
            Observable.merge(
                Observable.just(txHash).subscribeOn(Schedulers.io()),
                observeTxs(
                    listeners, inetAddress, notificationPort, timeout, timeUnit
                ).subscribeOn(Schedulers.io())
            ),
            address, amount, inetAddress, login, password
        );
    }

    @Override
    public RunnableFuture<Optional<Sha256Hash>> waitTxSecret(
        final int listeners,
        final TransactionOutPoint txOutPoint,
        final InetSocketAddress inetAddress,
        final String login, final String password,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) throws MalformedURLException {
        return new WorkerFutureTask<Sha256Hash>(
            observeTxs(
                listeners, inetAddress, notificationPort, timeout, timeUnit
            ).
            map(txHash -> {
                try {
                    return extractSecret(
                        txHash, txOutPoint, inetAddress, login, password
                    );
                } catch (final MalformedURLException exception) {
                    throw new RuntimeException(exception);
                }
            }).
            takeFirst(Optional::isPresent).
            map(Optional::get).publish()
        );
    }

    @Override
    public OptionalInt auditTx(
        final Sha256Hash txHash, final Address address,
        final Coin amount, final int confirmations,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) {

        final RawTransaction tx;
        try {
            tx = getRawTx(txHash, inetAddress, login, password);
        } catch (final Throwable exception) {
            return OptionalInt.empty();
        }

        if (confirmations > 0 && tx.confirmations() < confirmations) {
            return OptionalInt.empty();
        }

        return IntStream.range(0, tx.vOut().size()).
        filter(index -> {
            final Out out = tx.vOut().get(index);
            return out.value() >= ((double)amount.value / 100000000) &&
            out.scriptPubKey().addresses().size() != 0 &&
            out.scriptPubKey().addresses().get(0).equals(address.toBase58());
        }).findFirst();
    }

    @Override
    public Optional<Sha256Hash> extractSecret(
        final Sha256Hash txHash,
        final TransactionOutPoint txOutPoint,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException  {
        final RawTransaction tx = getRawTx(txHash, inetAddress, login, password);

        final OptionalInt position = IntStream.range(0, tx.vIn().size()).
        filter(index -> {
            final In in = tx.vIn().get(index);
            try {
            	return in.txid().equals(txOutPoint.getHash().toString()) &&
                        in.vout() == txOutPoint.getIndex();
            } catch (Throwable e) {
            	return false;
            }
        }).findFirst();


        if (!position.isPresent()) {
            return Optional.empty();
        }

        final ScriptChunk chunk = new Script(
            Utils.HEX.decode(
                (String)tx.vIn().get(position.getAsInt()).scriptSig().get("hex")
            )
        ).getChunks().get(2);

        return chunk.opcode == 32 ?
        Optional.of(Sha256Hash.wrap(chunk.data)) :
        Optional.empty();
    }

	@Override
    public Script createFundingScript(final ECKey keyForHash, final ECKey keyForCSV,
        final Sha256Hash secretHash, final int csv) {

        return new ScriptBuilder().

        op(OP_IF).
            op(OP_SHA256).
            data(secretHash.getBytes()).
            op(OP_EQUALVERIFY).
            op(OP_DUP).
            op(OP_HASH160).
            data(keyForHash.getPubKeyHash()).
        op(OP_ELSE).
            number(csv).
            op(OP_NOP3). // OP_CHECKSEQUENCEVERIFY BIP68, BIP112, BIP113
            op(OP_DROP).
            op(OP_DUP).
            op(OP_HASH160).
            data(keyForCSV.getPubKeyHash()).
        op(OP_ENDIF).
        op(OP_EQUALVERIFY).
        op(OP_CHECKSIG).

        build();
	}

	@Override
    public Address createP2SHAddress(final Script script, final NetworkParameters params) {
        return ScriptBuilder.createP2SHOutputScript(script).getToAddress(params);
    }

	@Override
	public Transaction createSpendingTx(
        final TransactionOutPoint txOutPoint,
	    final ECKey recipient, final Coin amount, final NetworkParameters params,
	    final ECKey key, final Script script, final Sha256Hash secret) {
        final Transaction transaction = createTx(txOutPoint, recipient, amount, params);

        transaction.getInput(0).setScriptSig(
            new ScriptBuilder().
            data(
                transaction.calculateSignature(
                    0, key, script, Transaction.SigHash.ALL, false
                ).encodeToBitcoin()
            ).
            data(key.getPubKey()).
            data(secret.getBytes()).
            op(ScriptOpCodes.OP_TRUE).
            data(script.getProgram()).
            build()
        );

        return transaction;
	}

    @Override
    public Transaction createRefundingTx(
        final TransactionOutPoint txOutPoint,
        final ECKey recipient, final Coin amount, final NetworkParameters params,
        final ECKey key, final Script script, final int csv) {
        final Transaction transaction = createTx(txOutPoint, recipient, amount, params);

        transaction.getInput(0).setSequenceNumber(csv);
        transaction.getInput(0).setScriptSig(
            new ScriptBuilder().
            data(
                transaction.calculateSignature(
                    0, key, script, Transaction.SigHash.ALL, false
                ).encodeToBitcoin()
            ).
            data(key.getPubKey()).
            addChunk(new ScriptChunk(ScriptOpCodes.OP_FALSE, null)).
            data(script.getProgram()).
            build()
        );

        return transaction;
    }



    private static Observable<Sha256Hash> observeBlocks(
        final int listeners,
        final InetSocketAddress inetAddress,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) {
        return observe(
            "hashblock", listeners, inetAddress, notificationPort, timeout, timeUnit
        );
    }

    private static Observable<Sha256Hash> observeTxs(
        final int listeners,
        final InetSocketAddress inetAddress,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) {
        return observe(
            "hashtx", listeners, inetAddress, notificationPort, timeout, timeUnit
        );
    }

    private RunnableFuture<Optional<TransactionOutPoint>> waitPartnerTx(
        final Observable<Sha256Hash> txs,
        final Address address, final Coin amount,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) {
        return new WorkerFutureTask<TransactionOutPoint>(
            txs.map(txHash -> {
                try {
                    return new TransactionOutPoint(
                        address.getParameters(),
                        auditTx(
                            txHash, address, amount, 0, inetAddress, login, password
                        ).orElse(-1),
                        txHash
                    );
                } catch (final Throwable exception) {
                    throw new RuntimeException(exception);
                }
            }).
            takeFirst(txOutPoint -> txOutPoint.getIndex() != -1).
            publish()
        );
    }

    private static BitcoindRpcClient getRpcClient(
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException {
        return new BitcoinJSONRPCClient(
            "http://" + login + ":" + password + "@" +
            inetAddress.getHostName() + ":" + inetAddress.getPort()
        );
    }

    private static RawTransaction getRawTx(
        final Sha256Hash txHash,
        final InetSocketAddress inetAddress,
        final String login, final String password
    ) throws MalformedURLException {
        return getRpcClient(inetAddress, login, password).
        getRawTransaction(txHash.toString());
    }

    private static Transaction createTx(
        final TransactionOutPoint txOutPoint,
        final ECKey recipient, final Coin amount, final NetworkParameters params) {
        final Transaction transaction = new Transaction(params);
        transaction.setVersion(2);

        transaction.addOutput(
            amount, new Address(params, recipient.getPubKeyHash())
        );

        transaction.addInput(new TransactionInput(
            params, transaction, new byte[]{}, txOutPoint
        ));

        return transaction;
    }

    private static Observable<Sha256Hash> observe(
        final String type,
        final int listeners,
        final InetSocketAddress inetAddress,
        final int notificationPort,
        final long timeout, final TimeUnit timeUnit
    ) {
        return Observable.create(new AsyncOnSubscribe<Void, Sha256Hash>() {

            {
                if (timeout < 0) {
                    throw new IllegalArgumentException("Negative timeout: " + timeout);
                }

                this.context_ = ZMQ.context(listeners);
                try {
                    this.socket_ = this.context_.socket(ZMQ.SUB);
                    try {
                        this.socket_.connect(
                            "tcp://" + inetAddress.getHostName() + ":" + notificationPort
                        );
                        this.socket_.subscribe(type.getBytes());
                    } catch (final Throwable exception) {
                        this.socket_.close();
                        throw exception;
                    }
                } catch (final Throwable exception) {
                    this.context_.close();
                    throw exception;
                }
            }

            @Override
            protected Void generateState() {
                currentTime(System.currentTimeMillis());
                endTime(currentTime() + timeUnit.toMillis(timeout));
                return null;
            }

            @Override
            protected Void next(
                final Void state, final long requested,
                final Observer<Observable<? extends Sha256Hash>> observer
            ) {
                try {
                    socket().setReceiveTimeOut(timeout == 0 ? -1 : (int)expiredTime());
                    final ZMsg msg = ZMsg.recvMsg(socket());
                    currentTime(System.currentTimeMillis());

                    if (Thread.currentThread().isInterrupted()) {
                        observer.onError(new InterruptedException());
                    } else if (msg == null) {
                        observer.onCompleted();
                    } else {
                        msg.pollFirst().getData();
                        observer.onNext(
                            Observable.just(
                                Sha256Hash.wrap(msg.pollFirst().getData())
                            )
                        );

                        if (timeout != 0 && expiredTime() < 0) {
                            observer.onCompleted();
                        }
                    }
                } catch (final Throwable exception) {
                    if (Thread.currentThread().isInterrupted()) {
                        observer.onError(new InterruptedException());
                    } else if (
                        exception instanceof ZMQException &&
                        ((ZMQException) exception).getErrorCode() == ZMQ.Error.ETERM.getCode()
                    ) {
                        observer.onCompleted();
                    } else {
                        observer.onError(exception);
                    }
                }

                return null;
            }

            @Override
            protected void onUnsubscribe(final Void state) {
                socket().close();
                context().close();
            }

            private ZMQ.Context context() {
                return context_;
            }

            private ZMQ.Socket socket() {
                return socket_;
            }

            private long currentTime() {
                return currentTime_;
            }

            private void currentTime(final long time) {
                currentTime_ = time;
            }

            private long endTime() {
                return endTime_;
            }

            private void endTime(final long time) {
                endTime_ = time;
            }

            private long expiredTime() {
                return endTime() - currentTime();
            }

            private final ZMQ.Context context_;
            private final ZMQ.Socket socket_;
            long currentTime_;
            long endTime_;

        });
    }

    private static class WorkerFutureTask<T> implements RunnableFuture<Optional<T>> {

        public WorkerFutureTask(final ConnectableObservable<T> publisher) {
            subscription_ = new AtomicReference<>(null);

            future_ = new FutureTask<>(() -> {
                final CompletableFuture<Optional<T>> subscriber = new CompletableFuture<>();
                subscription(publisher.subscribe(
                      value -> subscriber.complete(Optional.of(value)),
                      subscriber::completeExceptionally,
                      () -> subscriber.complete(Optional.empty())
                ));

                publisher.connect();

                try {
                    return subscriber.get();
                } catch (final ExecutionException exception) {
                    throw (Exception)exception.getCause();
                }
            });
        }

        @Override
        public void run() {
            future().run();
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            final Subscription subscription = subscription();
            if(subscription != null) {
                subscription.unsubscribe();
            }

            subscription(null);
            return future().cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future().isCancelled();
        }

        @Override
        public boolean isDone() {
            return future().isDone();
        }

        @Override
        public Optional<T> get() throws InterruptedException, ExecutionException {
            try {
                return future().get();
            } catch (final ExecutionException exception) {
                if (exception.getCause() instanceof InterruptedException) {
                    throw (InterruptedException)exception.getCause();
                }

                throw exception;
            }
        }

        @Override
        public Optional<T> get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            try {
                return future().get(timeout, unit);
            } catch (final ExecutionException exception) {
                if (exception.getCause() instanceof InterruptedException) {
                    throw (InterruptedException)exception.getCause();
                }

                if (exception.getCause() instanceof TimeoutException) {
                    throw (TimeoutException)exception.getCause();
                }

                throw exception;
            }
        }



        private RunnableFuture<Optional<T>> future() {
            return future_;
        }

        private Subscription subscription() {
            return subscription_.get();
        }

        private void subscription(final Subscription subscription) {
            subscription_.set(subscription);
        }

        private final RunnableFuture<Optional<T>> future_;
        private final AtomicReference<Subscription> subscription_;

    }

}