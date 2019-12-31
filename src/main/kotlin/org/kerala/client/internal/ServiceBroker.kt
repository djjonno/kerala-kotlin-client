/*
 * MIT License
 *
 * Copyright (c) 2019 Kerala
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
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
*/

package org.kerala.client.internal

import com.google.gson.GsonBuilder
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.kerala.client.*
import java.util.concurrent.TimeUnit

internal class ServiceBroker(
        private val brokerHost: String,
        private val brokerPort: Int
) {
    private val brokerChannel = ManagedChannelBuilder.forAddress(brokerHost, brokerPort).usePlaintext().build()
    private var clusterInfo: KClusterInfoResponse? = null

    /**
     * Bootstraps ServiceBroker with kerala cluster configuration.
     *
     * This will allow the ServiceBroker to provide ManagedChannels to appropriate nodes for
     * read and write operations.
     *
     * @throws KeralaClientException if kerala bootstrap node could not be reached.
     */
    @Throws(KeralaClientException::class)
    @Synchronized internal fun bootstrap() {
        try {
            val command = KeralaCommandRequest.newBuilder().setCommand("cluster").build()
            /* TODO: add configurable timeout to future#get */
            val json = KeralaClientServiceGrpc.newFutureStub(brokerChannel).keralaClientCommand(command).get(5, TimeUnit.SECONDS).response
            clusterInfo = GsonBuilder().create().fromJson<KClusterInfoResponse>(json, KClusterInfoResponse::class.java)
        } catch (e: Exception) {
            throw KeralaClientException("Failed to fetch ClusterInfo from `$brokerHost:$brokerPort`")
        }
    }

    fun readChannel(): ManagedChannel {
        if (clusterInfo == null) {
            bootstrap()
        }

        /* TODO: load-balancing strategy */
        return clusterInfo?.nodes?.random()?.toChannel() ?:
            throw KeralaClientException("Failed to obtain a `read` node from `$brokerHost:$brokerPort`")
    }

    fun writeChannel(): ManagedChannel {
        if (clusterInfo == null) {
            bootstrap()
        }

        return clusterInfo?.nodes?.find { it.leader }?.toChannel() ?:
            throw KeralaClientException("Failed to obtain a `write` node from `$brokerHost:$brokerPort`")
    }
}

fun KNodeResponse.toChannel(): ManagedChannel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
