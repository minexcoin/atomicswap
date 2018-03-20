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

package com.minexcoin.atomic_swap.fsm.states;

import com.minexcoin.atomic_swap.fsm.data.Data;

public abstract class AbstractTxState<StatusT extends Enum<StatusT>>
implements TxState<StatusT> {

    @Override
    public Data selfData() {
        return selfData_;
    }

    @Override
    public Data partnerData() {
        return partnerData_;
    }

    @Override
    public StatusT status() {
        return status_;
    }

    protected AbstractTxState(final TxState<StatusT> state, final StatusT status) {
        this(state.selfData(), state.partnerData(), status);
    }

    protected AbstractTxState(
        final Data selfData, final Data partnerData, final StatusT status
    ) {
        selfData_ = selfData.clone();
        partnerData_ = partnerData.clone();
        status_ = status;
    }

    private Data selfData_;
    private Data partnerData_;
    private final StatusT status_;

}