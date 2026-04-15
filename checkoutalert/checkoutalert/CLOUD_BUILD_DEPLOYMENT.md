# GCP Cloud Build Deployment Guide - Checkout Alert

## 🚀 Deploy using Cloud Build (No Docker Needed!)

This guide will deploy your Spring Boot app using **Cloud Build**, which:
- ✅ Builds Docker image automatically
- ✅ Pushes to Google Container Registry
- ✅ Deploys to Cloud Run
- ✅ All automated from GitHub push!
- ✅ Works within free tier ($300 credits)

---

## Step 1: Create GCP Project

```powershell
# Set your project details
$PROJECT_ID = "checkout-alert-prod"
$REGION = "us-central1"

# Create project
gcloud projects create $PROJECT_ID --name="Checkout Alert"

# Set as default
gcloud config set project $PROJECT_ID

# Set your billing account (IMPORTANT!)
gcloud config get-value billing/quota_project
# OR set billing account:
# gcloud billing projects link $PROJECT_ID --billing-account=BILLING_ACCOUNT_ID
```

---

## Step 2: Enable Required APIs

```powershell
$PROJECT_ID = "checkout-alert-prod"

# Enable APIs
gcloud services enable cloudbuild.googleapis.com `
  --project=$PROJECT_ID

gcloud services enable run.googleapis.com `
  --project=$PROJECT_ID

gcloud services enable containerregistry.googleapis.com `
  --project=$PROJECT_ID

gcloud services enable servicemanagement.googleapis.com `
  --project=$PROJECT_ID

gcloud services enable iam.googleapis.com `
  --project=$PROJECT_ID

# Verify
gcloud services list --enabled --project=$PROJECT_ID
```

---

## Step 3: Connect GitHub Repository

### Option A: Using Cloud Console (Recommended)

1. Go to: https://console.cloud.google.com/cloud-build/repositories
2. Click **Connect Repository**
3. Select **GitHub** → Authenticate with GitHub account
4. Select your repository: `SakShamJain8/Checkout-Alert-Backend`
5. Click **Connect**

### Option B: Using gcloud CLI

```powershell
# Install gcloud beta components
gcloud components install beta

# Create a connection
gcloud beta builds worker-pools create github-pool `
  --project=$PROJECT_ID
```

---

## Step 4: Create Cloud Build Trigger

### Using Cloud Console:

1. Go to: https://console.cloud.google.com/cloud-build/triggers
2. Click **Create Trigger**
3. Fill in:
   - **Name**: `checkout-alert-deploy`
   - **Event**: Select `Push to a branch`
   - **Repository**: `Checkout-Alert-Backend`
   - **Branch pattern**: `main` (or `^main$`)
   - **Configuration file**: `checkoutalert/checkoutalert/cloudbuild.yaml`
4. Click **Create**

### Advanced Settings (Optional):
- **Substitutions**: Click **Show and edit substituted variables**
- Add your secrets here (more secure than in cloudbuild.yaml)

---

## Step 5: Set Environment Variables

You have two options:

### Option A: Set in Cloud Build Trigger (More Secure ⭐)

1. Go to Cloud Build Triggers
2. Select `checkout-alert-deploy`
3. Click **Edit**
4. Scroll to **Substitution variables**
5. Add these variables:
   ```
   _JWT_SECRET = your-jwt-secret-value
   _ADMIN_SECRET = your-admin-secret-value
   _GEMINI_API_KEY = your-gemini-api-key
   _MAIL_HOST = smtp.gmail.com
   _MAIL_PORT = 587
   _MAIL_USERNAME = your-email@gmail.com
   _MAIL_PASSWORD = your-app-password
   ```
6. Click **Save**

### Option B: Update cloudbuild.yaml directly
Edit the `substitutions` section with your actual values.

