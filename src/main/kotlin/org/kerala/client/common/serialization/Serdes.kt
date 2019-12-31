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

object Serdes {
  fun Int(): Serde<Int> = Serde(IntSerializer(), IntDeserializer())
  fun Long(): Serde<Long> = Serde(LongSerializer(), LongDeserializer())
  fun Float(): Serde<Float> = Serde(FloatSerializer(), FloatDeserializer())
  fun Double(): Serde<Double> = Serde(DoubleSerializer(), DoubleDeserializer())
  fun String(): Serde<String> = Serde(StringSerializer(), StringDeserializer())

  fun ByteArray(): Serde<ByteArray> = Serde(ByteArraySerializer(), ByteArrayDeserializer())

  fun Unit(): Serde<Unit> = Serde(UnitSerializer(), UnitDeserializer())
}
