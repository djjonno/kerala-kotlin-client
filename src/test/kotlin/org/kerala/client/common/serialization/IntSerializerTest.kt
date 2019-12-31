package org.kerala.client.common.serialization

import org.junit.Test
import kotlin.test.assertEquals

class IntSerializerTest {
  @Test
  fun `should serialize to byte array`() {
    val expected = 101
    val serializer = IntSerializer()
    val bytes = serializer.serialize(expected)

    val actual = IntDeserializer().deserialize(bytes)

    assertEquals(expected, actual)
  }
}
