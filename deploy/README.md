# Hosting: GCP free tier

A single `e2-micro` Compute Engine VM (GCP Always Free tier, no time limit),
provisioned by Terraform and configured by Ansible, both run from
`.github/workflows/deploy.yml`. Fronted by Caddy for automatic HTTPS, running
the app's existing Docker image (built the same way `Dockerfile` already
builds it for local/CI use - `deploy/build.sh` just wraps that build and
saves the image as a tarball Ansible copies to the VM and `docker load`s).
No load balancer, no registry, no billed extras beyond the VM itself.

Unlike RideMergeBuddy (separate GitHub Pages frontend + GCP backend), this
app serves its whole UI + API from one Spring Boot process, so it's a single
origin end to end: `https://gamegallerybuddy.nobuddy.org`.

This reuses the same GCP project, GCS state bucket, and CI service account
that RideMergeBuddy's now-torn-down deployment used - `destroy-infra.yml`
over there deliberately preserved all three for exactly this. Terraform
state lives in the same bucket under a different prefix
(`gamegallerybuddy` vs `activitymerger`), so the two apps' state never
collides even though they share a bucket and project.

## One-time setup (before the first CI run)

If the bucket, service account, and Cloudflare token from RideMergeBuddy's
setup are still around (they should be - `destroy-infra.yml` preserved
them), skip straight to **GitHub configuration** below and reuse those same
values. Otherwise, follow RideMergeBuddy's `deploy/README.md` steps 1-3 and
5 to create them from scratch, then come back here.

1. In the repo's Settings → Secrets and variables → Actions, add a
   `production` environment (auto-created on first `deploy.yml` run if you
   skip this - only needed up front if you want required reviewers on it).

## GitHub configuration

Under Settings → Secrets and variables → Actions:

**Secrets** (sensitive):

| Secret                 | Value                                                              |
|------------------------|---------------------------------------------------------------------|
| `GCP_SA_KEY`           | Same CI service account key JSON as RideMergeBuddy used             |
| `GCP_SSH_PRIVATE_KEY`  | An SSH private key for CI to provision/connect with (can be the same one RideMergeBuddy used, or a new one) |
| `CLOUDFLARE_API_TOKEN` | Same Cloudflare token as RideMergeBuddy used (Zone:DNS:Edit + Zone:Zone:Read on `nobuddy.org`) |
| `BGG_API_TOKEN`        | Already used by `test.yml` - the same token works here              |

**Variables** (not sensitive):

| Variable          | Value                                                        |
|-------------------|-----------------------------------------------------------------|
| `TF_STATE_BUCKET`  | Same GCS bucket RideMergeBuddy used, e.g. `your-project-id-tfstate` |
| `GCP_PROJECT_ID`   | Same GCP project ID RideMergeBuddy used                      |
| `GCP_SSH_USER`     | Username to create on the VM, e.g. `deploy`                  |

No DNS step needed - Terraform creates the A record (Cloudflare) itself as
part of `terraform apply`, pointed at the static IP it just reserved, and
Caddy issues its own cert automatically once that resolves.

## Deploying

Once the above is in place:

- Push to `main` touching `src/**`, `build.gradle`, `Dockerfile`, or
  `deploy/**` → `deploy.yml` runs `terraform apply` (updates the VM/firewall
  if you changed them, no-ops otherwise), builds the Docker image, and rolls
  it out via the Ansible playbook.
- Can also be run manually from the Actions tab ("Run workflow").

`deploy.yml` targets the GitHub Environment named `production` - add
required reviewers there if you want a manual approval gate before
Terraform/Ansible actually run, since this workflow applies infrastructure
changes unattended otherwise.

## Running it locally instead

If you'd rather provision/deploy from your own machine:

```
cd deploy/terraform
cp backend.hcl.example backend.hcl        # fill in your bucket name
cp terraform.tfvars.example terraform.tfvars  # fill in project_id, ssh_user, ssh_public_key_path
terraform init -backend-config=backend.hcl
terraform apply
```

```
./deploy/build.sh   # requires Docker locally, same as run.sh option 1

cd deploy/ansible
cp inventory.ini.example inventory.ini   # fill in ansible_host (terraform's external_ip output), ansible_user
ansible-playbook -i inventory.ini playbook.yml \
  -e "domain_name=$(cd ../terraform && terraform output -raw app_domain)" \
  -e "bgg_api_token=$BGG_API_TOKEN"
```

Local and CI runs share the same Terraform state (same GCS bucket), so
either can safely be used interchangeably.
