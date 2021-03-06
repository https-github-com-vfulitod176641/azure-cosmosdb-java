/*
 * The MIT License (MIT)
 * Copyright (c) 2018 Microsoft Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
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

package com.microsoft.azure.cosmosdb.internal.directconnectivity;

import com.microsoft.azure.cosmosdb.BridgeInternal;
import com.microsoft.azure.cosmosdb.DocumentClientException;
import com.microsoft.azure.cosmosdb.internal.HttpConstants;
import com.microsoft.azure.cosmosdb.internal.ISessionToken;
import com.microsoft.azure.cosmosdb.internal.InternalServerErrorException;
import com.microsoft.azure.cosmosdb.internal.RequestChargeTracker;
import com.microsoft.azure.cosmosdb.rx.internal.Exceptions;
import com.microsoft.azure.cosmosdb.rx.internal.RMResources;
import com.microsoft.azure.cosmosdb.rx.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class StoreResult {
    private final static Logger logger = LoggerFactory.getLogger(StoreResult.class);

    private final StoreResponse storeResponse;
    private final DocumentClientException exception;

    final public long lsn;
    final public String partitionKeyRangeId;
    final public long quorumAckedLSN;
    final public long globalCommittedLSN;
    final public long numberOfReadRegions;
    final public long itemLSN;
    final public ISessionToken sessionToken;
    final public double requestCharge;
    final public int currentReplicaSetSize;
    final public int currentWriteQuorum;
    final public boolean isValid;
    final public boolean isGoneException;
    final public boolean isNotFoundException;
    final public boolean isInvalidPartitionException;
    final public Uri storePhysicalAddress;

    public StoreResult(
            StoreResponse storeResponse,
            DocumentClientException exception,
            String partitionKeyRangeId,
            long lsn,
            long quorumAckedLsn,
            double requestCharge,
            int currentReplicaSetSize,
            int currentWriteQuorum,
            boolean isValid,
            Uri storePhysicalAddress,
            long globalCommittedLSN,
            int numberOfReadRegions,
            long itemLSN,
            ISessionToken sessionToken) {
        this.storeResponse = storeResponse;
        this.exception = exception;
        this.partitionKeyRangeId = partitionKeyRangeId;
        this.lsn = lsn;
        this.quorumAckedLSN = quorumAckedLsn;
        this.requestCharge = requestCharge;
        this.currentReplicaSetSize = currentReplicaSetSize;
        this.currentWriteQuorum = currentWriteQuorum;
        this.isValid = isValid;
        this.isGoneException = this.exception != null && this.exception.getStatusCode() == HttpConstants.StatusCodes.GONE;
        this.isNotFoundException = this.exception != null && this.exception.getStatusCode() == HttpConstants.StatusCodes.NOTFOUND;
        this.isInvalidPartitionException = this.exception != null
                && Exceptions.isNameCacheStale(this.exception);
        this.storePhysicalAddress = storePhysicalAddress;
        this.globalCommittedLSN = globalCommittedLSN;
        this.numberOfReadRegions = numberOfReadRegions;
        this.itemLSN = itemLSN;
        this.sessionToken = sessionToken;
    }

    public DocumentClientException getException() throws InternalServerErrorException {
        if (this.exception == null) {
            String message = "Exception should be available but found none";
            assert false : message;
            logger.error(message);
            throw new InternalServerErrorException(RMResources.InternalServerError);
        }

        return exception;
    }

    public StoreResponse toResponse() throws DocumentClientException {
        return toResponse(null);
    }

    public StoreResponse toResponse(RequestChargeTracker requestChargeTracker) throws DocumentClientException {
        if (!this.isValid) {
            if (this.exception == null) {
                logger.error("Exception not set for invalid response");
                throw new InternalServerErrorException(RMResources.InternalServerError);
            }

            throw this.exception;
        }

        if (requestChargeTracker != null && this.isValid) {
            StoreResult.setRequestCharge(this.storeResponse, this.exception, requestChargeTracker.getTotalRequestCharge());
        }

        if (this.exception != null) {
            throw exception;
        }

        return this.storeResponse;
    }

    private static void setRequestCharge(StoreResponse response, DocumentClientException documentClientException, double totalRequestCharge) {
        if (documentClientException != null) {
            documentClientException.getResponseHeaders().put(HttpConstants.HttpHeaders.REQUEST_CHARGE,
                    Double.toString(totalRequestCharge));
        }
        // Set total charge as final charge for the response.
        else if (response.getResponseHeaderNames() != null) {
            for (int i = 0; i < response.getResponseHeaderNames().length; ++i) {
                if (Strings.areEqualIgnoreCase(
                        response.getResponseHeaderNames()[i],
                        HttpConstants.HttpHeaders.REQUEST_CHARGE)) {
                    response.getResponseHeaderValues()[i] = Double.toString(totalRequestCharge);
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        int statusCode = 0;
        int subStatusCode = HttpConstants.SubStatusCodes.UNKNOWN;

        if (this.storeResponse != null) {
            statusCode = this.storeResponse.getStatus();
            subStatusCode = this.storeResponse.getSubStatusCode();
        } else if (this.exception != null) {
            statusCode = this.exception.getStatusCode();
            subStatusCode = this.exception.getSubStatusCode();
        }

        return "storePhysicalAddress: " + this.storePhysicalAddress +
                ", lsn: " + this.lsn +
                ", globalCommittedLsn: " + this.globalCommittedLSN +
                ", partitionKeyRangeId: " + this.partitionKeyRangeId +
                ", isValid: " + this.isValid +
                ", statusCode: " + statusCode +
                ", subStatusCode: " + subStatusCode +
                ", isGone: " + this.isGoneException +
                ", isNotFound: " + this.isNotFoundException +
                ", isInvalidPartition: " + this.isInvalidPartitionException +
                ", requestCharge: " + this.requestCharge +
                ", itemLSN: " + this.itemLSN +
                ", sessionToken: " + (this.sessionToken != null ? this.sessionToken.convertToString() : null) +
                ", exception: " + BridgeInternal.getInnerErrorMessage(this.exception);
    }
}
