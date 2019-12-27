package org.kerala.client

import com.google.gson.GsonBuilder
import org.kerala.client.internal.*
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

typealias Block<T> = (T) -> Unit

class KeralaClient private constructor(internal val serviceInvoker: ServiceInvoker) {

    /**
     * Establish a connection with the Kerala Server.
     *
     * This is not a prerequisite to actually communicating and establishing a
     * connection with the kerala server.  It merely exists as a method to
     * allow a client to fail fast and eagerly bootstrap itself.
     *
     * @throws KeralaClientException if the bootstrap server cannot be contacted.
     */
    @Throws(KeralaClientException::class)
    fun connect() {
        serviceInvoker.bootstrap()
    }

    @Throws(KeralaClientException::class)
    fun getTopics(onSuccess: Block<KeralaGetTopicsResponse>) {
        wrapCommand(GET_TOPICS_COMMAND_NAME, onSuccess = onSuccess)
    }

    @Throws(KeralaClientException::class)
    fun getTopics(): Future<KeralaGetTopicsResponse> {
        return CompletableFuture<KeralaGetTopicsResponse>().apply {
            getTopics {
                complete(it)
            }
        }
    }

    @Throws(KeralaClientException::class)
    fun createTopic(namespace: String, onSuccess: Block<KeralaCommandResponse> = {}) {
        wrapCommand(CREATE_TOPIC_COMMAND_NAME, listOf(argPair("namespace", namespace)), onSuccess = onSuccess)
    }

    @Throws(KeralaClientException::class)
    fun createTopic(namespace: String): Future<KeralaCommandResponse> {
        return CompletableFuture<KeralaCommandResponse>().apply {
            createTopic(namespace) {
                complete(it)
            }
        }
    }

    @Throws(KeralaClientException::class)
    fun deleteTopic(namespace: String, onSuccess: Block<KeralaCommandResponse> = {}) {
        wrapCommand(DELETE_TOPIC_COMMAND_NAME, listOf(argPair("namespace", namespace)), onSuccess = onSuccess)
    }

    @Throws(KeralaClientException::class)
    fun deleteTopic(namespace: String): Future<KeralaCommandResponse> {
        return CompletableFuture<KeralaCommandResponse>().apply {
            deleteTopic(namespace) {
                complete(it)
            }
        }
    }

    fun <K, V> producer(produced: Produced<K, V>, block: KeralaProducer<K, V>.() -> Unit = {}): KeralaProducer<K, V> =
        Producer(this, produced).apply {
            block()
            close()
        }

    fun <K, V> consumer(topic: String, consumed: Consumed<K, V>, from: Long = 1, block: KeralaConsumer<K, V>.() -> Unit = {}): KeralaConsumer<K, V> =
        Consumer(this, consumed, topic, from).apply {
            block()
            close()
        }

    private inline fun <reified T> wrapCommand(commandName: String, args: List<RpcArgPair> = emptyList(), crossinline onSuccess: Block<T>) {
        serviceInvoker.clientCommand(RpcCommandRequest.newBuilder().setCommand(commandName).addAllArgs(args).build()) { response ->
            when (response.status) {
                ClientCommandACKCodes.OK.id -> onSuccess(GsonBuilder().create().fromJson(response.response, T::class.java))
                ClientCommandACKCodes.ERROR.id -> throw KeralaClientException(response.response)
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
