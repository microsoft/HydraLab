param(
    [String] $familyName
)
if ([string]::IsNullOrEmpty($familyName)) {
    write-host "Need a familyName"
    exit 1
}
try {
    Get-AppxPackage | Where-Object {$_.PackageFamilyName -eq $familyName} | Select-Object PackageFullName -OutVariable fullname | out-null

    $executableName = ((Get-AppxPackageManifest -package $fullname.PackageFullName).package.Applications.Application.Executable)

    $executableName = $executableName.split(".")[0]

    Get-Process | where {$_.mainWindowTitle -and $_.mainWindowHandle -ne 0 -and $_.Name -eq $executableName} | Select-Object mainWindowHandle,id,name,mainWindowTitle -OutVariable processInfo | Out-Null
    $processInfo.MainWindowHandle
} catch {

} 
