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

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import io.restassured.response.ResponseBodyExtractionOptions;
import org.eclipse.dataspaceconnector.system.tests.utils.TransferSimulationUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

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

public class BlobTransferIntegrationTest {
    public static final boolean USE_CLOUD_RESOURCES = parseBoolean(requiredPropOrEnv("use.cloud.resources", "false"));
    public static final String BLOB_STORE_ENDPOINT_TEMPLATE = "https://%s.blob.core.windows.net";
    public static final String KEY_VAULT_ENDPOINT_TEMPLATE = "https://%s.vault.azure.net";

    @Test
    public void transferBlob_success() {

        BlobServiceClient blobServiceClient2;
        if (USE_CLOUD_RESOURCES) {
            var destinationKeyVaultName = requiredPropOrEnv("consumer.eu.key.vault", null);
            var blobAccountDetails = blobAccount(destinationKeyVaultName);
            var storageAccountName = blobAccountDetails.get(0);
            var storageAccountKey = blobAccountDetails.get(1);
            blobServiceClient2 = getBlobServiceClient(
                    format(BLOB_STORE_ENDPOINT_TEMPLATE, storageAccountName),
                    storageAccountName,
                    storageAccountKey
            );
        } else {
            blobServiceClient2 = getBlobServiceClient(null, null, null);
        }


        // Act
        System.setProperty(ACCOUNT_NAME_PROPERTY, blobServiceClient2.getAccountName());
        runGatling(BlobTransferLocalSimulation.class, TransferSimulationUtils.DESCRIPTION);

        // Assert
        var container = getProvisionedContainerName();
        var destinationBlob = blobServiceClient2.getBlobContainerClient(container)
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
