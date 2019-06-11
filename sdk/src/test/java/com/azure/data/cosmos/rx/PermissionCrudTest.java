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
package com.azure.data.cosmos.rx;

import java.util.UUID;

import com.azure.data.cosmos.AsyncDocumentClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import com.azure.data.cosmos.Database;
import com.azure.data.cosmos.Permission;
import com.azure.data.cosmos.PermissionMode;
import com.azure.data.cosmos.ResourceResponse;
import com.azure.data.cosmos.User;
import com.azure.data.cosmos.internal.TestSuiteBase;

import rx.Observable;

//TODO: change to use external TestSuiteBase 
public class PermissionCrudTest extends TestSuiteBase {

    private Database createdDatabase;
    private User createdUser;

    private AsyncDocumentClient client;

    @Factory(dataProvider = "clientBuilders")
    public PermissionCrudTest(AsyncDocumentClient.Builder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    @Test(groups = { "simple" }, timeOut = TIMEOUT)
    public void createPermission() throws Exception {

        createdUser = safeCreateUser(client, createdDatabase.id(), getUserDefinition());
        //create permission
        Permission permission = new Permission();
        permission.id(UUID.randomUUID().toString());
        permission.setPermissionMode(PermissionMode.READ);
        permission.setResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=");
        

        Observable<ResourceResponse<Permission>> createObservable = client.createPermission(getUserLink(), permission, null);

        // validate permission creation
        ResourceResponseValidator<Permission> validator = new ResourceResponseValidator.Builder<Permission>()
                .withId(permission.id())
                .withPermissionMode(PermissionMode.READ)
                .withPermissionResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=")
                .notNullEtag()
                .build();
        validateSuccess(createObservable, validator);
    }

    @Test(groups = { "simple" }, timeOut = TIMEOUT)
    public void readPermission() throws Exception {
        createdUser = safeCreateUser(client, createdDatabase.id(), getUserDefinition());
        
        // create permission
        Permission permission = new Permission();
        permission.id(UUID.randomUUID().toString());
        permission.setPermissionMode(PermissionMode.READ);
        permission.setResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=");
        Permission readBackPermission = client.createPermission(getUserLink(), permission, null).toBlocking().single().getResource();

        // read Permission
        Observable<ResourceResponse<Permission>> readObservable = client.readPermission(readBackPermission.selfLink(), null);

        // validate permission read
        ResourceResponseValidator<Permission> validator = new ResourceResponseValidator.Builder<Permission>()
                .withId(permission.id())
                .withPermissionMode(PermissionMode.READ)
                .withPermissionResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=")
                .notNullEtag()
                .build();
        validateSuccess(readObservable, validator);
    }

    @Test(groups = { "simple" }, timeOut = TIMEOUT)
    public void deletePermission() throws Exception {
        
        createdUser = safeCreateUser(client, createdDatabase.id(), getUserDefinition());
        
        // create permission
        Permission permission = new Permission();
        permission.id(UUID.randomUUID().toString());
        permission.setPermissionMode(PermissionMode.READ);
        permission.setResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=");
        Permission readBackPermission = client.createPermission(getUserLink(), permission, null).toBlocking().single().getResource();
        
        // delete
        Observable<ResourceResponse<Permission>> deleteObservable = client.deletePermission(readBackPermission.selfLink(), null);

        // validate delete permission
        ResourceResponseValidator<Permission> validator = new ResourceResponseValidator.Builder<Permission>()
                .nullResource()
                .build();
        validateSuccess(deleteObservable, validator);

        waitIfNeededForReplicasToCatchUp(clientBuilder);

        // attempt to read the permission which was deleted
        Observable<ResourceResponse<Permission>> readObservable = client.readPermission(readBackPermission.selfLink(), null);
        FailureValidator notFoundValidator = new FailureValidator.Builder().resourceNotFound().build();
        validateFailure(readObservable, notFoundValidator);
    }

    @Test(groups = { "simple" }, timeOut = TIMEOUT)
    public void upsertPermission() throws Exception {
        
        createdUser = safeCreateUser(client, createdDatabase.id(), getUserDefinition());
        
        // create permission
        Permission permission = new Permission();
        permission.id(UUID.randomUUID().toString());
        permission.setPermissionMode(PermissionMode.READ);
        permission.setResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=");
        Permission readBackPermission = client.upsertPermission(getUserLink(), permission, null).toBlocking().single().getResource();

        // read Permission
        Observable<ResourceResponse<Permission>> readObservable = client.readPermission(readBackPermission.selfLink(), null);

        // validate permission creation
        ResourceResponseValidator<Permission> validatorForRead = new ResourceResponseValidator.Builder<Permission>()
                .withId(readBackPermission.id())
                .withPermissionMode(PermissionMode.READ)
                .withPermissionResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=")
                .notNullEtag()
                .build();
        validateSuccess(readObservable, validatorForRead);
        
        //update permission
        readBackPermission.setPermissionMode(PermissionMode.ALL);

        Observable<ResourceResponse<Permission>> updateObservable = client.upsertPermission(getUserLink(), readBackPermission, null);

        // validate permission update
        ResourceResponseValidator<Permission> validatorForUpdate = new ResourceResponseValidator.Builder<Permission>()
                .withId(readBackPermission.id())
                .withPermissionMode(PermissionMode.ALL)
                .withPermissionResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=")
                .notNullEtag()
                .build();
        validateSuccess(updateObservable, validatorForUpdate);   
    }
    
    @Test(groups = { "simple" }, timeOut = TIMEOUT)
    public void replacePermission() throws Exception {
        
        createdUser = safeCreateUser(client, createdDatabase.id(), getUserDefinition());
        
        // create permission
        Permission permission = new Permission();
        permission.id(UUID.randomUUID().toString());
        permission.setPermissionMode(PermissionMode.READ);
        permission.setResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=");
        Permission readBackPermission = client.createPermission(getUserLink(), permission, null).toBlocking().single().getResource();

        // read Permission
        Observable<ResourceResponse<Permission>> readObservable = client.readPermission(readBackPermission.selfLink(), null);

        // validate permission creation
        ResourceResponseValidator<Permission> validatorForRead = new ResourceResponseValidator.Builder<Permission>()
                .withId(readBackPermission.id())
                .withPermissionMode(PermissionMode.READ)
                .withPermissionResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=")
                .notNullEtag()
                .build();
        validateSuccess(readObservable, validatorForRead);
        
        //update permission
        readBackPermission.setPermissionMode(PermissionMode.ALL);

        Observable<ResourceResponse<Permission>> updateObservable = client.replacePermission(readBackPermission, null);

        // validate permission replace
        ResourceResponseValidator<Permission> validatorForUpdate = new ResourceResponseValidator.Builder<Permission>()
                .withId(readBackPermission.id())
                .withPermissionMode(PermissionMode.ALL)
                .withPermissionResourceLink("dbs/AQAAAA==/colls/AQAAAJ0fgTc=")
                .notNullEtag()
                .build();
        validateSuccess(updateObservable, validatorForUpdate);   
    }

    @BeforeClass(groups = { "simple" }, timeOut = SETUP_TIMEOUT)
    public void beforeClass() {
        client = clientBuilder.build();
        createdDatabase = SHARED_DATABASE;
    }

    @AfterClass(groups = { "simple" }, timeOut = SHUTDOWN_TIMEOUT, alwaysRun = true)
    public void afterClass() {
        safeClose(client);
    }

    private static User getUserDefinition() {
        User user = new User();
        user.id(UUID.randomUUID().toString());
        return user;
    }
    
    private String getUserLink() {
        return createdUser.selfLink();
    }
}