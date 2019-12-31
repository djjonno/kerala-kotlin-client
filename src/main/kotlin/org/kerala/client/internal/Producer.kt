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

import com.google.protobuf.ByteString
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kerala.client.*
import org.kerala.client.common.serialization.Produce
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

internal class Producer<K, V> internal constructor(
        client: KeralaClient,
        private val produced: Produce<K, V>
) : KeralaProducer<K, V> {

    /* inbound stream to receive server responses */
    private val responseStream = object : StreamObserver<KeralaProducerResponse> {
        val receiverChannel = Channel<KeralaProducerResponse>()

        override fun onNext(value: KeralaProducerResponse) {
            receiverChannel.sendBlocking(value)
        }

        override fun onError(t: Throwable) {
            receiverChannel.close(t)
        }

        override fun onCompleted() {
            receiverChannel.close(KeralaClientException("Server closed the stream"))
        }
    }

    /* outbound stream to dispatch requests to server */
    private val requestStream = client.serviceInvoker.produceTopic(responseStream)

    override fun send(topic: String, kv: KKV<K, V>, block: (KProducerResponse) -> Unit) = send(topic, listOf(kv), block)

    override fun send(topic: String, kv: KKV<K, V>): Future<KProducerResponse> = send(topic, listOf(kv))

    override fun send(topic: String, kvs: List<KKV<K, V>>, block: (KProducerResponse) -> Unit) {
        requestStream.onNext(createRequest(topic, kvs))

        try {
            val response = runBlocking {
                withTimeout(1_000) {
                    responseStream.receiverChannel.receive()
                }
            }

            when (response.responseCode) {
                KClientProducerACKCodes.OK.id -> block(KProducerResponse(response.responseCode))
                else -> throw KeralaClientException("Server error `${KClientProducerACKCodes.fromId(response.responseCode)?.name}`")
            }
        } catch (e: StatusRuntimeException) {
            throw KeralaClientException("Network error")
        }
    }

    override fun send(topic: String, kvs: List<KKV<K, V>>): Future<KProducerResponse> {
        return CompletableFuture<KProducerResponse>().apply {
            send(topic, kvs) {
                complete(it)
            }
        }
    }

    override fun close() {
        requestStream.onCompleted()
    }

    private fun createRequest(topic: String, kvs: List<KKV<K, V>>): KeralaProducerRequest {
        return KeralaProducerRequest.newBuilder()
            .setTopic(topic)
            .addAllKvs(kvs.map {
                val key = ByteString.copyFrom(produced.keySerde.serializer.serialize(it.key))
                val value = ByteString.copyFrom(produced.valueSerde.serializer.serialize(it.value))
                KeralaKV.newBuilder().setKey(key).setValue(value).setTimestamp(it.timestamp).build()
            })
            .build()
    }
}
