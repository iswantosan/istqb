# Project Latihan Day 3: Security dan Technical Review

Project ini berisi contoh kode Java untuk latihan Day 3:
- **Security Testing** (ISO 25010 Security sub-characteristic) — package `com.training.security`
- **Code Review via GitHub PR** — package `com.training.review`

## Struktur Folder

```
src/
  com/training/
    security/
      ApiSaldoServerWithAuth.java   service inquiry saldo dengan Authorization dan audit log
    review/
      SaldoUtil.java                class berisi 7 method untuk latihan Code Review
      Nasabah.java                  helper class (model)
```

## Setup di IntelliJ

1. **File > Open**, pilih folder `code-day3/`
2. Trust Project
3. Set SDK ke JDK 17 atau lebih tinggi

## Menjalankan Aplikasi

Klik gutter icon di `ApiSaldoServerWithAuth.main()`, pilih Run. Server start di `http://localhost:8088`.

## Kontrak API Keamanan

**Endpoint**: `GET /saldo?akun=<nomor>`

**Header wajib**:
```
Authorization: Bearer TOKEN-CUST-<N>
```

**Aturan akses**:
- Setiap token terikat ke akun miliknya
- Token `TOKEN-CUST-42` hanya boleh akses `AKUN-42`
- Token tidak boleh akses akun customer lain (cross-tenant)

**Status response**:
- `200 OK` — token valid, akses akun sendiri
- `401 Unauthorized` — token hilang atau format tidak valid
- `403 Forbidden` — token valid tapi akses bukan akun miliknya
- `404 Not Found` — akun tidak ada di sistem
- `400 Bad Request` — parameter akun hilang

**Audit log**:
Setiap request (sukses atau gagal) dicatat ke console dengan format:
```
[HH:mm:ss.SSS] [AUDIT] reqId=N token=TOKEN-CUST-X akun=AKUN-Y status=200/401/403 hasil=ALLOW/DENY_*
```

## Contoh Request dari Postman atau curl

**Sukses (token valid akses akun sendiri)**:
```
curl -H "Authorization: Bearer TOKEN-CUST-42" \
  "http://localhost:8088/saldo?akun=AKUN-42"
```

**Tanpa header**:
```
curl "http://localhost:8088/saldo?akun=AKUN-42"
```

**Token tidak valid**:
```
curl -H "Authorization: Bearer INVALID-TOKEN" \
  "http://localhost:8088/saldo?akun=AKUN-42"
```

**Cross-tenant access (token Cust-42 coba akses akun lain)**:
```
curl -H "Authorization: Bearer TOKEN-CUST-42" \
  "http://localhost:8088/saldo?akun=AKUN-99"
```

---

## Setup GitHub untuk R-02 Code Review (dilakukan trainer sebelum Day 3)

Latihan R-02 menggunakan GitHub Pull Request untuk code review. Trainer perlu push project ini ke repo GitHub `iswantosan/istqb` dengan 2 branch: `main` (baseline bersih) dan `feature/saldo-util-v1` (berisi SaldoUtil.java untuk di-review).

### Langkah 1: Buat Repo di GitHub

1. Login ke `https://github.com/iswantosan`
2. Buat repo baru dengan nama `istqb`
3. Set repo **public** (atau private, tapi perlu invite 7 peserta sebagai collaborator)
4. Jangan centang "Initialize with README" (karena kita push dari local)

### Langkah 2: Init Git di Local dan Push Main

```powershell
cd d:\Training\istqb\code-day3

git init
git add .
git commit -m "Initial commit: ApiSaldoServerWithAuth baseline"

git branch -M main
git remote add origin https://github.com/iswantosan/istqb.git
git push -u origin main
```

### Langkah 3: Buat Branch Feature dan Push

**Catatan**: Branch `feature/saldo-util-v1` isinya mirip `main`. Yang bedakan: di `main` file `SaldoUtil.java` dan `Nasabah.java` **belum ada**; di `feature/saldo-util-v1` keduanya baru di-add.

Supaya ini kejadian secara git history, langkahnya:

```powershell
# Masih di d:\Training\istqb\code-day3

# Backup file review
mkdir ..\_backup_review
copy src\com\training\review\SaldoUtil.java ..\_backup_review\
copy src\com\training\review\Nasabah.java ..\_backup_review\

# Hapus file review dari main, commit ulang clean
del src\com\training\review\SaldoUtil.java
del src\com\training\review\Nasabah.java
git add -A
git commit -m "chore: remove review files from main baseline"
git push origin main

# Buat branch feature, re-add file review
git checkout -b feature/saldo-util-v1
copy ..\_backup_review\SaldoUtil.java src\com\training\review\
copy ..\_backup_review\Nasabah.java src\com\training\review\
git add src\com\training\review\
git commit -m "feat: add SaldoUtil for saldo-related utility operations"
git push -u origin feature/saldo-util-v1

# Cleanup backup
rmdir /s /q ..\_backup_review
```

### Langkah 4: Buat Pull Request

1. Buka `https://github.com/iswantosan/istqb`
2. GitHub akan show banner "feature/saldo-util-v1 had recent pushes" → klik **Compare & pull request**
3. PR title: `Add SaldoUtil for saldo-related utility operations`
4. PR description (contoh):
   ```
   Add new utility class SaldoUtil.java with 7 methods for saldo-related operations:
   - formatNamaNasabah
   - bacaBarisPertama
   - hitungTotalSaldo
   - buatQueryTransfer
   - isStatusAktif
   - rataRataSaldo
   - hitungBunga

   Ready for review.
   ```
5. Klik **Create pull request**
6. Copy URL PR, share ke peserta saat sesi R-02 mulai

### Langkah 5: Invite Peserta (Kalau Repo Private)

1. Settings → Collaborators → Add people
2. Masukkan GitHub username atau email 7 peserta
3. Role: **Triage** (cukup untuk review, tidak bisa merge)
4. Peserta accept invitation via email

### Langkah 6: Verifikasi

Buka PR URL di browser incognito (tanpa login) untuk cek:
- Kalau public: PR terlihat tanpa login ✓
- Kalau private: muncul "You need to sign in" ✓

Kalau repo private, pastikan semua 7 peserta sudah accept invitation sebelum sesi mulai.

### Reset untuk Training Batch Berikutnya

Kalau mau training batch berikutnya pakai PR yang fresh:

```powershell
# Hapus PR lama (via GitHub UI: Close PR)
# Hapus branch feature lama
git push origin --delete feature/saldo-util-v1

# Buat branch baru untuk batch berikutnya
git checkout main
git pull origin main
git checkout -b feature/saldo-util-v2
# Re-add SaldoUtil.java + Nasabah.java, commit, push, create PR baru
```
