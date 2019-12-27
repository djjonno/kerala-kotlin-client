package org.kerala.client.internal

import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kerala.client.*
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

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

internal class Producer<K, V> internal constructor(
        client: KeralaClient,
        private val produced: Produced<K, V>
) : Closeable, KeralaProducer<K, V> {

    operator fun invoke(block: Producer<K, V>.() -> Unit) = block()

    /* inbound stream to receive server responses */
    private val responseStream = object : StreamObserver<RpcProducerResponse> {
        val receiverChannel = Channel<RpcProducerResponse>()

        override fun onNext(value: RpcProducerResponse) {
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

    override fun send(topic: String, record: KeralaKV<String, String>, block: (KeralaProducerResponse) -> Unit) = send(topic, listOf(record), block)
    override fun send(topic: String, record: KeralaKV<String, String>): Future<KeralaProducerResponse> = send(topic, record)

    override fun send(topic: String, records: List<KeralaKV<String, String>>, block: (KeralaProducerResponse) -> Unit) {
        requestStream.onNext(createRequest(topic, records))

        try {
            val response = runBlocking {
                withTimeout(1_000) {
                    responseStream.receiverChannel.receive()
                }
            }

            when (response.status) {
                ClientProducerACKCodes.OK.id -> block(KeralaProducerResponse(response.status))
                else -> throw KeralaClientException("Server error `${ClientProducerACKCodes.fromId(response.status)?.name}`")
            }
        } catch (e: StatusRuntimeException) {
            throw KeralaClientException("Network error")
        }

    }

    override fun send(topic: String, records: List<KeralaKV<String, String>>): Future<KeralaProducerResponse> {
        return CompletableFuture<KeralaProducerResponse>().apply {
            send(topic, records) {
                complete(it)
            }
        }
    }

    override fun close() {
        requestStream.onCompleted()
    }

    private fun createRequest(topic: String, records: List<KeralaKV<String, String>>) = RpcProducerRequest.newBuilder()
        .setTopic(topic)
        .addAllKvs(records.map {
            RpcKV.newBuilder().setKey(it.key).setValue(it.value).setTimestamp(it.timestamp).build()
        })
        .build()
}
