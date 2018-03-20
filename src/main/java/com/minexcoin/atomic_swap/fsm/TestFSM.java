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

package com.minexcoin.atomic_swap.fsm;

import com.minexcoin.fsm.FSM;
import com.minexcoin.fsm.GeneralFSM;

import java.util.concurrent.TimeUnit;

import com.minexcoin.atomic_swap.fsm.data.Data;
import com.minexcoin.atomic_swap.fsm.data.SimpleData;
import com.minexcoin.atomic_swap.fsm.states.TxStatus;
import com.minexcoin.atomic_swap.fsm.states.AbstractTxState;
import com.minexcoin.atomic_swap.fsm.states.TxState;

public final class TestFSM extends GeneralFSM<TxState<TxStatus>> {

    public static void main(String[] args) {
        final FSM<TxState<TxStatus>> fsm = TestFSM.create(
            true,
            5, TimeUnit.SECONDS,
            5, TimeUnit.SECONDS,
            1000, "TestFSM Event", "TestFSM-Task-%d"
        );

        fsm.states().
        subscribe(
            state -> System.out.println(state.getClass().getCanonicalName()),
            Throwable::printStackTrace
        );

        fsm.start();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("_____________________");

        fsm.states().
        map(state -> state.getClass().getCanonicalName()).
        subscribe(System.out::println);
    }

    public static FSM<TxState<TxStatus>> create(
        final boolean cache,
        final long eventKeepAliveTime, final TimeUnit eventUnit,
        final long taskKeepAliveTime, final TimeUnit taskUnit,
        final int taskLimit,
        final String eventThreadName, final String taskThreadName
    ) {
        return new TestFSM(
            cache,
            eventKeepAliveTime, eventUnit,
            taskKeepAliveTime, taskUnit,
            taskLimit, eventThreadName, taskThreadName
        );
    }

    private TestFSM(
        final boolean cache,
        final long eventKeepAliveTime, final TimeUnit eventUnit,
        final long taskKeepAliveTime, final TimeUnit taskUnit,
        final int taskLimit,
        final String eventThreadName, final String taskThreadName
    ) {
        super(
            new State0(new SimpleData(), new SimpleData()),
            cache ? StreamOpts.Always : StreamOpts.Once,
            eventKeepAliveTime, eventUnit,
            taskKeepAliveTime, taskUnit,
            taskLimit, eventThreadName, taskThreadName
        );
    }



    @Actor
    @Override
    protected void startImpl() {
        trigger(State0.class, state -> state.fun0(this));
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



    private final static class State0 extends AbstractTxState<TxStatus> {

        public State0(final Data selfData, final Data partnerData) {
            super(selfData, partnerData, TxStatus.Test);
        }

        @Event
        public void fun0(final TestFSM fsm) throws Throwable {
            fsm.transit(fsm.new State1(this), State1::fun1);
        }

        @Override
        public State0 copy() {
            return new State0(selfData(), partnerData());
        }

    }



    private final class State1 extends AbstractTxState<TxStatus> {

        public State1(final TxState<TxStatus> state) {
            super(state, TxStatus.Test);
        }

        @Actor
        public void fun1() {
            System.out.println("fun1");
            trigger(
                State1.class,
                state -> transit(new State2(this), State2::execute)
            );
        }

        @Override
        public State1 copy() {
            return new State1(this);
        }

    }



    private final class State2 extends AbstractTxState<TxStatus> {

        public State2(final TxState<TxStatus> state) {
            super(state, TxStatus.Test);
        }

        @Actor
        public void execute() {
            System.out.println("hard async work 2 srart");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("hard async work 2 end");

            trigger(State2.class, State2::fun2);
        }

        @Event
        public void fun2() throws Throwable {
            System.out.println("fun2");
            transit(new State3(this), State3::execute);
        }

        @Override
        public State2 copy() {
            return new State2(this);
        }

    }



    private final class State3 extends AbstractTxState<TxStatus> {

        public State3(final TxState<TxStatus> state) {
            super(state, TxStatus.Test);
        }

        @Actor
        public void execute() {
            System.out.println("hard work 3 srart");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("hard work 3 end");

            trigger(State3.class, State3::fun3);
        }

        @Event
        public void fun3() throws Throwable {
            System.out.println("fun3");
            transit(new FinishState(this));
        }

        @Override
        public State3 copy() {
            return new State3(this);
        }

    }



    private final class ErrorState extends AbstractTxState<TxStatus> {

        public ErrorState(final TxState<TxStatus> state) {
            super(state, TxStatus.Test);
        }

        @Override
        public ErrorState copy() {
            return new ErrorState(this);
        }

    }

    private final class FinishState extends AbstractTxState<TxStatus> {

        public FinishState(final TxState<TxStatus> state) {
            super(state, TxStatus.Test);
        }

        @Override
        public FinishState copy() {
            return new FinishState(this);
        }

    }

}