package org.kerala.client.internal

import com.google.protobuf.ByteString
import com.nhaarman.mockitokotlin2.*
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.kerala.client.KClientConsumerACKCodes
import org.kerala.client.KeralaClient
import org.kerala.client.KeralaClientException
import org.kerala.client.common.serialization.Consume
import org.kerala.client.common.serialization.Deserializer
import org.kerala.client.common.serialization.Serde
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals

internal class ConsumerTest {

  @Mock lateinit var keyDeserializer: Deserializer<String>
  @Mock lateinit var valueDeserializer: Deserializer<String>
  @Mock lateinit var serviceInvoker: ServiceInvoker
  @Mock lateinit var requestStream: StreamObserver<KeralaConsumerRequest>
  @Mock lateinit var responseStreamObserver: ChannelStreamObserver<KeralaConsumerResponse>
  private lateinit var keySerde: Serde<String>
  private lateinit var valueSerde: Serde<String>
  private lateinit var client: KeralaClient
  private lateinit var consume: Consume<String, String>
  private lateinit var consumer: Consumer<String, String>

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)

    keySerde = mock {
      on { deserializer } doReturn keyDeserializer
    }

    valueSerde = mock {
      on { deserializer } doReturn valueDeserializer
    }

    consume = mock {
      on { keySerde } doReturn keySerde
      on { valueSerde } doReturn valueSerde
    }

    client = mock {
      on { serviceInvoker } doReturn serviceInvoker
    }

    setupConsumerResponse(TOPIC, OFFSET, listOf())

    whenever(serviceInvoker.consumeTopic(any()))
        .thenReturn(requestStream)

    consumer = Consumer(
        client,
        consume,
        TOPIC,
        OFFSET,
        responseStreamObserver
    )
  }

  @Test
  fun `should dispatch consumer request to stream and increment offset`() {
    // Given
    setupConsumerResponse(TOPIC, OFFSET, listOf())

    // When
    consumer.poll(1_000)

    // Then
    verify(requestStream).onNext(argThat {
      offset == OFFSET && topic == TOPIC
    })

    // When
    consumer.poll(1_000)

    // Then
    verify(requestStream).onNext(argThat {
      offset == OFFSET + 1 && topic == TOPIC
    })
  }

  @Test
  fun `should return expected consumer response`() {
    // Given
    val key = "key"
    val value = "value"
    val keyByteArray = key.toByteArray()
    val valueByteArray = value.toByteArray()
    setupConsumerResponse(TOPIC, OFFSET, listOf(
        KeralaKV.newBuilder()
            .setKey(ByteString.copyFrom(keyByteArray))
            .setValue(ByteString.copyFrom(valueByteArray)).build()
    ))

    whenever(keyDeserializer.deserialize(keyByteArray))
        .thenReturn(key)
    whenever(valueDeserializer.deserialize(valueByteArray))
        .thenReturn(value)

    // When
    val response = consumer.poll(1_000)

    // Then
    assertEquals(response.kvs.first().key, key)
    assertEquals(response.kvs.first().value, value)
    assertEquals(response.topic, TOPIC)
    assertEquals(response.offset, OFFSET)
  }

  @Test(expected = KeralaClientException::class)
  fun `should throw exception on consumer request error`() {
    // Given
    setupConsumerResponse(TOPIC, OFFSET, listOf(), responseCode = KClientConsumerACKCodes.ERROR.id)

    // When
    consumer.poll(1_000)

    // Then - exception thrown
  }

  @Test
  fun `should deserialize key value with serde`() {
    // Given
    val keyByteArray = "key".toByteArray()
    val valueByteArray = "value".toByteArray()
    setupConsumerResponse(TOPIC, OFFSET, listOf(
        KeralaKV.newBuilder()
            .setKey(ByteString.copyFrom(keyByteArray))
            .setValue(ByteString.copyFrom(valueByteArray)).build()
    ))

    // When
    consumer.poll(1_000)

    // Then
    verify(keyDeserializer).deserialize(keyByteArray)
    verify(valueDeserializer).deserialize(valueByteArray)
  }

  @Test
  fun `should close stream`() {
    // Given / When
    consumer.close()

    // Then
    verify(responseStreamObserver).onCompleted()
  }

  @Ignore
  @Test(expected = TimeoutCancellationException::class)
  fun `should timeout poll on response stream`() = runBlockingTest {
    // Given
    advanceTimeBy(2_000)

    // When
    consumer.poll(1_000)

    // Then - exception thrown
  }

  private fun setupConsumerResponse(topic: String, offset: Long, kvs: List<KeralaKV>, responseCode: Int = KClientConsumerACKCodes.OK.id) {
    runBlocking {
      whenever(responseStreamObserver.receive())
          .doReturn(KeralaConsumerResponse.newBuilder()
              .setTopic(topic)
              .setOffset(offset)
              .setResponseCode(responseCode)
              .addAllKvs(kvs)
              .build())
    }
  }

  companion object {
    const val OFFSET = 0L
    const val TOPIC = "test-topic"
  }
}
