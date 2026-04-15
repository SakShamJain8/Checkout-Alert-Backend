# GCP Cloud Run Deployment Guide - Checkout Alert

## 🚀 Quick Start (Cost-Optimized for $300 Free Credits)

This guide will deploy your Spring Boot app to **Cloud Run** (serverless), which is FREE within the tier limits.

### Prerequisites
- [ ] GCP Account with $300 free credits
- [ ] gcloud CLI installed
- [ ] Docker installed
- [ ] Java 17 JDK

---

## Step 1: Install & Setup gcloud CLI

### Windows Installation:
Download and run: https://cloud.google.com/sdk/docs/install

After installation, verify:
```powershell
gcloud --version
```

### Authenticate with GCP:
```powershell
gcloud auth login
```
This opens your browser to authenticate. Follow the prompts.

---

## Step 2: Create GCP Project (if needed)

```powershell
# Set your project name (use lowercase, hyphens allowed)
$PROJECT_ID = "checkout-alert-prod"

# Create project
gcloud projects create $PROJECT_ID --name="Checkout Alert"

# Set as default
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

---

## Step 3: Build & Push Docker Image

### Create Artifact Registry Repository
```powershell
# Set variables
$REGION = "us-central1"
$REPO_NAME = "checkout-alert-repo"
$PROJECT_ID = "checkout-alert-prod"

# Create repository
gcloud artifacts repositories create $REPO_NAME `
  --repository-format=docker `
  --location=$REGION

# Configure authentication (one-time)
gcloud auth configure-docker "$REGION-docker.pkg.dev"
```

### Build & Push Image
```powershell
# Build image
$IMAGE_URL = "$REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/checkout-alert:latest"
docker build -t $IMAGE_URL .

# Push to Artifact Registry
docker push $IMAGE_URL
```

---

## Step 4: Deploy to Cloud Run

### Deploy with Environment Variables
```powershell
$IMAGE_URL = "us-central1-docker.pkg.dev/checkout-alert-prod/checkout-alert-repo/checkout-alert:latest"
$PROJECT_ID = "checkout-alert-prod"
$REGION = "us-central1"

gcloud run deploy checkout-alert `
  --image=$IMAGE_URL `
  --platform=managed `
  --region=$REGION `
  --allow-unauthenticated `
  --memory=512Mi `
  --cpu=1 `
  --timeout=3600 `
  --max-instances=100 `
  --set-env-vars=`
    "JWT_SECRET=your-jwt-secret-here,`
    `
ADMIN_SECRET=your-admin-secret-here,`
    `
GEMINI_API_KEY=your-gemini-key-here,`
    `
MAIL_HOST=smtp.gmail.com,`
    `
MAIL_PORT=587,`
    `
MAIL_USERNAME=your-email@gmail.com,`
    `
MAIL_PASSWORD=your-app-password-here,`
    `
SPRING_PROFILES_ACTIVE=prod"
```

---

## Step 5: View Your Deployment

```powershell
# Get your service URL
gcloud run services list --region=$REGION

# View logs
gcloud run services logs read checkout-alert --region=$REGION --limit=20

# Open in browser
gcloud run services describe checkout-alert --region=$REGION | Select-String "URL"
```

---

## Step 6: [OPTIONAL] Setup Cloud SQL Database

If you need persistent database storage:

```powershell
# Create a Cloud SQL instance (PostgreSQL)
gcloud sql instances create checkout-alert-db `
  --database-version=POSTGRES_15 `
  --tier=db-f1-micro `
  --region=$REGION `
  --database-flags=cloudsql_iam_authentication=on

# Create database
gcloud sql databases create checkoutalert --instance=checkout-alert-db

# Create IAM user for Cloud Run
gcloud sql users create cloud-run-user --instance=checkout-alert-db --type=CLOUD_IAM_USER
```

Update your app's database connection in Cloud Run environment variables:
```
SPRING_DATASOURCE_URL=jdbc:postgresql://cloudsql:5432/checkoutalert
SPRING_DATASOURCE_USERNAME=cloud-run-user
```

---

## 💰 Cost Breakdown (with $300 Credits)

| Service | Free Tier | Estimated Cost/Month |
|---------|-----------|----------------------|
| **Cloud Run** | 2M requests/month, 360k GB-sec | ~$0 (within tier) |
| **Artifact Registry** | 500 MB/month | ~$0 (within limit) |
| **Cloud SQL** (optional) | 30-day trial | ~$5-15 if enabled |
| **Total** | | ~$0-15 |

**Your $300 credits will last 20-30+ months!**

---

## 🐛 Troubleshooting

### Issue: "Permission denied"
```powershell
# Re-authenticate
gcloud auth application-default login
```

### Issue: Docker image push fails
```powershell
# Reconfigure Docker auth
gcloud auth configure-docker "us-central1-docker.pkg.dev"
```

### Issue: Cloud Run deployment fails
```powershell
# Check logs
gcloud run services logs read checkout-alert --region=us-central1 --limit=50
```

### Issue: Health checks timing out
- Check if `--timeout` is too low
- Verify `/actuator/health` endpoint exists (Spring Boot default)

---

## 📚 Useful Commands

```powershell
# Update deployment
gcloud run deploy checkout-alert --image=$IMAGE_URL --region=$REGION

# Scale instances
gcloud run services update checkout-alert --region=$REGION --max-instances=200

# View metrics
gcloud monitoring time-series list

# Delete service
gcloud run services delete checkout-alert --region=$REGION
```

---

## ✅ Next Steps

1. Prepare your environment variables (update Step 4)
2. Run the deployment commands in order
3. Test your endpoints at the service URL
4. Monitor logs and metrics in Cloud Console
5. [Optional] Set up continuous deployment with Cloud Build
