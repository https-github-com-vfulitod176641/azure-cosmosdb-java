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

package com.microsoft.azure.cosmosdb;

import com.microsoft.azure.cosmosdb.internal.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a partition key definition in the Azure Cosmos DB database service. A partition key definition specifies which
 * document property is used as the partition key in a collection that has multiple partitions.
 */
public final class PartitionKeyDefinition extends JsonSerializable {
    private List<String> paths;
    private PartitionKind kind;
    private PartitionKeyDefinitionVersion version;

    /**
     * Constructor. Creates a new instance of the PartitionKeyDefinition object.
     */
    public PartitionKeyDefinition() {
        this.setKind(PartitionKind.Hash);
    }

    /**
     * Constructor. Creates a new instance of the PartitionKeyDefinition object from a
     * JSON string.
     *
     * @param jsonString the JSON string that represents the partition key definition.
     */
    public PartitionKeyDefinition(String jsonString) {
        super(jsonString);
    }

    /**
     * Sets the partition algorithm used to calculate the partition id given a partition key.
     *
     * @return the partition algorithm.
     */
    public PartitionKind getKind() {
        if (this.kind == null) {
            this.kind = super.getObject(Constants.Properties.PARTITION_KIND, PartitionKind.class);
        }

        return this.kind;
    }

    /**
     * Sets the partition algorithm used to calculate the partition id given a partition key.
     *
     * @param kind the partition algorithm.
     */
    public void setKind(PartitionKind kind) {
        this.kind = kind;
    }

    PartitionKeyDefinitionVersion getVersion() {
        if (this.version == null) {
            version = super.getObject(Constants.Properties.PARTITION_KEY_DEFINITION_VERSION, PartitionKeyDefinitionVersion.class);
        }

        return this.version;
    }

    void setVersion(PartitionKeyDefinitionVersion version) {
        this.version = version;
    }

    /**
     * Gets the document property paths for the partition key.
     *
     * @return the paths to the document properties that form the partition key.
     */
    public List<String> getPaths() {
        if (this.paths == null) {
            if (super.has(Constants.Properties.PARTITION_KEY_PATHS)) {
                paths = super.getList(Constants.Properties.PARTITION_KEY_PATHS, String.class);
            } else {
                paths = new ArrayList<>();
            }
        }

        return paths;
    }

    /**
     * Sets the document property paths for the partition key.
     *
     * @param paths the paths to document properties that form the partition key.
     */
    public void setPaths(List<String> paths) {
        if (paths == null || paths.size() == 0) {
            throw new IllegalArgumentException("paths must not be null or empty.");
        }

        this.paths = paths;
    }

    @Override
    void populatePropertyBag() {
        if (this.kind != null) {
            super.set(Constants.Properties.PARTITION_KIND, kind.name());
        }
        if (this.paths != null) {
            super.set(Constants.Properties.PARTITION_KEY_PATHS, paths);
        }

        if (this.version != null) {
            super.set(Constants.Properties.PARTITION_KEY_DEFINITION_VERSION, version.name());
        }
        super.populatePropertyBag();
    }
}