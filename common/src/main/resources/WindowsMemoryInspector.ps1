param (
    [string]$keyword = "Phone",
	[string]$output = ""
)

if ($output -eq "") {
    write-host "-output is required."
    exit 1
}

Get-Process | Where-Object {$_.ProcessName -like "*$keyword*"} | %{"ID={0} Process_Name={1} CPU_Usage={2} Memory_Usage={3}" -f $_.Id, $_.ProcessName, $_.CPU, $_.PM} | Out-File -FilePath $output
exit 0