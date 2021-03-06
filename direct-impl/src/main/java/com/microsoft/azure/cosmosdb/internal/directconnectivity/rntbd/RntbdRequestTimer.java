/*
 * The MIT License (MIT)
 * Copyright (c) 2018 Microsoft Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.microsoft.azure.cosmosdb.internal.directconnectivity.rntbd;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public final class RntbdRequestTimer implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(RntbdRequestTimer.class);
    private final long requestTimeoutInNanos;
    private final Timer timer;

    public RntbdRequestTimer(final long requestTimeoutInNanos, final long requestTimerResolutionInNanos) {
        // The HashWheelTimer code shows that cancellation of a timeout takes two timer resolution units to complete.
        this.timer = new HashedWheelTimer(requestTimerResolutionInNanos, TimeUnit.NANOSECONDS);
        this.requestTimeoutInNanos = requestTimeoutInNanos;
    }

    public long getRequestTimeout(final TimeUnit unit) {
        return unit.convert(requestTimeoutInNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void close() {
        final Set<Timeout> timeouts = this.timer.stop();
        if (logger.isDebugEnabled()) {
            final int count = timeouts.size();
            if (count > 0) {
                logger.debug("request expiration tasks cancelled: {}", count);
            }
        }
    }

    public Timeout newTimeout(final TimerTask task) {
        return this.timer.newTimeout(task, this.requestTimeoutInNanos, TimeUnit.NANOSECONDS);
    }
}
