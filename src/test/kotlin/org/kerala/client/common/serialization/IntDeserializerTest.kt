package org.kerala.client.common.serialization

import org.junit.Test
import kotlin.test.assertEquals

class IntDeserializerTest {

  private val serializer = IntSerializer()
  private val deserializer = IntDeserializer()

  @Test
  fun `should deserialize int`() {
    // Given
    val original = Int.MAX_VALUE
    val serialized = serializer.serialize(original)

    // When
    val actual = deserializer.deserialize(serialized)

    // Then
    assertEquals(original, actual)
  }

  @Test(expected = SerializationException::class)
  fun `should throw exception if byte length too large`() {
    // Given
    val long = ByteArray(BYTE_LENGTH + 1)

    // When
    deserializer.deserialize(long)

    // Then - exception thrown
  }

  companion object {
    const val BYTE_LENGTH = 4
  }
}
