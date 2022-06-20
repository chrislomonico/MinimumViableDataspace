# Participants onboarding in MVD dataspace

This document presents the infrastructure steps for the scenario to onboard participants in the dataspace.

## Precondition - GAIA-X membership

To join the MVD dataspace, participants must have Verifiable Credentials signed by GAIA-X Authority which prove GAIA-X membership.

## Onboarding process - GAIA-X membership verification

During the onboarding process the GAIA-X membership claim is being verified by the Dataspace Authority.
The onboarding flow is presented in the [distributed authorisation sub-flow doc](https://github.com/agera-edc/MinimumViableDataspace/tree/feature/20-rs-adr-target/docs/developer/decision-records/2022-06-16-distributed-authorization).
Actors in the onboarding scenario:
- _Participant A_ is a participant that wants to join the MVD dataspace.
- _Participant B_ is the MVD Dataspace Authority (Registration Service and it's `did:web' document).
- _Authority_ is the GAIA-X Authority.

## Infrastructure

To enable above precondition and onboarding scenario in MVD, the following steps need to be implemented in the MVD infrastructure: 

1. The CD workflow will generate the private and public keys for GAIA-X Authority.
2. The CD workflow will deploy the GAIA-X Authority DID document containing the public key.
3. The GAIA-X private key will be made available to download as Github artifact so that it can be used in the participant deployment workflow to generate 
   GAIA-X membership Verifiable Credentials. 
4. IdentityHub CLI will be implemented and made available in the IdentityHub repository. Then it will be used in the CD workflow to populate 
   participant's Identity Hub with GAIA-X membership Verifiable Credentials.
5. The Registration Service will be configured with the GAIA-X DID in the CD workflow.