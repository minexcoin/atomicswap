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

package com.minexcoin.fsm;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

public abstract class GeneralFSM<StateT extends Copyable<StateT>> implements FSM<StateT> {

    @Override
    public Observable<StateT> states() {
        return states_.asObservable();
    }

    @Override
    public Status status() {
        return status_.get();
    }

    @Override
    public boolean start() {
        if (!status_.compareAndSet(Status.Ready, Status.Active))
            return false;

        startImpl();
        return true;
    }



    @FunctionalInterface
    protected static interface Processable<T> {
        void process(final T source) throws Throwable;
    }

    @FunctionalInterface
    protected static interface Executable {
        void execute() throws Throwable;
    }

    @Retention(SOURCE)
    @Target(METHOD)
    protected @interface Actor {}

    @Retention(SOURCE)
    @Target(METHOD)
    protected @interface Event {}



    protected static enum StreamOpts { Last, Once, Always }

    protected GeneralFSM(
        final StateT state, final StreamOpts opts,
        final long eventKeepAliveTime, final TimeUnit eventUnit,
        final long taskKeepAliveTime, final TimeUnit taskUnit,
        final int taskLimit,
        final String eventThreadName, final String taskThreadName
    ) {
        state_ = state;
        status_ =  new AtomicReference<Status>(Status.Ready);

        events_ = new ThreadPoolExecutor(
            1, 1,
            eventKeepAliveTime, eventUnit,
            new LinkedBlockingQueue<Runnable>(taskLimit),
            new ThreadFactoryBuilder().setNameFormat(eventThreadName).build()
        );
        executor_ = new ThreadPoolExecutor(
            0, Integer.MAX_VALUE,
            taskKeepAliveTime, taskUnit,
            new SynchronousQueue<Runnable>(),
            new ThreadFactoryBuilder().setNameFormat(taskThreadName).build()
        );

        subject_ = PublishSubject.create();
        states_ = (opts == StreamOpts.Always) ?
        ReplaySubject.create() : BehaviorSubject.create();

        (
            (opts == StreamOpts.Last) ?
            (Function<Observable<StateT>, Observable<StateT>>)
            Observable<StateT>::onBackpressureLatest :
            (Function<Observable<StateT>, Observable<StateT>>)
            Observable<StateT>::onBackpressureBuffer
        ).apply(subject_.asObservable()).
        subscribeOn(Schedulers.from(executor_)).
        subscribe(states_);

        eventThread_ = new AtomicReference<Thread>(null);
    }

    protected <StateU extends StateT> void trigger(
        final Class<StateU> to,
        final Processable<? super StateU> processor
    ) {
        trigger(() -> processor.process(cast(state(), to)));
    }

    protected void async(final Executable task) {
        executor().submit(makeActor(task));
    }

    protected void async(
        final Collection<Executable> tasks, final boolean all
    ) throws InterruptedException, ExecutionException {
        final List<Callable<?>> collection =
        tasks.stream().
        map(this::makeActor).
        collect(Collectors.toList());

        if (all) {
            executor().invokeAll(collection);
        } else {
            executor().invokeAny(collection);
        }
    }

    protected <StateU extends StateT> void transit(final StateU state) throws Throwable {
        transit(state, () -> {});
    }

    protected <StateU extends StateT> void transit(
        final StateU state,
        final Processable<? super StateU> processor
    ) throws Throwable {
        transit(
            state,
            () -> async(() -> processor.process(state))
        );
    }

    protected <StateU extends StateT> void transit(
        final StateU state,
        final List<Processable<? super StateU>> processors, final boolean all
    ) throws Throwable {
        transit(
            state,
            () -> {
                final List<Executable> collection =
                processors.stream().
                <Executable>map(processor -> {
                    return () -> processor.process(state);
                }).
                collect(Collectors.toList());

                async(collection, all);
            }
        );
    }

    protected abstract void startImpl();
    protected abstract boolean isFinal(final StateT state);
    protected abstract StateT errorState(final StateT state, final Throwable exception);



    private <StateU extends StateT> void trigger(final Executable executer) {
        try {
            events().submit(() -> {
                try {
                    if (isError()) {
                        throw new RejectedExecutionException("Event pool is full");
                    }

                    eventThread(Thread.currentThread());
                    executer.execute();
                } catch (final Throwable exception) {
                    state(errorState(state(), exception));
                    status(Status.Error);
                    subject().onNext(state());
                    subject().onError(exception);
                    events().shutdownNow();
                    executor().shutdownNow();
                } finally {
                    eventThread(null);
                }
            });
        } catch (final RejectedExecutionException exception) {
            status(Status.Error);
        }
    }

    private Callable<?> makeActor(final Executable executer) {
        return () -> {
            try {
                executer.execute();
            } catch (InterruptedException | CancellationException exception) {
                // Just ignore it
            } catch (final Throwable exception) {
                trigger(() -> {throw exception;});
            }
            return null;
        };
    }

    private <StateU extends StateT> void transit(
        final StateU state,
        final Executable executer
    ) throws Throwable {
        if (!eventThread().equals(Thread.currentThread())) {
            throw new IllegalMonitorStateException("Transition only able in event thread");
        }

        this.state(state);
        subject().onNext(state().copy());
        if (isFinal(state())) {
            status(Status.Done);
            subject().onCompleted();
            events().shutdownNow();
            executor().shutdownNow();
        } else {
            executer.execute();
        }
    }

    private StateT state() {
        return state_;
    }

    private void state(final StateT state) {
        state_ = state;
    }

    private void status(final Status status) {
        status_.set(status);
    }

    private ExecutorService events() {
        return events_;
    }

    private ExecutorService executor() {
        return executor_;
    }

    private Subject<StateT, StateT> subject() {
        return subject_;
    }

    private Thread eventThread() {
        return eventThread_.get();
    }

    private void eventThread(final Thread thread) {
        eventThread_.set(thread);
    }

    private StateT state_;
    private final AtomicReference<Status> status_;
    private final ExecutorService events_;
    private final ExecutorService executor_;
    private final Subject<StateT, StateT> subject_;
    private final Subject<StateT, StateT> states_;
    private final AtomicReference<Thread> eventThread_;

}