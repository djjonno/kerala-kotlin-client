package org.kerala.client.internal

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kerala.client.*
import java.io.Closeable

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

internal class Consumer<K, V>(
    client: KeralaClient,
    private val consumed: Consumed<K, V>,
    val topic: String,
    private var offset: Long = 1
) : Closeable, KeralaConsumer<K, V> {

    private val responseStream = object : StreamObserver<RpcConsumerResponse> {
        val receiverChannel = Channel<RpcConsumerResponse>()

        override fun onNext(value: RpcConsumerResponse) {
            receiverChannel.sendBlocking(value)
        }

        override fun onError(t: Throwable?) {
            receiverChannel.close(t)
        }

        override fun onCompleted() {
            receiverChannel.close(KeralaClientException("Server closed the stream"))
        }
    }

    private val requestStream = client.serviceInvoker.consumeTopic(responseStream)

    operator fun invoke(block: Consumer<K, V>.() -> Unit) = block()

    override fun poll(timeout: Long): KeralaConsumerResponse<String, String> = runBlocking {
        requestStream.onNext(createRequest())

        val response = withTimeout(timeout) {
            responseStream.receiverChannel.receive()
        }

        when (response.status) {
            ClientConsumerACKCodes.OK.id -> offset = response.offset + 1
        }

        response.toConsumerResponse()
    }

    override fun close() {
        responseStream.onCompleted()
    }

    private fun createRequest(): RpcConsumerRequest {
        return RpcConsumerRequest.newBuilder()
            .setTopic(topic)
            .setOffset(offset)
            .build()
    }
}
