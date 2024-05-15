# Copyright (c) Microsoft Corporation.
# Licensed under the MIT License.

param (
    [string]$keyword = "Phone",
    [string]$output = ""
)

if ($output -eq "") {
    write-host "-output is required."
    exit 1
}

Get-Process | Where-Object {$_.ProcessName -like "*$keyword*"} |
%{"Id={0} ProcessName={1} CPU={2} NonpagedSystemMemorySize64={3} PagedMemorySize64={4} PagedSystemMemorySize64={5} PeakPagedMemorySize64={6} PeakVirtualMemorySize64={7} PeakWorkingSet64={8} PrivateMemorySize64={9} WorkingSet64={10} Description={11} Path={12} Product={13} ProductVersion={14}" -f
$_.Id, $_.ProcessName, $_.CPU, $_.NonpagedSystemMemorySize64, $_.PagedMemorySize64, $_.PagedSystemMemorySize64, $_.PeakPagedMemorySize64, $_.PeakVirtualMemorySize64, $_.PeakWorkingSet64, $_.PrivateMemorySize64, $_.WorkingSet64, $_.Description, $_.Path, $_.Product, $_.ProductVersion} |
Out-File -FilePath $output

exit 0