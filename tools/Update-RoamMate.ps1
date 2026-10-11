param(
    [string]$RepoPath = "C:\Projects\COMP90018"
)

# This installer never discards edits, changes git identity, pushes, or merges a remote branch.
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$packageRoot = $PSScriptRoot
if (-not (Test-Path -LiteralPath (Join-Path $packageRoot "patches"))) {
    throw "Run the copy of Update-RoamMate.ps1 at the root of the extracted update package."
}
if (-not (Test-Path -LiteralPath $RepoPath)) { throw "Repository folder does not exist: $RepoPath" }

Push-Location -LiteralPath $RepoPath
try {
    $inside = & git rev-parse --is-inside-work-tree
    if ($LASTEXITCODE -ne 0 -or $inside -ne "true") { throw "This is not a Git working tree." }
    $branch = (& git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0) { throw "Cannot read current branch." }
    if ($branch -ne "jie-pet" -and $branch -notlike "jie-pet-ui-voice-*") {
        throw "Current branch is '$branch'. Keep your edits safe and switch to jie-pet first."
    }
    $dirty = @(& git status --porcelain)
    if ($LASTEXITCODE -ne 0) { throw "Cannot inspect working tree." }
    if ($dirty.Count -gt 0) {
        $dirty | ForEach-Object { Write-Host $_ }
        throw "Working tree has changes. Commit or stash them first; this installer will not overwrite them."
    }
    if (Test-Path -LiteralPath "docs/PET_V1_1_GUIDE_ZH.md") {
        throw "This version's guide already exists. Review git log; do not apply the update twice."
    }
    $amPath = & git rev-parse --git-path rebase-apply
    if (Test-Path -LiteralPath $amPath) { throw "A previous git am/rebase is still active. Resolve it before updating." }
    $null = & git var GIT_COMMITTER_IDENT
    if ($LASTEXITCODE -ne 0) { throw "Configure git user.name and user.email in this repository first." }

    $selectedPatch = $null
    foreach ($name in @("from-weather.patch", "from-single-koala.patch", "from-pixel.patch")) {
        $candidate = Join-Path $packageRoot ("patches/" + $name)
        if (-not (Test-Path -LiteralPath $candidate)) { throw "Missing package file: $candidate" }
        # Preflight is read-only. Capture expected errors for non-matching older baselines.
        $savedPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $checkOutput = & git apply --check --index -- $candidate 2>&1
        $checkExit = $LASTEXITCODE
        $ErrorActionPreference = $savedPreference
        if ($checkExit -eq 0) {
            $selectedPatch = $candidate
            break
        }
    }
    if ($null -eq $selectedPatch) {
        throw "None of the three known pet versions matches this checkout. No changes made. Share git status and git --no-pager log -5 --oneline for an adapted patch."
    }

    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $backup = "backup-jie-pet-" + $stamp
    $updateBranch = "jie-pet-ui-voice-" + $stamp
    & git branch $backup
    if ($LASTEXITCODE -ne 0) { throw "Could not create backup branch." }
    & git switch -c $updateBranch
    if ($LASTEXITCODE -ne 0) { throw "Could not create update branch. Backup: $backup" }
    & git am -- $selectedPatch
    if ($LASTEXITCODE -ne 0) {
        throw "Patch stopped. Keep the output. To cancel only this import, run git am --abort. Backup: $backup"
    }
    Write-Host "Update applied on $updateBranch"
    Write-Host "Previous version is safe on $backup and $branch"
    & git --no-pager log -1 --oneline
    & git status --short
    Write-Host "Next: Android Studio > Sync Project with Gradle Files"
    Write-Host "Then run: .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest"
    Write-Host "Nothing has been pushed to GitHub."
}
finally {
    Pop-Location
}
