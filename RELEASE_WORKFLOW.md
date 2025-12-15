# Release Workflow Documentation

This document explains the complete release process for MythicRod, including automated builds, testing, and GitHub Releases.

## Overview

The MythicRod project uses GitHub Actions to automate the entire release process. When you push a version tag, the workflow automatically builds the plugin, runs tests, creates a GitHub Release, and uploads the compiled JAR file.

## Workflow Files

The repository includes several workflow files:

- **`.github/workflows/build.yml`** - Main build and release workflow
- **`.github/workflows/verify-release-workflow.yml`** - Validates the release workflow configuration
- **`.github/workflows/branch-protection.yml`** - Documents branch protection setup

## How It Works

### 1. Continuous Integration (CI)

On every push to `main` or `master` branch, or on pull requests:
- The code is built using Gradle
- Tests are run (if any)
- Build artifacts are uploaded to GitHub Actions (7-day retention)

### 2. Release Creation

When you push a tag starting with `v` (e.g., `v1.0.1`):
- All CI steps are performed
- If the build succeeds, a GitHub Release is created
- The compiled JAR is uploaded to the release
- Release notes are automatically generated from merged pull requests

## Creating a Release

### Step 1: Update Version

Update the version in `gradle.properties`:

```properties
version=1.0.1
```

Commit the change:

```bash
git add gradle.properties
git commit -m "Bump version to 1.0.1"
git push origin master
```

### Step 2: Create and Push Tag

Create a tag matching the version:

```bash
git tag v1.0.1
git push origin v1.0.1
```

### Step 3: Monitor the Workflow

1. Go to: https://github.com/xcutiboo/MythicRod/actions
2. Watch the "Build and Release" workflow run
3. If successful, the release will appear at: https://github.com/xcutiboo/MythicRod/releases

## Workflow Configuration

### Build Job

The build job:
- Runs on Ubuntu latest
- Uses Java 21 (Temurin distribution)
- Validates the Gradle wrapper for security
- Builds the project with `./gradlew build`
- Uploads artifacts (excluding `-plain.jar` and `-sources.jar`)

### Release Job

The release job:
- Only runs when a tag starting with `v` is pushed
- Requires the build job to succeed first
- Downloads the build artifact
- Creates a GitHub Release using `softprops/action-gh-release@v2`
- Uploads all JAR files from `build/libs/`
- Generates release notes from merged PRs

## Testing the Workflow

Before creating an actual release, you can verify the workflow configuration:

```bash
./test-release-workflow.sh
```

This script checks:
- ✅ Workflow file exists and is valid
- ✅ Build configuration is correct
- ✅ Gradle wrapper is present
- ✅ Workflow triggers are configured
- ✅ Release job has correct permissions
- ✅ Artifact paths are correct
- ✅ Java version matches
- ✅ Source code structure is valid
- ✅ Plugin metadata exists

## Manual Workflow Dispatch

You can also manually trigger the workflow:

1. Go to: https://github.com/xcutiboo/MythicRod/actions/workflows/build.yml
2. Click "Run workflow"
3. Select the branch
4. Click "Run workflow"

This is useful for testing without creating tags or pushing commits.

## Troubleshooting

### Build Fails

If the build fails:
1. Check the workflow logs in GitHub Actions
2. Look for compilation errors or test failures
3. Fix the issues locally and push the changes
4. The workflow will run again automatically

### Release Not Created

If a release is not created after pushing a tag:
1. Verify the tag name starts with `v` (e.g., `v1.0.1`)
2. Check if the build job succeeded
3. Look at the release job logs for errors
4. Ensure the workflow has `contents: write` permission

### Artifact Not Uploaded

If the JAR is not in the release:
1. Verify `build/libs/*.jar` exists after building
2. Check that the artifact exclusions are correct
3. Look for file path issues in the logs

### Tag Already Exists

If you need to recreate a release:

```bash
# Delete the tag locally
git tag -d v1.0.1

# Delete the tag remotely
git push origin :refs/tags/v1.0.1

# Delete the release on GitHub (manually via web UI)

# Recreate and push the tag
git tag v1.0.1
git push origin v1.0.1
```

## Best Practices

### Semantic Versioning

Follow semantic versioning (semver) for releases:
- `v1.0.0` - Major release (breaking changes)
- `v1.1.0` - Minor release (new features)
- `v1.1.1` - Patch release (bug fixes)

### Release Notes

The workflow auto-generates release notes from PR titles. Write clear PR titles:
- ✅ "Add biome-specific fishing loot"
- ✅ "Fix NPE in statistics manager"
- ❌ "Update code"
- ❌ "Fix bug"

### Pre-releases

For beta or release candidate versions:
- Tag: `v1.0.0-beta.1`
- The workflow will create a pre-release automatically

### Testing Before Release

1. Test locally: `./gradlew clean build`
2. Push to a branch and create a PR
3. Wait for CI to pass
4. Get code review
5. Merge to master
6. Create and push the release tag

## Security

### Gradle Wrapper Validation

The workflow includes `gradle/actions/wrapper-validation@v4` which:
- Validates the Gradle wrapper JAR checksum
- Ensures the wrapper hasn't been tampered with
- Prevents supply chain attacks

### Permissions

The workflow uses minimal permissions:
- Build job: `contents: read` (read-only)
- Release job: `contents: write` (only for creating releases)

### Branch Protection

Protect the `master` branch to prevent:
- Direct pushes (require PRs)
- Force pushes
- Branch deletion

See [BRANCH_PROTECTION.md](BRANCH_PROTECTION.md) for setup instructions.

## Advanced Configuration

### Custom Release Notes

To customize release notes, modify the release job in `.github/workflows/build.yml`:

```yaml
- name: Create GitHub release
  uses: softprops/action-gh-release@v2
  with:
    files: build/libs/*.jar
    generate_release_notes: true
    body: |
      Custom release notes here
      
      ## What's New
      - Feature 1
      - Feature 2
```

### Notification Integration

Add Discord or Slack notifications:

```yaml
- name: Notify Discord
  if: success()
  uses: sarisia/actions-status-discord@v1
  with:
    webhook: ${{ secrets.DISCORD_WEBHOOK }}
    title: "New Release: ${{ github.ref_name }}"
```

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Gradle Build Tool](https://gradle.org/)
- [Semantic Versioning](https://semver.org/)
- [softprops/action-gh-release](https://github.com/softprops/action-gh-release)
