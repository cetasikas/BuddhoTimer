# New Computer Migration & Setup Guide

This guide has all the commands needed to set up your development environment and clone your repositories on your new computer.

---

## Step 1: Install Git & GitHub CLI

Open **PowerShell** and run:

```powershell
winget source reset --force
winget install Git.Git
winget install GitHub.cli
```

> **Note:** If `winget` gives errors, run this direct PowerShell downloader instead:
> ```powershell
> Invoke-WebRequest -Uri "https://github.com/git-for-windows/git/releases/download/v2.46.0.windows.1/Git-2.46.0-64-bit.exe" -OutFile "$env:TEMP\Git-Installer.exe"
> Invoke-WebRequest -Uri "https://github.com/cli/cli/releases/download/v2.55.0/gh_2.55.0_windows_amd64.msi" -OutFile "$env:TEMP\gh_installer.msi"
> Start-Process -FilePath "$env:TEMP\Git-Installer.exe" -ArgumentList "/VERYSILENT /NORESTART" -Wait
> Start-Process -FilePath "msiexec.exe" -ArgumentList "/i `"$env:TEMP\gh_installer.msi`" /quiet /norestart" -Wait
> Remove-Item "$env:TEMP\Git-Installer.exe", "$env:TEMP\gh_installer.msi" -Force
> ```

**Close and reopen PowerShell** after installation so `git` and `gh` are loaded.

---

## Step 2: Log into Both GitHub Accounts

Run the login command for your first account (**`cetasikas`**):

```powershell
gh auth login
```
- **What account do you want to log into?** `GitHub.com`
- **What is your preferred protocol?** `HTTPS`
- **Authenticate Git with your GitHub credentials?** `Yes`
- **How would you like to authenticate?** `Login with a web browser`
- Press Enter, copy the one-time code shown, and sign into **`cetasikas`** in the browser.

Now run the login command again for your second account (**`hackysackapp`**):

```powershell
gh auth login
```
- Select the exact same options (GitHub.com -> HTTPS -> Yes -> Web browser).
- When the browser opens, sign in as **`hackysackapp`** and authorize.

**Verify both accounts are connected:**
```powershell
gh auth status
```

---

## Step 3: Create the Portfolio Folder and Clone Repositories

Run these commands in PowerShell:

```powershell
cd ~
mkdir Portfolio
cd Portfolio

# --- Clone cetasikas repos ---
gh auth switch --user cetasikas
git clone https://github.com/cetasikas/BuddhoTimer.git
git clone https://github.com/cetasikas/buddho-timer.git
git clone https://github.com/cetasikas/Portfolio.git "Chicago Maps"
git clone https://github.com/cetasikas/fraud-detection-pipeline.git

# --- Clone hackysackapp repos ---
gh auth switch --user hackysackapp
git clone https://github.com/hackysackapp/HackySack.git
git clone https://github.com/hackysackapp/HackySack-Releases.git
```

---

## Step 4: Configure Commit Authorship Per Repo

Run this in PowerShell to ensure future commits are tagged with the correct username and noreply email:

```powershell
# Set author for cetasikas repos
@("BuddhoTimer", "buddho-timer", "Chicago Maps", "fraud-detection-pipeline") | ForEach-Object {
    git -C "$HOME\Portfolio\$_" config user.name "cetasikas"
    git -C "$HOME\Portfolio\$_" config user.email "1184735+cetasikas@users.noreply.github.com"
}

# Set author for hackysack repos
@("HackySack", "HackySack-Releases") | ForEach-Object {
    git -C "$HOME\Portfolio\$_" config user.name "hackysackapp"
    git -C "$HOME\Portfolio\$_" config user.email "hackysackapp@users.noreply.github.com"
}
```

---

## Quick Reference: Switching Accounts When Pushing

- Before pushing changes to **BuddhoTimer**, **buddho-timer**, **Chicago Maps**, or **fraud-detection-pipeline**:
  ```powershell
  gh auth switch --user cetasikas
  ```

- Before pushing changes to **HackySack** or **HackySack-Releases**:
  ```powershell
  gh auth switch --user hackysackapp
  ```

---

## Keys & Local Secrets Notes
- **BuddhoTimer Keystore**: The Android signing key (`app/keystore/release.keystore`) and passwords in `build.gradle.kts` are already committed inside the repo. No manual key transfer needed.
- **HackySack Secrets**: Release signing keys live in GitHub Actions Secrets, not locally. When running HackySack, simply paste your LLM API keys into the app's settings dialog.
