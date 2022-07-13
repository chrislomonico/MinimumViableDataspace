## System tests

The test copy a file from provider to consumer blob storage account.

### Building MVD project

MVD dependencies are Eclipse DataSpaceConnector(EDC) and Registration Service. Both of these dependencies are not published to any central artifactory yet so in local
development we have to use locally published dependencies, once this is done MVD can be build using

```bash
./gradlew build
```

#### Publish EDC and Registration Service to local Maven

Checkout [Eclipse DataSpaceConnector repository](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector).

Publish EDC libraries to local Maven artifactory by executing gradle command `./gradlew publishToMavenLocal -Pskip.signing` from EDC root
folder.

Checkout [Registration Service repository](https://github.com/eclipse-dataspaceconnector/RegistrationService).

Publish Registration Service libraries to local Maven artifactory by executing gradle command `./gradlew publishToMavenLocal` from Registration Service root
folder.

### Running test locally

MVD System tests can be executed locally against a local MVD instance. MVD runs three EDC Connectors and one Registration Service.

First please make sure that you are able to build MVD locally as described in [Building MVD project](#building-mvd-project) section.

- We need to build EDC Connector launcher and Registration Service launcher.
- Go to EDC root folder. And execute

    ```bash
    ./gradlew -DuseFsVault="true" :launcher:shadowJar
    ```

- Go to Registration service root folder. And execute

    ```bash
    ./gradlew :launcher:shadowJar
    ```

- Update Registration service launcher path in `system-tests/docker-compose.yml` file. Look for section `#UPDATE_REGISTRATION_SERVICE_LAUNCHER_PATH_HERE#` and update it e.g. `/home/user/RegistrationService/launcher`.

- Start MVD using docker-compose.yml file.

    ```bash
    docker-compose -f system-tests/docker-compose.yml up --build
    ```

- This will start three EDC Connectors, one Registration Service, one HTTP Nginx Server to serve DIDs, Azurite blob storage service and also will seed initial required data using a [postman collection](../deployment/data/MVD.postman_collection.json).

- `newman` docker container will automatically stop after seeding initial data from postman scripts.

- EDC Connectors needs to be registered using Registration Service CLI client jar. This client jar must be available under `RegistrationService-Root/client-cli/build/libs` folder.

    ```bash
    export REGISTRATION_SERVICE_CLI_JAR_PATH=registration service client jar path
    system-tests/resources/register-participants.sh
    ```

- Run MVD system tests, and for that environment variable `LOCAL_BLOB_TRANSFER_TEST` must be set to `true` to enable local blob transfer test.

    ```bash
    export LOCAL_BLOB_TRANSFER_TEST=true
    ./gradlew :system-tests:test
    ```

#### Local test resources

### Debugging MVD locally

Follow the instructions in the previous sections to run an MVD with a consumer and provider locally using docker-compose.

Once running, you can use a Java debugger to connect to the consumer (port 5006) and provider (port 5005) instances. If you are using IntelliJ you can use the provided "EDC consumer" or "EDC provider" [runtime configurations](../.run) to remote debug the connector instances.

### Issuing requests manually with Postman

A [postman collection](../deployment/data/MVD.postman_collection.json) can be used to issue requests to an MVD instance of your choice. You will need to adapt the environment variables accordingly to match your target MVD instance.