"""Demostración HTTP reproducible de Clientes con H2 en disco y reinicio real.

Uso: python scripts/demo-clientes.py --output docs/evidencias/clientes-http.json
Requiere Java 21, Python 3 y haber ejecutado mvnw verify. No modifica la BD del grupo.
"""
import argparse
import json
import socket
import subprocess
import tempfile
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", default="target/ventas-facturacion-1.0.0.jar")
    parser.add_argument("--output", default="target/clientes-http.json")
    args = parser.parse_args()
    jar = Path(args.jar).resolve(strict=True)
    evidencias = []
    # Elegir un puerto libre para no interferir con una API que ya esté ejecutándose.
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
    base = f"http://127.0.0.1:{port}"
    proceso = None

    def solicitar(nombre, metodo, ruta, esperado, cuerpo=None, clave=True):
        headers = {"Content-Type": "application/json"}
        if clave:
            headers["X-API-KEY"] = "demo-clientes-local"
        data = None if cuerpo is None else json.dumps(cuerpo).encode()
        request = urllib.request.Request(base + ruta, data=data, headers=headers, method=metodo)
        inicio = time.perf_counter()
        try:
            response = urllib.request.urlopen(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            texto = response.read().decode()
            resultado = json.loads(texto) if texto else None
            registro = {"caso": nombre, "metodo": metodo, "ruta": ruta,
                        "esperado": esperado, "status": response.status,
                        "duracion_ms": round((time.perf_counter() - inicio) * 1000, 2),
                        "respuesta": resultado}
            evidencias.append(registro)
            if response.status != esperado:
                raise AssertionError(f"{nombre}: {registro}")
            print(f"OK {response.status} {nombre}")
            return resultado

    def detener():
        nonlocal proceso
        if proceso is not None and proceso.poll() is None:
            proceso.terminate()
            try:
                proceso.wait(timeout=20)
            except subprocess.TimeoutExpired:
                proceso.kill()
                proceso.wait(timeout=10)

    with tempfile.TemporaryDirectory(prefix="demo-clientes-") as temporal:
        logpath = Path(temporal) / "aplicacion.log"
        with logpath.open("w", encoding="utf-8") as log:
            def iniciar():
                nonlocal proceso
                proceso = subprocess.Popen([
                    "java", "-jar", str(jar), "--spring.profiles.active=dev",
                    f"--server.port={port}", "--server.address=127.0.0.1",
                    # Forzar escritura en disco antes del reinicio, incluso si Windows termina Java abruptamente.
                    "--spring.datasource.url=jdbc:h2:file:./clientes-demo;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0",
                    "--spring.sql.init.mode=never", "--spring.h2.console.enabled=false",
                    "--app.security.api-key=demo-clientes-local",
                    "--app.comprobante.retardo-ms=0"],
                    cwd=temporal, stdout=log, stderr=subprocess.STDOUT)
                for _ in range(120):
                    if proceso.poll() is not None:
                        raise RuntimeError(logpath.read_text(encoding="utf-8", errors="replace")[-5000:])
                    try:
                        req = urllib.request.Request(base + "/api/clientes", headers={"X-API-KEY": "demo-clientes-local"})
                        with urllib.request.urlopen(req, timeout=2):
                            return
                    except (urllib.error.URLError, TimeoutError):
                        time.sleep(0.5)
                raise TimeoutError("La API no inició en el plazo esperado")

            try:
                iniciar()
                datos = {"cedula": "1800000099", "nombre": " Ana ", "apellido": " Perez ",
                         "email": "DEMO.CLIENTE@EXAMPLE.COM", "telefono": "0991234567", "direccion": "Quito"}
                solicitar("Interceptor sin clave", "GET", "/api/clientes", 401, clave=False)
                cliente = solicitar("Registro y normalización", "POST", "/api/clientes", 201, datos)
                assert cliente["email"] == "demo.cliente@example.com"
                ruta = f"/api/clientes/{cliente['id']}"
                consultado = solicitar("Consulta por ID", "GET", ruta, 200)
                lista = solicitar("Filtro con espacios y mayúsculas", "GET", "/api/clientes?apellido=%20PeR%20", 200)
                assert len(lista) == 1
                solicitar("Cédula duplicada", "POST", "/api/clientes", 409, {**datos, "email": "otra@example.com"})
                solicitar("Correo duplicado sin distinguir mayúsculas", "POST", "/api/clientes", 409,
                          {**datos, "cedula": "1800000098", "email": "demo.cliente@example.com"})
                solicitar("Validación de cédula", "POST", "/api/clientes", 400, {**datos, "cedula": "123"})
                actualizado = solicitar("Actualización", "PUT", ruta, 200, {**datos, "nombre": "Ana Maria"})
                # Comparar con la fecha ya persistida: SQL puede redondear los nanosegundos.
                assert actualizado["fechaRegistro"] == consultado["fechaRegistro"]
                detener()
                iniciar()
                persistido = solicitar("Persistencia después de reiniciar Java", "GET", ruta, 200)
                assert persistido["nombre"] == "Ana Maria"
                solicitar("Eliminación sin ventas", "DELETE", ruta, 204)
                solicitar("Consulta de recurso eliminado", "GET", ruta, 404)
                auditoria = solicitar("Auditoría de operaciones de clientes", "GET", "/api/auditoria", 200)
                assert any(a["operacion"] == "ClienteService.crear" for a in auditoria)
                assert any(a["operacion"] == "ClienteService.eliminar" for a in auditoria)
                salida = Path(args.output)
                salida.parent.mkdir(parents=True, exist_ok=True)
                salida.write_text(json.dumps({"fecha_utc": datetime.now(timezone.utc).isoformat(),
                    "entorno": "Java 21 / Spring Boot / H2 persistente en directorio temporal aislado",
                    "reinicio_verificado": True, "casos": evidencias}, ensure_ascii=False, indent=2), encoding="utf-8")
                print(f"Evidencias guardadas: {salida}")
            finally:
                detener()


if __name__ == "__main__":
    main()