**Note**: If you use Gmail, generate an [App Password here](https://myaccount.google.com/apppasswords)

---

## Step 6: Deploy 🚀

### Automatic (Recommended):
Just push to your `main` branch:
```bash
git add .
git commit -m "Deploy to GCP"
git push origin main
```

Cloud Build will automatically:
- ✅ Trigger from your push
- ✅ Build Docker image
- ✅ Push to Google Container Registry
- ✅ Deploy to Cloud Run
- ✅ Update your live service

### Manual Trigger:

```powershell
gcloud builds submit `
  --config=checkoutalert/checkoutalert/cloudbuild.yaml `
  --substitutions=_JWT_SECRET="your-secret",_ADMIN_SECRET="your-admin",_GEMINI_API_KEY="your-key",_MAIL_USERNAME="your-email@gmail.com",_MAIL_PASSWORD="your-password" `
  --project=$PROJECT_ID
```

---

## Step 7: Monitor Deployment

```powershell
# Watch build progress
gcloud builds log LATEST --stream --project=$PROJECT_ID

# List all builds
gcloud builds list --project=$PROJECT_ID

# View specific build
gcloud builds log BUILD_ID --project=$PROJECT_ID

# View Cloud Run service
gcloud run services describe checkoutalert --region=us-central1 --project=$PROJECT_ID

# Get service URL
gcloud run services list --region=us-central1 --project=$PROJECT_ID
```

---

## Step 8: Check Deployment in Console

1. **Cloud Build Logs**: https://console.cloud.google.com/cloud-build/builds
2. **Cloud Run Services**: https://console.cloud.google.com/run
3. **Container Registry**: https://console.cloud.google.com/gcr/images

---

## 💰 Cost Breakdown (with $300 Credits)

| Service | Free Tier | Cost |
|---------|-----------|------|
| **Cloud Build** | 120 minutes/day | $0.01/min after |
| **Cloud Run** | 2M requests/month, 360k GB-sec | FREE (mostly) |
| **Container Registry** | 500 MB/month | FREE (mostly) |
| **Total** | | ~$0-5/month |

**Estimated credits usage: $5-50 for entire $300 budget!**

---

## 📝 cloudbuild.yaml Breakdown

Your updated `cloudbuild.yaml` does:

1. **Build Step**: Creates Docker image from `Dockerfile`
   - Tags with `:latest` and `:$SHORT_SHA` (commit hash)
   - Uses working dir: `checkoutalert/checkoutalert/`

2. **Push Steps**: Pushes both tags to Google Container Registry
   - `gcr.io/$PROJECT_ID/checkoutalert:latest`
   - `gcr.io/$PROJECT_ID/checkoutalert:$SHORT_SHA`

3. **Deploy Step**: Deploys to Cloud Run with:
   - Environment variables (from substitutions)
   - Resource limits: 1 CPU, 512MB RAM
   - Timeout: 1 hour
   - Max instances: 100
   - Publicly accessible

---

## 🐛 Troubleshooting

### Issue: Build fails with "Permission denied"
**Solution**: Ensure Cloud Build service account has permissions:
```powershell
$PROJECT_NUMBER = (gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
gcloud projects add-iam-policy-binding $PROJECT_ID `
  --member="serviceAccount:$PROJECT_NUMBER@cloudbuild.gserviceaccount.com" `
  --role="roles/run.admin"
```

### Issue: Cloud Run deployment fails
```powershell
# Check build logs
gcloud builds log LATEST --stream --project=$PROJECT_ID

# Check Cloud Run logs
gcloud run services logs read checkoutalert --region=us-central1 --limit=50
```

### Issue: "cloudbuild.yaml not found"
Make sure your trigger's "Configuration file location" points to:
```
checkoutalert/checkoutalert/cloudbuild.yaml
```

### Issue: Environment variables not set
- Check substitutions in trigger settings
- Verify variable names (must start with `_`)
- Ensure values don't contain special characters that need escaping

---

## ✅ Verify Deployment Success

Once deployed:

```powershell
# 1. Get service URL
$URL = gcloud run services describe checkoutalert `
  --region=us-central1 `
  --format='value(status.url)' `
  --project=$PROJECT_ID

# 2. Test endpoint
curl "$URL/actuator/health"

# 3. View real-time logs
gcloud run services logs read checkoutalert `
  --region=us-central1 `
  --tail `
  --project=$PROJECT_ID
```

Expected response:
```json
{"status":"UP"}
```

---

## 📚 Next Steps

1. ✅ Create GCP project
2. ✅ Enable APIs
3. ✅ Connect GitHub repository
4. ✅ Create Cloud Build trigger
5. ✅ Set environment variables
6. ✅ Push to main branch or trigger manually
7. ✅ Monitor build and deployment
8. ✅ Test your endpoints
9. 🔄 Set up continuous deployment (auto-deploy on push)
10. 📊 Monitor costs in Cloud Console

---

## 🔗 Useful Links

- [Cloud Build Documentation](https://cloud.google.com/build/docs)
- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [cloudbuild.yaml Reference](https://cloud.google.com/build/docs/build-config)
- [GCP Console - Cloud Build](https://console.cloud.google.com/cloud-build)
- [GCP Console - Cloud Run](https://console.cloud.google.com/run)

---

**You're all set! Start with Step 1 and let me know if you hit any issues.** 🚀
