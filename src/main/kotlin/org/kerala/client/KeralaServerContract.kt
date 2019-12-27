package org.kerala.client

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

data class KeralaNodeResponse(val id: String, val host: String, val port: Int, val leader: Boolean)
data class KeralaClusterInfoResponse(val nodes: List<KeralaNodeResponse>)
data class KeralaTopicMetaResponse(val namespace: String, val id: String)
data class KeralaGetTopicsResponse(val topics: List<KeralaTopicMetaResponse>)
data class KeralaCommandResponse(val message: String)
data class KeralaKV<K, V>(val key: K? = null, val value: V, val timestamp: Long = System.currentTimeMillis())
data class KeralaProducerResponse(val status: Int)
data class KeralaConsumerResponse<K, V>(val topic: String, val offset: Long, val kvs: List<KeralaKV<K, V>>, val status: Int)

/* Server Command Response Codes */
enum class ClientCommandACKCodes(val id: Int) {
    OK(0),
    ERROR(1)
}

/* Server Consumer Response Codes */
enum class ClientConsumerACKCodes(val id: Int) {
    OK(0),
    ERROR(1),
    NETWORK_ERROR(2),
    INVALID_OPERATION(3),
    TOPIC_UNKNOWN(4);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id }
    }
}

/* Server Producer Response Codes */
enum class ClientProducerACKCodes(val id: Int) {
    OK(0),
    ERROR(1),
    NETWORK_ERROR(2),
    INVALID_OPERATION(3),
    TOPIC_UNKNOWN(4),
    TIMEOUT(5);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id }
    }
}
