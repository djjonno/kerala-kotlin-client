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
package org.kerala.client.common.serialization

class DoubleSerializer : Serializer<Double> {
  override fun serialize(data: Double): ByteArray {
    val bits = java.lang.Double.doubleToLongBits(data)
    return byteArrayOf(
        (bits ushr 56).toByte(),
        (bits ushr 48).toByte(),
        (bits ushr 40).toByte(),
        (bits ushr 32).toByte(),
        (bits ushr 24).toByte(),
        (bits ushr 16).toByte(),
        (bits ushr 8).toByte(),
        bits.toByte()
    )
  }
}
