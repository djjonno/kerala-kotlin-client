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

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kerala.client.*
import org.kerala.client.common.serialization.Consume

internal class Consumer<K, V>(
    client: KeralaClient,
    private val consume: Consume<K, V>,
    override val topic: String,
    private var offset: Long = 1
) : KeralaConsumer<K, V> {

    /* inbound stream to receive server responses */
    private val responseStream = object : StreamObserver<KeralaConsumerResponse> {
        val receiverChannel = Channel<KeralaConsumerResponse>()

        override fun onNext(value: KeralaConsumerResponse) {
            receiverChannel.sendBlocking(value)
        }

        override fun onError(t: Throwable?) {
            receiverChannel.close(t)
        }

        override fun onCompleted() {
            receiverChannel.close(KeralaClientException("Server closed the stream"))
        }
    }

    /* outbound stream to dispatch requests to server */
    private val requestStream = client.serviceInvoker.consumeTopic(responseStream)

    override fun poll(timeout: Long): KConsumerResponse<K, V> = runBlocking {
        requestStream.onNext(createRequest())

        val response = withTimeout(timeout) {
            responseStream.receiverChannel.receive()
        }

        when (response.responseCode) {
            KClientConsumerACKCodes.OK.id -> offset = response.offset + 1
        }

        KConsumerResponse(
            topic = response.topic,
            offset = response.offset,
            status = response.responseCode,
            kvs = deserializeKVs(response.kvsList)
        )
    }

    private fun deserializeKVs(kvs: List<KeralaKV>): List<KKV<K, V>> = kvs.map {
        val key = consume.keySerde.deserializer.deserialize(it.key.toByteArray())
        val value = consume.valueSerde.deserializer.deserialize(it.value.toByteArray())
        KKV(key, value, it.timestamp)
    }

    override fun close() {
        responseStream.onCompleted()
    }

    private fun createRequest(): KeralaConsumerRequest {
        return KeralaConsumerRequest.newBuilder()
            .setTopic(topic)
            .setOffset(offset)
            .build()
    }
}
