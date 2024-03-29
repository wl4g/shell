/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.shell.core.locks;

import static com.wl4g.infra.common.lang.Assert2.hasTextOf;
import static com.wl4g.infra.common.lang.Assert2.notNullOf;
import static com.wl4g.infra.common.lang.SystemUtils2.GLOBAL_PROCESS_SERIAL;
import static com.wl4g.infra.common.log.SmartLoggerFactory.getLogger;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static java.util.Objects.isNull;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.google.common.annotations.Beta;
import com.wl4g.infra.common.log.SmartLogger;
import com.wl4g.shell.core.cache.ShellCache;

/**
 * {@link ShellLockManager}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-07-01 v1.0.0
 *  {@link com.wl4g.infra.support.cache.locks.JedisLockManager}
 */
public class ShellLockManager {
    protected final SmartLogger log = getLogger(getClass());
    protected static final String NAMESPACE = "reentrantUnfairLock.";
    protected static final String NXXX = "NX";
    protected static final String EXPX = "PX";
    protected static final long FRAME_INTERVAL_MS = 50L;

    protected final ShellCache shellCache;

    public ShellLockManager(ShellCache shellCache) {
        this.shellCache = notNullOf(shellCache, "shellCache");
    }

    /**
     * Get and create {@link FastReentrantUnfairDistributedRedLock} with name.
     * 
     * @param name
     * @return
     */
    public Lock getLock(String name) {
        return getLock(name, 10, TimeUnit.SECONDS);
    }

    /**
     * Get and create {@link FastReentrantUnfairDistributedRedLock} with name.
     * 
     * @param name
     * @param expiredAt
     * @param unit
     * @return
     */
    public Lock getLock(String name, long expiredAt, TimeUnit unit) {
        hasText(name, "Lock name must not be empty.");
        isTrue(expiredAt > 0, "Lock expiredAt must greater than 0");
        notNull(unit, "TimeUnit must not be null.");
        return new FastReentrantUnfairDistributedRedLock(name, unit.toMillis(expiredAt));
    }

    /**
     * Get current thread unique process ID. 
     * 
     * <pre>
     * Host serial + local processId + threadId
     * </pre>
     * 
     * @return
     */
    public final static String getThreadCurrentProcessId() {
        return GLOBAL_PROCESS_SERIAL + "-" + Thread.currentThread().getId();
    }

    /**
     * Fast unsafe reentrant unfair redlock implemented by REDIS cluster.
     * 
     * <font color=red style="text-decoration:underline;">Note: This
     * implementation is not strictly strong consistency. It is recommended to
     * use this distributed lock for scenarios with high performance
     * requirements and low consistency requirements. On the contrary, for
     * scenarios with high consistency requirements (such as orders, payments,
     * etc.), please do not use this lock. You can use the lock of Raft/Paxos
     * strong consistency algorithm such as zookeeper distributed lock. Because
     * redis cluster distributed locks, for example, when the master dies before
     * copying to the slave or when the master and slave die together, there
     * will be serious consequences of locking.</font>
     * 
     * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
     * @version v1.0 2019-3月21日
     * @since v1.0
     * @see <a href=
     *      'https://blog.csdn.net/matt8/article/details/64442064'>Discuss
     *      Antirez failover analysis</a>
     * @see <a href=
     *      'https://martin.kleppmann.com/2016/02/08/how-to-do-distributed-locking.html'>Martin
     *      Kleppmann analysis redlock</a>
     * @see <a href='https://redis.io/topics/distlock'>Antirez failover
     *      analysis</a>
     * @see <a href='http://antirez.com/news/101'>VS Martin Kleppmann for
     *      Redlock failover analysis</a>
     */
    @Beta
    private final class FastReentrantUnfairDistributedRedLock implements Lock {
        /** Current locker name. */
        protected final String name;
        /** Current locker request ID. */
        protected final String requestId;
        /** Current locker expired time(MS). */
        protected final long expiredMs;
        /**
         * Current locker reentrant counter.
         * <font color=red>Special Note: assuming that the situation of retry to
         * obtain lock occurs, it must be in the same JVM process.</font>
         */
        protected final AtomicLong counter;

        public FastReentrantUnfairDistributedRedLock(String name, long expiredMs) {
            this(name, expiredMs, 0L);
        }

