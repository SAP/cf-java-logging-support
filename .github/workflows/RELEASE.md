# Release workflow

`release.yml` publishes to Maven Central (Sonatype Central Portal) via the `central` Maven profile. It runs on a published GitHub Release or a manual `workflow_dispatch`; it doesn't create releases.

## Flow
1. Cut a GitHub Release (or dispatch the workflow, which won't create a github release).
2. A maintainer approves the `maven-central` environment prompt.
3. The job builds, signs, and uploads to the Portal, where it's staged.
4. A maintainer has to Publish the version in the Portal to release to Central. This could be automated if deemed useful (https://central.sonatype.org/publish/publish-portal-maven/#autopublish).

## Secrets
Kept on the `maven-central` GitHub Environment, not repo or org secrets. This is essential for security, if you ever need to set this up yourself (https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments):

- `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_GPG_PRIVATE_KEY`

They only inject after the environment rules pass: review, and allowed refs limited to `main`. Other runs are not able to retrieve the credentials.

The signing key has no passphrase on purpose: gpg then signs non-interactively on the runner. A passphrase would make gpg open a pinentry prompt, which a headless CI can't answer, and the build fails.
