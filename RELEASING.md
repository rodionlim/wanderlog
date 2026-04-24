# Releasing

Workflow file: `.github/workflows/android.yml`

## How the pipeline works

| Trigger             | What runs                                                     |
| ------------------- | ------------------------------------------------------------- |
| Push / PR to `main` | Unit tests + debug APK build (artifact kept 7 days)           |
| `v*.*.*` tag push   | Release APK build → optional signing → GitHub Release created |

## Creating a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Release notes are auto-generated from commits since the previous tag.

## Signing setup (one-time)

Without signing secrets the pipeline publishes an **unsigned APK** — installable via sideload but not suitable for the Play Store.

### 1. Generate a keystore

```bash
keytool -genkey -v -keystore wanderlog.jks \
  -alias wanderlog -keyalg RSA -keysize 2048 -validity 10000
```

Keep `wanderlog.jks` somewhere safe — **never commit it**.

### 2. Add secrets to GitHub

Go to **repo → Settings → Secrets and variables → Actions** and add:

| Secret              | Value                                 |
| ------------------- | ------------------------------------- |
| `KEYSTORE_BASE64`   | `base64 -w0 wanderlog.jks`            |
| `KEYSTORE_PASSWORD` | keystore password                     |
| `KEY_ALIAS`         | alias chosen above (e.g. `wanderlog`) |
| `KEY_PASSWORD`      | key password                          |
| `MAPS_API_KEY`      | Google Maps API key (can be blank)    |

Once set, the next tag push will produce a signed `wanderlog-v<tag>.apk` in the GitHub Release.

## Multiple GitHub accounts

Per-path credential storage lets different accounts coexist:

```bash
git config --global credential.useHttpPath true
# push once with token embedded to register credentials
git push https://<username>:<token>@github.com/<owner>/<repo>.git HEAD:main
```

Subsequent pushes via `git push origin main` will use the cached token.
