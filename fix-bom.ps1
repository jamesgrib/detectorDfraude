$noBom = [System.Text.UTF8Encoding]::new($false)

# Fix TarjetaService
$ts = [System.IO.File]::ReadAllText("c:\Users\mater\Desktop\fraude-detection\src\main\java\com\fraude\tarjeta\service\TarjetaService.java")
$ts = $ts.TrimStart([char]0xFEFF)
[System.IO.File]::WriteAllText("c:\Users\mater\Desktop\fraude-detection\src\main\java\com\fraude\tarjeta\service\TarjetaService.java", $ts, $noBom)

# Fix FacturaService
$fs = [System.IO.File]::ReadAllText("c:\Users\mater\Desktop\fraude-detection\src\main\java\com\fraude\factura\service\FacturaService.java")
$fs = $fs.TrimStart([char]0xFEFF)
[System.IO.File]::WriteAllText("c:\Users\mater\Desktop\fraude-detection\src\main\java\com\fraude\factura\service\FacturaService.java", $fs, $noBom)

Write-Host "BOM removed from both service files"
