/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       ZF Friedrichshafen AG - add management api configurations
 *       Fraunhofer Institute for Software and Systems Engineering - added IDS API context
 *
 */

package org.eclipse.dataspaceconnector.system.tests.local;

import com.azure.core.util.BinaryData;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.system.tests.local.BlobTransferLocalSimulation.ACCOUNT_NAME_PROPERTY;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.API_KEY;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.API_KEY_HEADER;
import static org.eclipse.dataspaceconnector.system.tests.local.TransferLocalSimulation.CONSUMER_MANAGEMENT_URL;
import static org.eclipse.dataspaceconnector.system.tests.utils.GatlingUtils.runGatling;
import static org.eclipse.dataspaceconnector.system.tests.utils.TestUtils.requiredPropOrEnv;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.PROVIDER_ASSET_FILE;
import static org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils.TRANSFER_PROCESSES_PATH;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class BlobTransferIntegrationTest {
    public static final boolean USE_CLOUD_RESOURCES = parseBoolean(requiredPropOrEnv("use.cloud.resources", "true"));
    public static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    public static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";
    public static final String PROVIDER_CONTAINER_NAME = "src-container";

    //Local Resource
    private List<Runnable> containerCleanup = new ArrayList<>();
    public static final String LOCAL_BLOB_STORE_ENDPOINT_TEMPLATE = "http://127.0.0.1:10000/%s";
    public static final String LOCAL_SOURCE_BLOB_STORE_ACCOUNT = "providerassets";
    public static final String LOCAL_SOURCE_BLOB_STORE_ACCOUNT_KEY = "key1";
    public static final String LOCAL_DESTINATION_BLOB_STORE_ACCOUNT = "consumereuassets";
    public static final String LOCAL_DESTINATION_BLOB_STORE_ACCOUNT_KEY = "key2";

    @AfterEach
    public void teardown() {
        containerCleanup.parallelStream().forEach(Runnable::run);
    }

    @Test
    public void transferBlob_success() {
        // Arrange
        BlobServiceClient dstBlobServiceClient;
        if (USE_CLOUD_RESOURCES) {
            var destinationKeyVaultName = requiredPropOrEnv("consumer.eu.key.vault", null);
            var blobAccountDetails = blobAccount(destinationKeyVaultName);
            var storageAccountName = blobAccountDetails.get(0);
            var storageAccountKey = blobAccountDetails.get(1);
            dstBlobServiceClient = getBlobServiceClient(
                    format(BLOB_STORE_ENDPOINT_TEMPLATE, storageAccountName),
                    storageAccountName,
                    storageAccountKey
            );
        } else {
            // without cloud resources.
            var srcBlobServiceClient = getBlobServiceClient(
                    format(LOCAL_BLOB_STORE_ENDPOINT_TEMPLATE, LOCAL_SOURCE_BLOB_STORE_ACCOUNT),
                    LOCAL_SOURCE_BLOB_STORE_ACCOUNT,
                    LOCAL_SOURCE_BLOB_STORE_ACCOUNT_KEY
            );
            dstBlobServiceClient = getBlobServiceClient(
                    format(LOCAL_BLOB_STORE_ENDPOINT_TEMPLATE, LOCAL_DESTINATION_BLOB_STORE_ACCOUNT),
                    LOCAL_DESTINATION_BLOB_STORE_ACCOUNT,
                    LOCAL_DESTINATION_BLOB_STORE_ACCOUNT_KEY
            );

            // Upload a blob with test data on provider blob container.
            createContainer(srcBlobServiceClient, PROVIDER_CONTAINER_NAME);
            srcBlobServiceClient.getBlobContainerClient(PROVIDER_CONTAINER_NAME)
                    .getBlobClient(PROVIDER_ASSET_FILE)
                    .upload(BinaryData.fromString(UUID.randomUUID().toString()), true);
        }


        // Act
        System.setProperty(ACCOUNT_NAME_PROPERTY, dstBlobServiceClient.getAccountName());
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var container = getProvisionedContainerName();
        var destinationBlob = dstBlobServiceClient.getBlobContainerClient(container)
                .getBlobClient(PROVIDER_ASSET_FILE);
        assertThat(destinationBlob.exists())
                .withFailMessage("Destination blob %s not created", destinationBlob.getBlobUrl())
                .isTrue();
    }

    /**
     * Provides Blob storage account name and key.
     *
     * @param keyVaultName Key Vault name. This key vault must have storage account key secrets.
     * @return storage account name and account key on first and second position of list.
     */
    private List<String> blobAccount(String keyVaultName) {
        var credential = new DefaultAzureCredentialBuilder().build();
        var vault = new SecretClientBuilder()
                .vaultUrl(format(KEY_VAULT_ENDPOINT_TEMPLATE, keyVaultName))
                .credential(credential)
                .buildClient();
        // Find the first account with a key in the key vault
        var accountKeySecret = vault.listPropertiesOfSecrets().stream().filter(s -> s.getName().endsWith("-key1")).findFirst().orElseThrow(
                () -> new AssertionError("Key vault " + keyVaultName + " should contain the storage account key")
        );
        var accountKey = vault.getSecret(accountKeySecret.getName());
        var accountName = accountKeySecret.getName().replaceFirst("-key1$", "");

        return List.of(accountName, accountKey.getValue());
    }

    private void createContainer(BlobServiceClient client, String containerName) {
        assertFalse(client.getBlobContainerClient(containerName).exists());

        BlobContainerClient blobContainerClient = client.createBlobContainer(containerName);
        assertTrue(blobContainerClient.exists());
        containerCleanup.add(() -> client.deleteBlobContainer(containerName));
    }

    @NotNull
    private BlobServiceClient getBlobServiceClient(String endpoint, String accountName, String accountKey) {

        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new StorageSharedKeyCredential(accountName, accountKey))
                .buildClient();
    }

    private String getProvisionedContainerName() {
        ResponseBodyExtractionOptions body = given()
                .baseUri(CONSUMER_MANAGEMENT_URL)
                .header(API_KEY_HEADER, API_KEY)
                .when()
                .get(TRANSFER_PROCESSES_PATH)
                .then()
                .statusCode(200)
                .extract().body();
        return body
                .jsonPath().getString("[0].dataDestination.properties.container");
    }


}
