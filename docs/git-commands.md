# Git And GitHub Commands

Run from `D:\office-work\Empwr\Code\pearl-payroll-platform`.

```powershell
git init
git branch -M main
git remote add origin https://github.com/<github-organization>/pearl-payroll-platform.git
git add .
git commit -m "Initial PEARL payroll platform monorepo"
git push -u origin main
```

Alternative GitHub CLI setup:

```powershell
gh auth login
gh repo create pearl-payroll-platform --private --source . --remote origin --push
```

Branch bootstrap:

```powershell
git checkout -b develop
git push -u origin develop
git checkout main
```

Protect `main` and `develop` in GitHub with required pull request reviews, CI status checks, signed commits where required, and restricted direct pushes.
