# Participants onboarding in MVD

This document outlines the infrastructure steps for the scenario of onboarding participants to the Minimum Viable Dataspace (MVD).

## Precondition - GAIA-X membership

To join the MVD, participants must have Verifiable Credentials signed by GAIA-X Authority which prove GAIA-X membership.

At present the MVD deployment pipeline does not interact with the real GAIA-X Authority. A simulated GAIA-X Authority is deployed within MVD for demonstration purposes.

## Onboarding process - GAIA-X membership verification

During the onboarding process, the GAIA-X membership claim is verified by the Dataspace Authority.
The onboarding flow is presented in the [distributed authorisation sub-flow document](https://github.com/agera-edc/MinimumViableDataspace/tree/feature/20-rs-adr-target/docs/developer/decision-records/2022-06-16-distributed-authorization).
Actors in the onboarding scenario:

- _Participant A_ is a putative participant that wants to join the MVD.
- _Participant B_ is the Dataspace Authority (Registration Service and its `did:web` document).
- _Authority_ is the simulated GAIA-X Authority.

## Infrastructure

To enable above precondition and onboarding scenario in MVD, the following steps need to be implemented in the MVD infrastructure: 

1. The CD workflow will generate the private and public keys for GAIA-X Authority.
2. The CD workflow will deploy the GAIA-X Authority DID document containing the public key.
3. The GAIA-X private key will be published as Github artifact so that it can be used in the participant deployment workflow to generate GAIA-X membership Verifiable Credentials. 
4. A CLI client for IdentityHub will be developed. It will be used in the MVD deployment workflow to populate the participant's Identity Hub with GAIA-X membership Verifiable Credentials.
5. The Registration Service will be configured with the simulated GAIA-X Authority DID URL in the CD workflow.
6. A CLI client for the Registration Service will be developed. It will be used in the MVD deployment workflow to start the participant onboarding process. The CLI client can also be used locally to onboard additional participants. The CLI client needs access to the participant's DID private key on the local file system.
7. The Registration Service will be configured with a enrollment policy, that requires a GAIA-X membership Verifiable Credential issued by the simulated GAIA-X Authority DID.

