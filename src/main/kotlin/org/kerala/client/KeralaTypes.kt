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

package org.kerala.client

data class KNodeResponse(val id: String, val host: String, val port: Int, val leader: Boolean)

data class KClusterInfoResponse(val nodes: List<KNodeResponse>)

data class KTopicMetaResponse(val id: String, val namespace: String, val index: Long)

data class KGetTopicsResponse(val topics: List<KTopicMetaResponse>)

data class KCommandResponse(val message: String)

data class KProducerResponse(val responseCode: Int)

data class KConsumerResponse<K, V>(val topic: String, val offset: Long, val responseCode: Int, val kvs: List<KKV<K, V>>)

/**
 * Status codes returned by server when sending a Command request.
 */
enum class KClientCommandACKCodes(val id: Int) {
    /**
     * OK - command was successful.
     */
    OK(0),

    /**
     * ERROR - command failed.
     */
    ERROR(1)
}

/* Server Consumer Response Codes */
enum class KClientConsumerACKCodes(val id: Int) {
    /**
     * OK - consumer request was successful.
     */
    OK(0),

    /**
     * ERROR - consumer request was not successful.  This is a generic catch-all error;
     * recommendation is to retry sending the request.
     */
    ERROR(1),

    /**
     * NETWORK_ERROR - a network error occurred while processing the request.
     */
    NETWORK_ERROR(2),

    /**
     * INVALID_OPERATION - the Kerala node does not support this operation type.
     */
    INVALID_OPERATION(3),

    /**
     * TOPIC_UNKNOWN - the topic does not exist on this Kerala node.
     */
    TOPIC_UNKNOWN(4);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id }
    }
}

/* Server Producer Response Codes */
enum class KClientProducerACKCodes(val id: Int) {
    /**
     * OK - producer request was successful.
     */
    OK(0),

    /**
     * ERROR - consumer request was not successful.  This is a generic catch-all error;
     * recommendation is to retry sending the request.
     */
    ERROR(1),

    /**
     * NETWORK_ERROR - a network error occurred while processing the request.
     */
    NETWORK_ERROR(2),

    /**
     * INVALID_OPERATION - the Kerala node does not support this operation type.
     */
    INVALID_OPERATION(3),

    /**
     * TOPIC_UNKNOWN - the topic does not exist on this Kerala node.
     */
    TOPIC_UNKNOWN(4),

    /**
     * TIMEOUT - the connection/production has timed out.
     */
    TIMEOUT(5);

    companion object {
        fun fromId(id: Int) = values().firstOrNull { it.id == id }
    }
}
