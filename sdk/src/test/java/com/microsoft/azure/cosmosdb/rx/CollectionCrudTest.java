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
package com.microsoft.azure.cosmosdb.rx;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.UnknownHostException;

import com.microsoft.azure.cosmosdb.DatabaseForTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import com.microsoft.azure.cosmosdb.ConnectionPolicy;
import com.microsoft.azure.cosmosdb.ConsistencyLevel;
import com.microsoft.azure.cosmosdb.Database;
import com.microsoft.azure.cosmosdb.DocumentCollection;
import com.microsoft.azure.cosmosdb.IndexingMode;
import com.microsoft.azure.cosmosdb.IndexingPolicy;
import com.microsoft.azure.cosmosdb.ResourceResponse;
import com.microsoft.azure.cosmosdb.rx.AsyncDocumentClient;

import rx.Observable;

import javax.net.ssl.SSLException;

public class CollectionCrudTest extends TestSuiteBase {
    private static final int TIMEOUT = 30000;
    private static final int SETUP_TIMEOUT = 20000;
    private static final int SHUTDOWN_TIMEOUT = 20000;
    private final AsyncDocumentClient.Builder clientBuilder;
    private final String databaseId = DatabaseForTest.generateId();

    private AsyncDocumentClient client;
    private Database database;

    @Factory(dataProvider = "clientBuildersWithDirect")
    public CollectionCrudTest(AsyncDocumentClient.Builder clientBuilder) {
        this.clientBuilder = clientBuilder;
        this.subscriberValidationTimeout = TIMEOUT;
    }

    @Test(groups = { "emulator" }, timeOut = TIMEOUT)
    public void createCollection() throws Exception {
        DocumentCollection collectionDefinition = getCollectionDefinition();
        
        Observable<ResourceResponse<DocumentCollection>> createObservable = client
                .createCollection(database.getSelfLink(), collectionDefinition, null);

        ResourceResponseValidator<DocumentCollection> validator = new ResourceResponseValidator.Builder<DocumentCollection>()
                .withId(collectionDefinition.getId()).build();
        
        validateSuccess(createObservable, validator);
    }

    @Test(groups = { "emulator" }, timeOut = TIMEOUT)
    public void readCollection() throws Exception {
        DocumentCollection collectionDefinition = getCollectionDefinition();
        
        Observable<ResourceResponse<DocumentCollection>> createObservable = client.createCollection(database.getSelfLink(), collectionDefinition,
                null);
        DocumentCollection collection = createObservable.toBlocking().single().getResource();

        Observable<ResourceResponse<DocumentCollection>> readObservable = client.readCollection(collection.getSelfLink(), null);

        ResourceResponseValidator<DocumentCollection> validator = new ResourceResponseValidator.Builder<DocumentCollection>()
                .withId(collection.getId()).build();
        validateSuccess(readObservable, validator);
    }
    
    @Test(groups = { "emulator" }, timeOut = TIMEOUT)
    public void readCollection_NameBase() throws Exception {
        DocumentCollection collectionDefinition = getCollectionDefinition();
        
        Observable<ResourceResponse<DocumentCollection>> createObservable = client.createCollection(database.getSelfLink(), collectionDefinition,
                null);
        DocumentCollection collection = createObservable.toBlocking().single().getResource();
        
        Observable<ResourceResponse<DocumentCollection>> readObservable = client.readCollection(
                Utils.getCollectionNameLink(database.getId(), collection.getId()), null);

        ResourceResponseValidator<DocumentCollection> validator = new ResourceResponseValidator.Builder<DocumentCollection>()
                .withId(collection.getId()).build();
        validateSuccess(readObservable, validator);
    }
    
    @Test(groups = { "emulator" }, timeOut = TIMEOUT)
    public void readCollection_DoesntExist() throws Exception {

        Observable<ResourceResponse<DocumentCollection>> readObservable = client
                .readCollection(Utils.getCollectionNameLink(database.getId(), "I don't exist"), null);

        FailureValidator validator = new FailureValidator.Builder().resourceNotFound().build();
        validateFailure(readObservable, validator);
    }
    
    @Test(groups = { "emulator" }, timeOut = TIMEOUT)
    public void deleteCollection() throws Exception {
        DocumentCollection collectionDefinition = getCollectionDefinition();
        
        Observable<ResourceResponse<DocumentCollection>> createObservable = client.createCollection(database.getSelfLink(), collectionDefinition, null);
        DocumentCollection collection = createObservable.toBlocking().single().getResource();

        Observable<ResourceResponse<DocumentCollection>> deleteObservable = client.deleteCollection(collection.getSelfLink(),
                null);

        ResourceResponseValidator<DocumentCollection> validator = new ResourceResponseValidator.Builder<DocumentCollection>()
                .nullResource().build();
        validateSuccess(deleteObservable, validator);
    }
    
    @Test(groups = { "emulator" }, timeOut = TIMEOUT)
    public void replaceCollection()  {
        // create a collection
        DocumentCollection collectionDefinition = getCollectionDefinition();
        Observable<ResourceResponse<DocumentCollection>> createObservable = client.createCollection(database.getSelfLink(), collectionDefinition, null);
        DocumentCollection collection = createObservable.toBlocking().single().getResource();
        // sanity check
        assertThat(collection.getIndexingPolicy().getIndexingMode()).isEqualTo(IndexingMode.Consistent);
        
        // replace indexing mode
        IndexingPolicy indexingMode = new IndexingPolicy();
        indexingMode.setIndexingMode(IndexingMode.Lazy);
        collection.setIndexingPolicy(indexingMode);
        Observable<ResourceResponse<DocumentCollection>> readObservable = client.replaceCollection(collection, null);
        
        // validate
        ResourceResponseValidator<DocumentCollection> validator = new ResourceResponseValidator.Builder<DocumentCollection>()
                .indexingMode(IndexingMode.Lazy).build();
        validateSuccess(readObservable, validator);
    }

    @BeforeClass(groups = { "emulator" }, timeOut = SETUP_TIMEOUT)
    public void beforeClass() {
        client = clientBuilder.build();
        database = createDatabase(client, databaseId);
    }

    @AfterClass(groups = { "emulator" }, timeOut = SHUTDOWN_TIMEOUT, alwaysRun = true)
    public void afterClass() {
        safeDeleteDatabase(client, databaseId);
        safeClose(client);
    }
}