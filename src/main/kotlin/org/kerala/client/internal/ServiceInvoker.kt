package org.kerala.client.internal

import io.grpc.stub.StreamObserver
import org.kerala.client.*

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

internal class ServiceInvoker(private val serviceBroker: ServiceBroker) {
    private val commandService = ClientServiceGrpc.newFutureStub(serviceBroker.writeChannel())

    fun bootstrap() {
        serviceBroker.bootstrap()
    }

    fun clientCommand(commandRequest: RpcCommandRequest, onSuccess: (RpcCommandResponse) -> Unit = {}) {
        onSuccess(commandService.clientCommand(commandRequest).get())
    }

    fun produceTopic(responseObserver: StreamObserver<RpcProducerResponse>): StreamObserver<RpcProducerRequest> {
        return ClientServiceGrpc.newStub(serviceBroker.writeChannel()).topicProducer(responseObserver)
    }

    fun consumeTopic(responseObserver: StreamObserver<RpcConsumerResponse>): StreamObserver<RpcConsumerRequest> {
        return ClientServiceGrpc.newStub(serviceBroker.readChannel()).topicConsumer(responseObserver)
    }
}
