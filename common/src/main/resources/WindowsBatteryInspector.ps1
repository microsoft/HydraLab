# Copyright (c) Microsoft Corporation.
# Licensed under the MIT License.

param (
	[string]$output = ""
)

if ($output -eq "") {
    Write-Host "-output is required."
    Read-Host -Prompt "output is empty."
    exit 1
}

Start-Process -FilePath Powershell.exe -Verb RunAs -WindowStyle Hidden -ArgumentList "-command ""powercfg /srumutil /OUTPUT $output /CSV"""

exit 0