        public FastReentrantUnfairDistributedRedLock(String name, long expiredMs, long counterValue) {
            this(name, expiredMs, new AtomicLong(counterValue));
        }

        public FastReentrantUnfairDistributedRedLock(String name, long expiredMs, AtomicLong counter) {
            this((NAMESPACE.concat(name)), getThreadCurrentProcessId(), expiredMs, counter);
        }

        public FastReentrantUnfairDistributedRedLock(String name, String currentProcessId, long expiredMs, AtomicLong counter) {
            this.name = hasTextOf(name, "lockName");
            this.requestId = hasTextOf(currentProcessId, "currentProcessId");
            isTrue(expiredMs > 0, "Lock expiredMs must greater than 0");
            this.expiredMs = expiredMs;
            this.counter = counter;
            isTrue(counter.get() >= 0, "Lock count must greater than 0");
        }

        @Override
        public void lock() {
            try {
                lockInterruptibly();
            } catch (InterruptedException e) {
                currentThread().interrupt();
            }
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            if (interrupted())
                throw new InterruptedException();
            while (true) {
                if (doTryAcquire())
                    break;
                sleep(FRAME_INTERVAL_MS);
            }
        }

        @Override
        public boolean tryLock() {
            return doTryAcquire();
        }

        @Override
        public boolean tryLock(long tryTimeout, TimeUnit unit) throws InterruptedException {
            notNull(unit, "TimeUnit must not be null.");
            isTrue((tryTimeout > 0 && tryTimeout <= expiredMs), "TryTimeout must be > 0 && <= " + expiredMs);
            long t = unit.toMillis(tryTimeout) / FRAME_INTERVAL_MS, c = 0;
            while (t > ++c) {
                if (doTryAcquire())
                    return true;
                sleep(FRAME_INTERVAL_MS);
            }
            return false;
        }

        @Override
        public void unlock() {
            // Obtain locked processId.
            String acquiredRequestId = shellCache.get(name, String.class);
            // Current thread is holder?
            if (!requestId.equals(acquiredRequestId)) {
                log.debug("No need to unlock of requestId: {}, acquiredRequestId: {}, counter: {}", requestId, acquiredRequestId,
                        counter);
                return;
            }

            // Obtain lock record once decrement.
            counter.decrementAndGet();
            log.debug("No need to unlock and reenter the stack lock layer, counter: {}", counter);

            if (counter.longValue() == 0L) { // All thread stack layers exited?
                Object res = shellCache.deleq(name, requestId);
                if (!assertValidity(res)) {
                    log.debug("Failed to unlock for %{}@{}", requestId, name);
                } else {
                    log.debug("Unlock successful for %{}@{}", requestId, name);
                }
            }
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        /**
         * Execution try acquire locker by reentrant info.
         * 
         * @see ShellLockManager.java
         * @return
         */
        private final boolean doTryAcquire() {
            String acquiredProcessId = shellCache.get(name, String.class); // Locked-processId.
            if (requestId.equals(acquiredProcessId)) {
                // Obtain lock record once cumulatively.
                counter.incrementAndGet();
                log.debug("Reuse acquire lock for name: {}, acquiredProcessId: {}, counter: {}", name, acquiredProcessId,
                        counter);
                return true;
            } else {
                // Not currently locked? Lock expired? Local counter reset.
                counter.set(0L);
            }

            // Try to acquire a new lock from the server.
            if (assertValidity(shellCache.setnx(name, requestId, expiredMs))) {
                // Obtain lock record once cumulatively.
                counter.incrementAndGet();
                return true;
            }
            return false;
        }

        /**
         * Assertion validate lock result is acquired/UnAcquired success?
         * 
         * @param res
         * @return
         */
        private final boolean assertValidity(Object res) {
            if (isNull(res)) {
                return false;
            }
            if (res instanceof String) {
                String res0 = res.toString().trim();
                return "1".equals(res0) || "OK".equalsIgnoreCase(res0);
            } else if (res instanceof Boolean) {
                return (boolean) res;
            } else if (res instanceof Number) {
                return ((Number) res).longValue() >= 1L;
            } else {
                throw new IllegalStateException(format("Unknown acquired state for %s", res));
            }
        }

    }

}