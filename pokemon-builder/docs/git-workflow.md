# Git workflow

Git here is for rollback and for showing agents the development flow.

| Branch | Role |
|---|---|
| `main` | Stable baseline. Normally not touched. |
| `develop` | Daily integration branch. Finished feature branches are merged here. |
| `release` | Versions ready to ship. Merge `develop` into it when it is time for a release build. |
| `feature/<name>` | One per new feature. Branch from `develop`, merge back into `develop` when done, then delete. |

Rules for agents:
- Never commit directly to `main` or `release`.
- Start new work with `git switch -c feature/<name> develop`.
- Commit by topic (battle, event, field, ui, builder, docs) with `feat(scope): ...` / `fix(scope): ...` / `chore(scope): ...` messages.
