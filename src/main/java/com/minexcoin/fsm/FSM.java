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

import rx.Observable;

public interface FSM<T> {

    enum Status { Ready, Active, Error, Done }

    static class StateCastException extends RuntimeException {

        private static final long serialVersionUID = -4330411058722011374L;

        StateCastException(final ClassCastException exception) {
            super(exception);
        }

        StateCastException(final String message, final ClassCastException exception) {
            super(message, exception);
        }

    }

    default <U extends T> U cast(final T object, final Class<U> to) {
        try {
            return to.cast(object);
        } catch (final ClassCastException exception) {
            throw new StateCastException(
                "Bad cast from " + object.getClass().getCanonicalName() +
                " to " + to.getCanonicalName(), exception
            );
        }
    }

    Observable<T> states();

    Status status();

    boolean start();

    default boolean isReady() {
        return status() == Status.Ready;
    }

    default boolean isActive() {
        return status() == Status.Active;
    }

    default boolean isError() {
        return status() == Status.Error;
    }

    default boolean isDone() {
        return status() == Status.Done;
    }

    default boolean isFinish() {
        return isError() || isDone();
    }

}