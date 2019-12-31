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
*/

package org.kerala.client

import com.google.gson.GsonBuilder
import org.kerala.client.common.serialization.Consume
import org.kerala.client.common.serialization.Produce
import org.kerala.client.internal.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

typealias Block<T> = (T) -> Unit

class KeralaClient private constructor(internal val serviceInvoker: ServiceInvoker) {

    /**
     * Establish a connection with a Kerala node.
     *
     * @throws KeralaClientException if the kerala node cannot be contacted.
     */
    @Throws(KeralaClientException::class)
    fun connect() {
        serviceInvoker.bootstrap()
    }

    @Throws(KeralaClientException::class)
    fun getTopics(onSuccess: Block<KGetTopicsResponse>) {
        wrapCommand(GET_TOPICS_COMMAND_NAME, onSuccess = onSuccess)
    }

    @Throws(KeralaClientException::class)
    fun getTopics(): Future<KGetTopicsResponse> {
        return CompletableFuture<KGetTopicsResponse>().apply {
            getTopics {
                complete(it)
            }
        }
    }

    @Throws(KeralaClientException::class)
    fun createTopic(namespace: String, onSuccess: Block<KCommandResponse> = {}) {
        wrapCommand(CREATE_TOPIC_COMMAND_NAME, listOf(argPair("namespace", namespace)), onSuccess = onSuccess)
    }

    @Throws(KeralaClientException::class)
    fun createTopic(namespace: String): Future<KCommandResponse> {
        return CompletableFuture<KCommandResponse>().apply {
            createTopic(namespace) {
                complete(it)
            }
        }
    }

    @Throws(KeralaClientException::class)
    fun deleteTopic(namespace: String, onSuccess: Block<KCommandResponse> = {}) {
        wrapCommand(DELETE_TOPIC_COMMAND_NAME, listOf(argPair("namespace", namespace)), onSuccess = onSuccess)
    }

    @Throws(KeralaClientException::class)
    fun deleteTopic(namespace: String): Future<KCommandResponse> {
        return CompletableFuture<KCommandResponse>().apply {
            deleteTopic(namespace) {
                complete(it)
            }
        }
    }

    fun <K, V> producer(produce: Produce<K, V>): KeralaProducer<K, V> = Producer(this, produce)

    fun <K, V> producer(produce: Produce<K, V>, block: KeralaProducer<K, V>.() -> Unit): KeralaProducer<K, V> =
        Producer(this, produce).apply {
            block()
            close()
        }

    fun <K, V> consumer(topic: String, consume: Consume<K, V>, from: Long = KeralaConsumer.FROM_START): KeralaConsumer<K, V> = Consumer(this, consume, topic, from)

    fun <K, V> consumer(topic: String, consume: Consume<K, V>, from: Long = KeralaConsumer.FROM_START, block: KeralaConsumer<K, V>.() -> Unit = {}): KeralaConsumer<K, V> =
        Consumer(this, consume, topic, from).apply {
            block()
            close()
        }

    private inline fun <reified T> wrapCommand(commandName: String, args: List<KeralaArgPair> = emptyList(), crossinline onSuccess: Block<T>) {
        serviceInvoker.clientCommand(KeralaCommandRequest.newBuilder().setCommand(commandName).addAllArgs(args).build()) { response ->
            when (response.responseCode) {
                KClientCommandACKCodes.OK.id -> onSuccess(GsonBuilder().create().fromJson(response.response, T::class.java))
                KClientCommandACKCodes.ERROR.id -> throw KeralaClientException(response.response)
            }
        }
    }

    companion object {
        /**
         * Create an instance of the KeralaClient.
         *
         * @param host bootstrap server host
         * @param port bootstrap server port
         * @return a configured KeralaClient instance
         */
        fun create(host: String = "localhost", port: Int = 9191): KeralaClient {
            return KeralaClient(ServiceInvoker(ServiceBroker(host, port)))
        }

        private const val GET_TOPICS_COMMAND_NAME = "topics"
        private const val CREATE_TOPIC_COMMAND_NAME = "create-topic"
        private const val DELETE_TOPIC_COMMAND_NAME = "delete-topic"
    }
}
