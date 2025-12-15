# Branch Protection Setup Guide

This document explains how to configure branch protection for the MythicRod repository to prevent accidental force pushes, deletions, and ensure code quality through required reviews and status checks.

## Why Branch Protection?

Branch protection rules help maintain code quality and prevent accidental changes to important branches. For the `master` branch, we want to:

- ✅ Prevent force pushes that could rewrite history
- ✅ Prevent accidental deletion of the branch
- ✅ Require pull requests for all changes
- ✅ Require code reviews before merging
- ✅ Ensure CI/CD checks pass before merging
- ✅ Require all conversations to be resolved

## Setup Instructions

### Option 1: Using GitHub Web Interface (Recommended)

1. **Navigate to Branch Settings**
   - Go to: https://github.com/xcutiboo/MythicRod/settings/branches
   - Or: Repository → Settings → Branches

2. **Add Branch Protection Rule**
   - Click "Add branch protection rule"
   - Enter branch name pattern: `master`

3. **Configure Protection Settings**

   **Require a pull request before merging**
   - ☑️ Enable this option
   - Required number of approvals: `1`
   - ☑️ Dismiss stale pull request approvals when new commits are pushed

   **Require status checks to pass before merging**
   - ☑️ Enable this option
   - ☑️ Require branches to be up to date before merging
   - Add required status checks:
     - `build` (from the "Build and Release" workflow)

   **Other Settings**
   - ☑️ Require conversation resolution before merging
   - ☑️ Do not allow bypassing the above settings
   - ☑️ Block force pushes
   - ☑️ Do not allow deletions

4. **Save Changes**
   - Click "Create" or "Save changes"

### Option 2: Using GitHub CLI

If you have the GitHub CLI (`gh`) installed and authenticated with admin permissions:

```bash
gh api repos/xcutiboo/MythicRod/branches/master/protection \
  -X PUT \
  --input - << 'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["build"]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "required_approving_review_count": 1,
    "dismiss_stale_reviews": true
  },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true
}
JSON
```

### Option 3: Using GitHub API

With a Personal Access Token (PAT) that has `repo` scope:

```bash
curl -X PUT \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer YOUR_PAT_TOKEN" \
  https://api.github.com/repos/xcutiboo/MythicRod/branches/master/protection \
  -d '{
    "required_status_checks": {
      "strict": true,
      "contexts": ["build"]
    },
    "enforce_admins": true,
    "required_pull_request_reviews": {
      "required_approving_review_count": 1,
      "dismiss_stale_reviews": true
    },
    "restrictions": null,
    "allow_force_pushes": false,
    "allow_deletions": false,
    "required_conversation_resolution": true
  }'
```

## Verification

After setting up branch protection, verify it's working:

1. Try to push directly to master - it should be blocked
2. Try to force push - it should be blocked
3. Create a PR and verify:
   - The "build" status check must pass
   - At least 1 approval is required
   - All conversations must be resolved

## Workflow Impact

With branch protection enabled:

### For Contributors
- All changes must go through pull requests
- PRs must pass CI/CD checks (build succeeds)
- PRs must be reviewed and approved
- All PR comments must be resolved

### For Releases
- Tags can still be pushed directly (releases are not affected)
- The release workflow will continue to work as expected

## Troubleshooting

**"I can't push to master anymore"**
- This is expected! Create a feature branch and open a pull request

**"I need to bypass protection for an emergency fix"**
- Repository admins can temporarily disable protection
- Or admins can use "Allow specified actors to bypass required pull requests"

**"The build status check is not showing up"**
- Make sure the "Build and Release" workflow has run at least once
- The status check name must match exactly: `build`

## Additional Resources

- [GitHub Docs: About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/about-protected-branches)
- [GitHub Docs: Managing branch protection rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/defining-the-mergeability-of-pull-requests/managing-a-branch-protection-rule)
