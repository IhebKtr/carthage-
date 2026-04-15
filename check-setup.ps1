# ══════════════════════════════════════════════════════════════
# check-setup.ps1 - Vérifie que tout est prêt pour lancer le projet
# Usage : .\check-setup.ps1
# ══════════════════════════════════════════════════════════════

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host "  🏟️  Carthage Arena - Vérification de l'installation" -ForegroundColor Cyan
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""

$allOk = $true

# ── Java ─────────────────────────────────────────────────────
Write-Host "[ 1/5 ] Java 17+" -NoNewline
try {
    $javaVersion = & java -version 2>&1
    $versionLine = $javaVersion | Select-String "version"
    Write-Host "  ✅  $versionLine" -ForegroundColor Green
} catch {
    Write-Host "  ❌  Java non installé → https://adoptium.net/" -ForegroundColor Red
    $allOk = $false
}

# ── Maven ─────────────────────────────────────────────────────
Write-Host "[ 2/5 ] Maven" -NoNewline
try {
    $mvnVersion = & mvn -version 2>&1 | Select-Object -First 1
    Write-Host "  ✅  $mvnVersion" -ForegroundColor Green
} catch {
    Write-Host "  ❌  Maven non installé → https://maven.apache.org/download.cgi" -ForegroundColor Red
    $allOk = $false
}

# ── MySQL ─────────────────────────────────────────────────────
Write-Host "[ 3/5 ] MySQL (port 3306)" -NoNewline
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect("127.0.0.1", 3306)
    $tcp.Close()
    Write-Host "  ✅  MySQL accessible sur le port 3306" -ForegroundColor Green
} catch {
    Write-Host "  ❌  MySQL non démarré → Lance XAMPP → Start MySQL" -ForegroundColor Red
    $allOk = $false
}

# ── Fichier .env ───────────────────────────────────────────────
Write-Host "[ 4/5 ] Fichier .env" -NoNewline
$envPath = Join-Path $PSScriptRoot ".env"
if (Test-Path $envPath) {
    $envContent = Get-Content $envPath
    $hasGroq    = ($envContent | Select-String "GROQ_API_KEY=(?!REMPLACER)").Matches.Count -gt 0
    $hasStripe  = ($envContent | Select-String "STRIPE_SECRET_KEY=(?!REMPLACER)").Matches.Count -gt 0

    if ($hasGroq -and $hasStripe) {
        Write-Host "  ✅  .env configuré avec les vraies clés" -ForegroundColor Green
    } elseif (-not $hasGroq -and -not $hasStripe) {
        Write-Host "  ⚠️  .env présent mais clés API non configurées" -ForegroundColor Yellow
        Write-Host "          → Éditez D:\projet-java-web\.env" -ForegroundColor Yellow
    } else {
        Write-Host "  ⚠️  .env partiel (certaines clés manquantes)" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌  Fichier .env manquant → Copiez/créez D:\projet-java-web\.env" -ForegroundColor Red
    $allOk = $false
}

# ── pom.xml ──────────────────────────────────────────────────
Write-Host "[ 5/5 ] pom.xml" -NoNewline
$pomPath = Join-Path $PSScriptRoot "pom.xml"
if (Test-Path $pomPath) {
    Write-Host "  ✅  pom.xml trouvé" -ForegroundColor Green
} else {
    Write-Host "  ❌  pom.xml manquant" -ForegroundColor Red
    $allOk = $false
}

# ── Résumé ───────────────────────────────────────────────────
Write-Host ""
Write-Host "=====================================================" -ForegroundColor Cyan
if ($allOk) {
    Write-Host "  ✅  Tout est prêt ! Lance le projet avec :" -ForegroundColor Green
    Write-Host ""
    Write-Host "       mvn clean javafx:run" -ForegroundColor White
    Write-Host ""
    Write-Host "  Ou ouvre IntelliJ IDEA → Run MainApp.java" -ForegroundColor White
} else {
    Write-Host "  ⚠️  Corrige les erreurs ci-dessus avant de lancer" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  Guide complet : guide_lancement.md" -ForegroundColor White
}
Write-Host "=====================================================" -ForegroundColor Cyan
Write-Host ""
