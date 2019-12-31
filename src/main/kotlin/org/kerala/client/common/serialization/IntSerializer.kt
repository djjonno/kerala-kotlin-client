package org.kerala.client.common.serialization

class IntSerializer : Serializer<Int> {
  override fun serialize(data: Int): ByteArray {
    return byteArrayOf(
        (data ushr 24).toByte(),
        (data ushr 16).toByte(),
        (data ushr 8).toByte(),
        data.toByte()
    )
  }
}
