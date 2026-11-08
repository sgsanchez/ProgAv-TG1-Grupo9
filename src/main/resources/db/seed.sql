-- ============================================================================
-- Datos semilla del Sistema de Ventas y Facturacion - Grupo 9
-- Se ejecuta al arrancar la aplicacion (perfiles dev y postgres).
-- Es idempotente: cada INSERT comprueba antes si el registro ya existe,
-- por lo que se puede reiniciar la aplicacion sin duplicar informacion.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Clientes
-- ---------------------------------------------------------------------------
INSERT INTO clientes (cedula, nombre, apellido, email, telefono, direccion, fecha_registro, activo)
SELECT '1712345678', 'Ana', 'Perez', 'ana.perez@espe.edu.ec', '0991111111', 'Av. General Ruminahui', CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE cedula = '1712345678');

INSERT INTO clientes (cedula, nombre, apellido, email, telefono, direccion, fecha_registro, activo)
SELECT '1798765432', 'Luis', 'Torres', 'luis.torres@espe.edu.ec', '0992222222', 'Av. Amazonas y Colon', CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE cedula = '1798765432');

INSERT INTO clientes (cedula, nombre, apellido, email, telefono, direccion, fecha_registro, activo)
SELECT '0501234567', 'Maria', 'Jimenez', 'maria.jimenez@espe.edu.ec', '0993333333', 'Calle Los Shyris', CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM clientes WHERE cedula = '0501234567');

-- ---------------------------------------------------------------------------
-- Productos (P-004 queda por debajo del stock minimo para la consulta de alertas)
-- ---------------------------------------------------------------------------
INSERT INTO productos (codigo, nombre, descripcion, precio, stock, stock_minimo, activo)
SELECT 'P-001', 'Cuaderno universitario', 'Cuaderno de 100 hojas', 2.50, 100, 20, TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'P-001');

INSERT INTO productos (codigo, nombre, descripcion, precio, stock, stock_minimo, activo)
SELECT 'P-002', 'Esfero azul', 'Esfero de tinta azul', 0.80, 250, 50, TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'P-002');

INSERT INTO productos (codigo, nombre, descripcion, precio, stock, stock_minimo, activo)
SELECT 'P-003', 'Mochila', 'Mochila de 25 litros', 25.00, 15, 10, TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'P-003');

INSERT INTO productos (codigo, nombre, descripcion, precio, stock, stock_minimo, activo)
SELECT 'P-004', 'Calculadora cientifica', 'Calculadora de 240 funciones', 18.75, 8, 10, TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'P-004');

INSERT INTO productos (codigo, nombre, descripcion, precio, stock, stock_minimo, activo)
SELECT 'P-005', 'Resma de papel', 'Resma A4 de 500 hojas', 4.60, 60, 15, TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE codigo = 'P-005');

-- ---------------------------------------------------------------------------
-- Ventas
-- V-0001: Ana Perez  -> subtotal 15.50 | IVA 2.33 | total 17.83 (PAGADA)
-- V-0002: Luis Torres -> subtotal 25.00 | IVA 3.75 | total 28.75 (PAGADA)
-- V-0003: Maria Jimenez -> subtotal 18.75 | IVA 2.81 | total 21.56 (ANULADA)
-- ---------------------------------------------------------------------------
INSERT INTO ventas (numero, cliente_id, fecha_venta, subtotal, iva, total, estado)
SELECT 'V-0001', c.id, CURRENT_TIMESTAMP - INTERVAL '5' DAY, 15.50, 2.33, 17.83, 'PAGADA'
FROM clientes c
WHERE c.cedula = '1712345678'
  AND NOT EXISTS (SELECT 1 FROM ventas WHERE numero = 'V-0001');

INSERT INTO ventas (numero, cliente_id, fecha_venta, subtotal, iva, total, estado)
SELECT 'V-0002', c.id, CURRENT_TIMESTAMP - INTERVAL '2' DAY, 25.00, 3.75, 28.75, 'PAGADA'
FROM clientes c
WHERE c.cedula = '1798765432'
  AND NOT EXISTS (SELECT 1 FROM ventas WHERE numero = 'V-0002');

INSERT INTO ventas (numero, cliente_id, fecha_venta, subtotal, iva, total, estado)
SELECT 'V-0003', c.id, CURRENT_TIMESTAMP - INTERVAL '1' DAY, 18.75, 2.81, 21.56, 'ANULADA'
FROM clientes c
WHERE c.cedula = '0501234567'
  AND NOT EXISTS (SELECT 1 FROM ventas WHERE numero = 'V-0003');

-- ---------------------------------------------------------------------------
-- Detalles de venta
-- ---------------------------------------------------------------------------
-- V-0001: 3 cuadernos (7.50) + 10 esferos (8.00)
INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 3, 2.50, 7.50
FROM ventas v, productos p
WHERE v.numero = 'V-0001' AND p.codigo = 'P-001'
  AND NOT EXISTS (SELECT 1 FROM detalle_venta d WHERE d.venta_id = v.id AND d.producto_id = p.id);

INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 10, 0.80, 8.00
FROM ventas v, productos p
WHERE v.numero = 'V-0001' AND p.codigo = 'P-002'
  AND NOT EXISTS (SELECT 1 FROM detalle_venta d WHERE d.venta_id = v.id AND d.producto_id = p.id);

-- V-0002: 1 mochila (25.00)
INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, 25.00, 25.00
FROM ventas v, productos p
WHERE v.numero = 'V-0002' AND p.codigo = 'P-003'
  AND NOT EXISTS (SELECT 1 FROM detalle_venta d WHERE d.venta_id = v.id AND d.producto_id = p.id);

-- V-0003: 1 calculadora (18.75) - venta anulada
INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario, subtotal)
SELECT v.id, p.id, 1, 18.75, 18.75
FROM ventas v, productos p
WHERE v.numero = 'V-0003' AND p.codigo = 'P-004'
  AND NOT EXISTS (SELECT 1 FROM detalle_venta d WHERE d.venta_id = v.id AND d.producto_id = p.id);
