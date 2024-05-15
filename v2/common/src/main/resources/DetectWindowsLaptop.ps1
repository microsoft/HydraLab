# Copyright (c) Microsoft Corporation.
# Licensed under the MIT License.

$isWindowsLaptop = $false

# See more info regarding ChassisTypes: https://powershell.one/wmi/root/cimv2/win32_systemenclosure#chassistypes
# 9-Laptop, 10-Notebook, 14-Sub Notebook
$isWindowsLaptop = [bool](Get-WmiObject -Class Win32_SystemEnclosure | Where-Object ChassisTypes -in '9', '10', '14')

# By checking the battery status, it can be determined if the device is a laptop.
if ((-not $isWindowsLaptop) -and (Get-WmiObject -Class win32_battery))
{
    $isWindowsLaptop = $true
}

if ($isWindowsLaptop)
{
    Write-Host "Yes"
}
else
{
    Write-Host "No"
}

exit 0