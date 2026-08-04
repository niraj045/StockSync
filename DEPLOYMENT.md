# StockSync Production Deployment Guide

This document explains exactly how to safely build, package, and deploy the StockSync application to the production server.

## ⚠️ Important Warning about Git on the Server
The `/opt/stocksync` directory on the production server is **NOT** a Git repository. It is a raw extraction directory.
* **NEVER** run `git pull` on the server! It will fail with `fatal: not a git repository`.
* If you try to update the server by running `git pull`, the server files will **not** update, which will cause Docker to use old cached layers instead of your new code.
* All deployments must happen by uploading a `.tar.gz` package from your local machine.

---

## 1. How to Deploy (Step-by-Step)

### Step 1: Package and Upload from your Local Computer
Depending on your local operating system, run the deployment script to compress your source code and SCP it to the server.

**For Linux / Mac:**
```bash
bash ./deployment/scripts/upload-from-linux.sh
```

**For Windows (PowerShell):**
```powershell
.\deployment\scripts\upload-from-windows.ps1
```

> [!IMPORTANT]
> When the script finishes, it will print out the exact `tar -xzf ...` command you need to run on the server. **Copy the exact filename it prints (e.g., `stocksync-deployment-20260804-203427.tar.gz`)!**

### Step 2: Extract and Restart on the Server
SSH into the server and run the following commands, substituting the exact `.tar.gz` filename you got from Step 1.

```bash
# 1. SSH into the server
ssh stocksync@66.116.253.40

# 2. Go to the deployment directory
cd /opt/stocksync

# 3. Extract the NEW file you just uploaded
tar -xzf stocksync-deployment-YYYYMMDD-HHMMSS.tar.gz

# 4. Make sure scripts are executable
chmod +x deployment/scripts/*.sh backend/mvnw

# 5. Build and restart the Docker containers
./deployment/scripts/start.sh
```

---

## 2. Server & Environment Information
* **Server IP:** `66.116.253.40`
* **SSH User:** `stocksync`
* **App Directory:** `/opt/stocksync`
* **Public URL:** `https://stocksync.caretakers.ind.in`
* **Database Port:** `33060` (Mapped from `3306` inside Docker)
* **Backend API:** Spring Boot 3.5.3 (Java 21)
* **Frontend:** React (Vite)

## 3. Common Troubleshooting

### 1. "Docker says CACHED but I changed the code!"
* **Cause:** You extracted the *wrong (old)* `.tar.gz` file on the server. If the file contents didn't change, Docker uses the cached layers.
* **Fix:** Look in `/opt/stocksync` with `ls -la` and make sure you run `tar -xzf` on the most recently created `.tar.gz` file.

### 2. "Failed to generate PDF from DOCX template" (LibreOffice Error)
* **Cause:** When running LibreOffice headlessly inside a Docker container, it attempts to write user configurations to system-level folders. If it lacks permissions, it silently crashes.
* **Fix:** Our codebase explicitly fixes this by assigning a dynamic `-env:UserInstallation` path pointing to `/tmp/lo_profile_...`. 
* **Logging:** The `convertDocxToPdf` process in `SteelFabChallanTemplateStamper.java` now correctly redirects and attaches the LibreOffice error output directly to the exception message so it shows up in the API response.

### 3. Missing `.env` Files
* If you run `./start.sh` and it fails because of missing environment files, you need to copy them:
```bash
cp deployment/.env.example deployment/.env
nano deployment/.env
```
