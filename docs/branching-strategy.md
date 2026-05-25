# Branching Strategy

The PEARL platform uses a conservative enterprise Git workflow:

| Branch | Purpose | Merge Target |
| --- | --- | --- |
| `main` | Production-ready release history. Every commit is deployable. | None |
| `develop` | Integrated work for the next release candidate. | `main` through release PR |
| `feature/*` | One feature, fix, or technical task. | `develop` |
| `release/*` | Stabilization, final QA, and version hardening. | `main`, then back-merge to `develop` |
| `hotfix/*` | Urgent production fixes cut from `main`. | `main`, then back-merge to `develop` |

Workflow:

1. Create feature branches from `develop`.
2. Open pull requests into `develop` with CI, review, and security checks.
3. Cut `release/<version>` from `develop` when scope is frozen.
4. Promote release branches through QA, stage, and production.
5. Merge release branches to `main` and tag the release.
6. Back-merge `main` into `develop` after release.
7. Cut `hotfix/<ticket>` from `main` only for production incidents.

Naming examples:

```text
feature/FIN-1234-add-tax-normalization
release/2026.05.0
hotfix/FIN-1299-correct-sqs-redrive-policy
```
