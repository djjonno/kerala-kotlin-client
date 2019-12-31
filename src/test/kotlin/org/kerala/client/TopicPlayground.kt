package org.kerala.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.kerala.client.common.serialization.Consume
import org.kerala.client.common.serialization.Produce
import org.kerala.client.common.serialization.Serdes
import kotlin.test.Test

class TopicPlayground : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    private val keralaClient = KeralaClient.create(port = 9191)

    @Test
    fun test() {
        println(keralaClient.getTopics().get())


////        create_topics()
//        produce()
//        consume_and_produce()
        consume()
    }

    fun create_topics() {
        /* this stream will continue integers */
        keralaClient.createTopic("names")
        /* this stream will be a projection of the `integer-stream`
           where each integer is multiplied by 2 */
        keralaClient.createTopic("names-reversed")
    }

    fun produce() {
        val names = listOf(
            "jonathon",
            "leana",
            "ray",
            "liz",
            "chris",
            "kari"
        )
        /* this will produce 100 random integers to the stream */
        keralaClient.producer(Produce.with(Serdes.Unit(), Serdes.String())) {
            names.forEach { name ->
                send("names", Unit kv name).get()
            }
        }
    }

    fun consume_and_produce() {
        /* this will consume those integers from the `integer-stream`,
           multiply them by 2, and produce the result to the `doubled-stream`.
         */
        val producer = keralaClient.producer(Produce.with(Serdes.String(), Serdes.String()))
        keralaClient.consumer("names", Consume.with(Serdes.Unit(), Serdes.String())) {
            do {
                producer.send("names-reversed", poll(5_000).kvs.map { it.value kv it.value.reversed() }).get()
            } while (true)
        }
    }

    fun consume() {
        val integerConsumer = keralaClient.consumer("names", Consume.with(Serdes.Unit(), Serdes.String()))
        val doubleConsumer = keralaClient.consumer("names-reversed", Consume.with(Serdes.String(), Serdes.String()))
        do {
            try {
                println("original -> " + integerConsumer.poll(5_000).kvs.map { it.value })
                println("doubled -> " + doubleConsumer.poll(5_000).kvs.map { it.value })
            } catch (e: Exception) {
                break
            }
        } while (true)
    }
}
