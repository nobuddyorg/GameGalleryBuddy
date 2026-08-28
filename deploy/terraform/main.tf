# Always-Free footprint:
#   - 1x e2-micro instance in us-west1 / us-central1 / us-east1
#   - <=30GB pd-standard boot disk
#   - 1x static external IP (free while attached to a running instance)
# Nothing here provisions a load balancer, Cloud NAT, or any billed extras.
# Shares the GCP project (and its CI service account/IAM) with
# RideMergeBuddy's now-torn-down deployment, freed up for reuse here.

resource "google_compute_address" "static_ip" {
  name   = "${var.name}-ip"
  region = var.region
}

resource "google_compute_firewall" "allow_web" {
  name    = "${var.name}-allow-web"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["80", "443"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = [var.name]
}

resource "google_compute_firewall" "allow_ssh" {
  name    = "${var.name}-allow-ssh"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = var.ssh_source_ranges
  target_tags   = [var.name]
}

resource "google_compute_instance" "app" {
  name         = var.name
  machine_type = "e2-micro"
  zone         = var.zone
  tags         = [var.name]

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = 30
      type  = "pd-standard"
    }
  }

  network_interface {
    network = "default"
    access_config {
      nat_ip = google_compute_address.static_ip.address
    }
  }

  metadata = {
    ssh-keys = "${var.ssh_user}:${file(var.ssh_public_key_path)}"
  }

  labels = {
    app = var.name
  }
}

locals {
  app_domain = "${var.subdomain}.${var.dns_zone_name}"
}

data "cloudflare_zone" "root" {
  name = var.dns_zone_name
}

resource "cloudflare_record" "app" {
  zone_id = data.cloudflare_zone.root.id
  name    = var.subdomain
  type    = "A"
  value   = google_compute_address.static_ip.address
  ttl     = 300
  # Caddy issues its own Let's Encrypt cert and needs to see real client
  # IPs/ACME challenges directly - keep this record un-proxied (grey cloud).
  proxied = false
}
