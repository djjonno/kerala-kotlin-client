package org.kerala.client.common.serialization

import org.junit.Test
import kotlin.test.assertEquals

internal class IntSerializerTest {

  private val serializer = IntSerializer()
  private val deserializer = IntDeserializer()

  @Test
  fun `should serialize int`() {
    // Given
    val original = Int.MAX_VALUE

    // When
    val serialized = serializer.serialize(original)

    // Then
    assertEquals(BYTE_LENGTH, serialized.size)
    assertEquals(original, deserializer.deserialize(serialized))
  }

  companion object {
    const val BYTE_LENGTH = 4
  }
}
