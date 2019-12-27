package org.kerala.client

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class LibraryTest : CoroutineScope by CoroutineScope(Dispatchers.IO) {
    @Test
    fun test() {
        val client = KeralaClient.create(port = 9191)

        client.createTopic("foo").get()

        val job1 = launch {
            client.producer(mock<Produced<String, String>>()) {
                var i = 0
                do {
                    println(send("foo", (0..i).map { KeralaKV("key-$it", "value-$it") }).get())
                } while (++i < 1000)
            }
        }

        val job2 = launch {
            client.consumer("foo", consumed = mock<Consumed<String, String>>()) {
                do {
                    try {
                        println(poll(5_000).kvs)
                    } catch (e: Exception) {
                        println(e)
                        break
                    }
                } while (true)
            }
        }

        runBlocking {
            job1.join()
            job2.join()
        }
    }
}
