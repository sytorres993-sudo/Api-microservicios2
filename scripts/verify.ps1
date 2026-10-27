param()
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$logs = Join-Path ([IO.Path]::GetTempPath()) ('tienda-verify-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $logs | Out-Null
$processes = @()
$script:checks = 0

function Stop-TestProcess($Child) {
    if (-not $Child.HasExited) {
        # El lanzador de Oracle puede crear otro java.exe: detener todo el arbol propio.
        & taskkill.exe /PID $Child.Id /T /F | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "No fue posible detener el proceso de prueba $($Child.Id)" }
        $Child.WaitForExit()
    }
}
function Assert-Equal($Actual, $Expected, [string]$Message) {
    if ($Actual -ne $Expected) { throw "$Message. Esperado: $Expected; recibido: $Actual" }
    $script:checks++
}
function Send([string]$Method, [string]$Url, $Body, [int]$Expected) {
    $params = @{ Uri=$Url; Method=$Method; UseBasicParsing=$true; TimeoutSec=15 }
    if ($null -ne $Body) {
        $params.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 12))
        $params.ContentType = 'application/json; charset=utf-8'
    }
    $content = ''
    try {
        $response = Invoke-WebRequest @params
        $status = [int]$response.StatusCode
        $content = $response.Content
    } catch {
        if ($null -eq $_.Exception.Response) { throw }
        $status = [int]$_.Exception.Response.StatusCode
        if ($_.ErrorDetails.Message) { $content = $_.ErrorDetails.Message }
        elseif ($_.Exception.Response -is [Net.HttpWebResponse]) {
            $reader = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
            try { $content = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
    }
    Assert-Equal $status $Expected "$Method $Url ($content)"
    if ($content) { return ($content | ConvertFrom-Json) }
}
try {
    $ports = @(18081,18082,18083)
    foreach ($port in $ports) {
        $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, $port)
        try { $listener.Start() } finally { $listener.Stop() }
    }
    $names = @('productos','clientes','pedidos')
    foreach ($index in 0..2) {
        $name = $names[$index]
        $jar = Join-Path $root "$name-service/target/$name-service-0.0.1-SNAPSHOT.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "Falta $jar. Ejecute .\mvnw.cmd clean verify primero." }
        $arguments = @('-Xms32m', '-Xmx192m', '-jar', ('"' + $jar + '"'), "--server.port=$($ports[$index])", "--spring.datasource.url=jdbc:h2:mem:smoke-$name")
        if ($name -eq 'pedidos') { $arguments += @('--services.productos.url=http://localhost:18081','--services.clientes.url=http://localhost:18082') }
        $processes += Start-Process -FilePath (Get-Command java).Source -ArgumentList $arguments -WorkingDirectory $root -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs "$name.out.log") -RedirectStandardError (Join-Path $logs "$name.err.log")
    }
    foreach ($index in 0..2) {
        $ready = $false
        foreach ($attempt in 1..60) {
            if ($processes[$index].HasExited) { throw "El servicio $($names[$index]) termino al iniciar. Consulte $logs" }
            try {
                $health = Invoke-RestMethod -Uri "http://localhost:$($ports[$index])/actuator/health" -TimeoutSec 2
                if ($health.status -eq 'UP') { $ready=$true; break }
            } catch {}
            Start-Sleep -Seconds 1
        }
        if (-not $ready) { throw "Timeout al iniciar $($names[$index]). Consulte $logs" }
    }
    Write-Host 'Los tres servicios estan listos. Comprobando API...'
    $productUrl='http://localhost:18081/api/productos'
    $clientUrl='http://localhost:18082/api/clientes'
    $orderUrl='http://localhost:18083/api/pedidos'
    $pBody=@{sku='TEST-001'; nombre='Portatil'; marca='Lenovo'; categoria='Computadores'; descripcion='Prueba'; precio=1250.50; stock=10}
    $cBody=@{nombre='Ana Garcia'; email='ana@example.com'; telefono='+57 3001234567'; direccion='Bogota'}
    $p=Send POST $productUrl $pBody 201
    $c=Send POST $clientUrl $cBody 201
    $null=Send POST $productUrl $pBody 409
    $null=Send POST $clientUrl $cBody 409
    $null=Send GET $productUrl $null 200
    $filtered=Send GET "${productUrl}?categoria=Computadores&buscar=Lenovo" $null 200
    $null=Send GET "$clientUrl/$($c.id)" $null 200
    $pBody.nombre='Portatil actualizado'
    $null=Send PUT "$productUrl/$($p.id)" $pBody 200
    $cBody.nombre='Ana Actualizada'
    $null=Send PUT "$clientUrl/$($c.id)" $cBody 200
    $o=Send POST $orderUrl @{clienteId=$c.id; items=@(@{productoId=$p.id;cantidad=2},@{productoId=$p.id;cantidad=1})} 201
    Assert-Equal $o.estado 'CONFIRMADO' 'Estado del pedido'
    Assert-Equal $o.total 3751.50 'Total con cantidades repetidas'
    Assert-Equal $o.items.Count 1 'Lineas repetidas agrupadas'
    Assert-Equal $o.clienteNombre 'Ana Actualizada' 'Nombre del cliente consultado por HTTP'
    $after=Send GET "$productUrl/$($p.id)" $null 200
    Assert-Equal $after.stock 7 'Descuento de existencias'
    $null=Send POST "$orderUrl/$($o.id)/confirmar" $null 200
    $after=Send GET "$productUrl/$($p.id)" $null 200
    Assert-Equal $after.stock 7 'Confirmacion sin doble descuento'
    $pBody.precio=2000
    $pBody.stock=7
    $null=Send PUT "$productUrl/$($p.id)" $pBody 200
    $historical=Send GET "$orderUrl/$($o.id)" $null 200
    Assert-Equal $historical.total 3751.50 'El pedido conserva el precio historico'
    $pBody.precio=1250.50
    $null=Send PUT "$productUrl/$($p.id)" $pBody 200
    $null=Send GET "$orderUrl/$($o.id)" $null 200
    $null=Send GET "${orderUrl}?clienteId=$($c.id)" $null 200
    $null=Send POST "$orderUrl/$($o.id)/cancelar" $null 200
    $null=Send POST "$orderUrl/$($o.id)/cancelar" $null 200
    $after=Send GET "$productUrl/$($p.id)" $null 200
    Assert-Equal $after.stock 10 'Cancelacion sin doble devolucion'
    $null=Send POST "$orderUrl/$($o.id)/confirmar" $null 409
    $bad=@{}+$pBody; $bad.precio=-1
    $null=Send POST $productUrl $bad 400
    $bad=@{}+$cBody; $bad.email='invalido'
    $null=Send POST $clientUrl $bad 400
    $null=Send POST $orderUrl @{clienteId=$c.id;items=@(@{productoId=$p.id;cantidad=0})} 400
    $null=Send POST $orderUrl @{clienteId=$c.id;items=@($null)} 400
    $null=Send GET "$orderUrl/no-es-uuid" $null 400
    $null=Send POST $orderUrl @{clienteId=999999;items=@(@{productoId=$p.id;cantidad=1})} 404
    $missing=Send POST $orderUrl @{clienteId=$c.id;items=@(@{productoId=999999;cantidad=1})} 404
    $null=Send POST "$orderUrl/$($missing.pedidoId)/cancelar" $null 200
    $insufficient=Send POST $orderUrl @{clienteId=$c.id;items=@(@{productoId=$p.id;cantidad=11})} 409
    $pending=Send GET "$orderUrl/$($insufficient.pedidoId)" $null 200
    Assert-Equal $pending.estado 'PENDIENTE' 'Pedido recuperable tras falta de stock'
    $pBody.stock=15
    $null=Send PUT "$productUrl/$($p.id)" $pBody 200
    # Simula reserva confirmada en inventario antes de que pedidos guarde su resultado.
    $null=Send PUT "http://localhost:18081/api/reservas/$($pending.id)" @{items=@(@{productoId=$p.id;cantidad=11})} 200
    $retried=Send POST "$orderUrl/$($pending.id)/confirmar" $null 200
    Assert-Equal $retried.total 13755.50 'Total de pedido reintentado'
    $after=Send GET "$productUrl/$($p.id)" $null 200
    Assert-Equal $after.stock 4 'Inventario reservado al reintentar'
    $null=Send POST "$orderUrl/$($pending.id)/cancelar" $null 200

    $pBody.sku='TEMP-DELETE'
    $temporaryProduct=Send POST $productUrl $pBody 201
    $null=Send DELETE "$productUrl/$($temporaryProduct.id)" $null 204
    $null=Send GET "$productUrl/$($temporaryProduct.id)" $null 404
    $cBody.email='temporal@example.com'
    $temporaryClient=Send POST $clientUrl $cBody 201
    $null=Send DELETE "$clientUrl/$($temporaryClient.id)" $null 204
    $null=Send GET "$clientUrl/$($temporaryClient.id)" $null 404

    # Dependencia caida: 503, pedido recuperable y cancelacion posterior.
    Stop-TestProcess $processes[0]
    $unavailable=Send POST $orderUrl @{clienteId=$c.id;items=@(@{productoId=$p.id;cantidad=1})} 503
    $pending=Send GET "$orderUrl/$($unavailable.pedidoId)" $null 200
    Assert-Equal $pending.estado 'PENDIENTE' 'Pedido conservado cuando inventario no responde'
    $null=Send POST "$orderUrl/$($pending.id)/cancelar" $null 503
    Stop-TestProcess $processes[1]
    $null=Send POST $orderUrl @{clienteId=$c.id;items=@(@{productoId=$p.id;cantidad=1})} 503
    Write-Host "OK: $script:checks comprobaciones HTTP y de negocio. Logs: $logs"
} finally {
    foreach ($child in $processes) {
        try { Stop-TestProcess $child } catch { Write-Warning $_.Exception.Message }
    }
}
