/* ============================================================
   Datos mock — tienda retail (Honduras). Moneda HNL (Lempiras).
   ISV: 0.15 estándar, 0 exento. Códigos = código de barras / altCode.
   ============================================================ */
(function () {
  // Paleta de monogramas por categoría (oklch armónico, baja croma).
  const CAT = [
    { id: 1, name: 'Bebidas',    color: 'oklch(0.62 0.14 250)' },
    { id: 2, name: 'Abarrotes',  color: 'oklch(0.60 0.13 145)' },
    { id: 3, name: 'Lácteos',    color: 'oklch(0.64 0.12 75)'  },
    { id: 4, name: 'Panadería',  color: 'oklch(0.62 0.13 50)'  },
    { id: 5, name: 'Snacks',     color: 'oklch(0.60 0.14 25)'  },
    { id: 6, name: 'Limpieza',   color: 'oklch(0.60 0.13 195)' },
    { id: 7, name: 'Higiene',    color: 'oklch(0.60 0.13 300)' },
    { id: 8, name: 'Carnes',     color: 'oklch(0.58 0.14 15)'  },
  ];

  // price = precio público (L). tax = ISV. unit = unidad de venta.
  const P = [
    // Bebidas (1)
    ['Coca-Cola 600ml',        '7501055300013', 1, 22.00, 0.15, 'und'],
    ['Pepsi 500ml',            '7411000023456', 1, 20.00, 0.15, 'und'],
    ['Agua Azul 500ml',        '7421000110023', 1, 12.00, 0.15, 'und'],
    ['Jugo Del Valle 1L',      '7501055362210', 1, 34.00, 0.15, 'und'],
    ['Café Maya 200g',         '7460000048810', 1, 78.00, 0.15, 'und'],
    ['Cerveza Salva Vida lata','7411000019087', 1, 28.00, 0.18, 'und'],
    ['Gatorade 600ml',         '7411000045512', 1, 30.00, 0.15, 'und'],
    // Abarrotes (2)
    ['Arroz Progreso 5lb',     '7421001220015', 2, 62.00, 0.15, 'und'],
    ['Frijol Ducal 29oz',      '7411001770024', 2, 38.00, 0.15, 'und'],
    ['Aceite Issima 750ml',    '7421005140037', 2, 49.00, 0.15, 'und'],
    ['Maseca 2lb',             '7501020563012', 2, 24.00, 0.15, 'und'],
    ['Azúcar blanca 5lb',      '7421008810044', 2, 55.00, 0.00, 'und'],
    ['Sal Sol 1lb',            '7421009920016', 2,  9.00, 0.00, 'und'],
    ['Atún Sardimar lata',     '7441001230058', 2, 27.00, 0.15, 'und'],
    ['Pasta Naturas 200g',     '7421004551239', 2, 14.00, 0.15, 'und'],
    // Lácteos (3)
    ['Leche Sula 1L',          '7421000110078', 3, 28.00, 0.15, 'und'],
    ['Queso seco lb',          '0000000000301', 3, 95.00, 0.00, 'lb'],
    ['Mantequilla Sula 250g',  '7421000119012', 3, 42.00, 0.15, 'und'],
    ['Yogurt Yoplait 150g',    '7421000118091', 3, 16.00, 0.15, 'und'],
    ['Huevos cartón 30u',      '0000000000318', 3, 105.00, 0.00, 'und'],
    // Panadería (4)
    ['Pan Bimbo grande',       '7501000670023', 4, 48.00, 0.15, 'und'],
    ['Pan dulce unidad',       '0000000000404', 4,  6.00, 0.15, 'und'],
    ['Tortillas paquete 20u',  '0000000000411', 4, 25.00, 0.00, 'und'],
    // Snacks (5)
    ['Churritos Yummies',      '7411000067123', 5, 12.00, 0.15, 'und'],
    ['Galleta Crema Rellenas', '7591001112345', 5, 10.00, 0.15, 'und'],
    ['Chocolate Tigo barra',   '7411000099210', 5, 15.00, 0.15, 'und'],
    ['Maní salado 100g',       '7411000088134', 5, 18.00, 0.15, 'und'],
    // Limpieza (6)
    ['Detergente Xtra 1kg',    '7501025404013', 6, 58.00, 0.15, 'und'],
    ['Cloro Magia 1L',         '7421006330027', 6, 24.00, 0.15, 'und'],
    ['Jabón Marfil barra',     '7501035911018', 6,  9.50, 0.15, 'und'],
    ['Lavaplatos Axion 425g',  '7501035955019', 6, 32.00, 0.15, 'und'],
    // Higiene (7)
    ['Papel higiénico Scott 4u','7806500992018', 7, 45.00, 0.15, 'und'],
    ['Pasta Colgate 100ml',    '7509546000012', 7, 28.00, 0.15, 'und'],
    ['Jabón Protex barra',     '7509546050017', 7, 14.00, 0.15, 'und'],
    ['Shampoo Sedal 190ml',    '7506306234561', 7, 38.00, 0.15, 'und'],
    // Carnes (8)
    ['Pollo entero lb',        '0000000000801', 8, 32.00, 0.00, 'lb'],
    ['Carne molida res lb',    '0000000000818', 8, 68.00, 0.00, 'lb'],
    ['Chuleta de cerdo lb',    '0000000000825', 8, 58.00, 0.00, 'lb'],
    ['Jamón rebanado lb',      '0000000000832', 8, 75.00, 0.15, 'lb'],
  ];

  function monogram(name) {
    const clean = name.replace(/[0-9]+(ml|g|lb|oz|kg|u|L)?/gi, '').trim();
    const words = clean.split(/\s+/).filter(Boolean);
    if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
    return (words[0][0] + words[1][0]).toUpperCase();
  }

  const products = P.map((row, i) => {
    const [name, code, categoryId, price, tax, unit] = row;
    const cat = CAT.find((c) => c.id === categoryId);
    return {
      id: i + 1,
      name, code, categoryId, price, tax, unit,
      catName: cat.name,
      color: cat.color,
      mono: monogram(name),
    };
  });

  // Clientes mock — modelo del repo: name, rtn, address, phone.
  const customers = [
    { id: 1, name: 'Pulpería La Esquina',      rtn: '0801-1998-004521', phone: '9988-2233', address: 'Col. Kennedy, bloque 4, Tegucigalpa' },
    { id: 2, name: 'Ferretería El Clavo',      rtn: '0501-1990-112233', phone: '9712-4500', address: 'Bo. Lempira, San Pedro Sula' },
    { id: 3, name: 'Comercial Mejía S. de R.L.',rtn: '0801-1985-667788', phone: '2235-9087', address: 'Av. La Paz, Comayagüela' },
    { id: 4, name: 'Distribuidora Suyapa',     rtn: '0801-2002-554120', phone: '3344-1199', address: 'Col. Miraflores, Tegucigalpa' },
    { id: 5, name: 'Carlos Andino',            rtn: '0801-1992-008145', phone: '9456-7781', address: 'Res. Las Hadas, casa 12' },
    { id: 6, name: 'Glenda Paredes',           rtn: '',                 phone: '8890-1245', address: 'Col. La Granja' },
    { id: 7, name: 'Mini Súper El Ahorro',     rtn: '0703-1999-330021', phone: '2668-7720', address: 'Barrio El Centro, La Ceiba' },
    { id: 8, name: 'José Ramón Cálix',         rtn: '0801-1978-001209', phone: '9901-3367', address: 'Col. Las Uvas, Tegucigalpa' },
  ];

  // Vendedores — usuarios con rol SELLER (tipoPermiso 3) + codigoEmpleado.
  const vendedores = [
    { id: 3,  nombreCompleto: 'María Fúnez',      codigoEmpleado: 101 },
    { id: 7,  nombreCompleto: 'Kevin Lozano',     codigoEmpleado: 104 },
    { id: 11, nombreCompleto: 'Daniela Mejía',    codigoEmpleado: 108 },
    { id: 14, nombreCompleto: 'Óscar Banegas',    codigoEmpleado: 112 },
    { id: 19, nombreCompleto: 'Wendy Cárcamo',    codigoEmpleado: 117 },
  ];

  window.POS_DATA = {
    categories: CAT,
    products,
    customers,
    vendedores,
    cashDenoms: [50, 100, 200, 500],
    cajero: { name: 'María Fúnez', role: 'Cajero', caja: 'Caja 01', initials: 'MF', codigoEmpleado: 101 },
  };

  /* ------------------------------------------------------------
   * Configuración del módulo de facturación.
   *
   * IMPORTANTE: en producción estos valores provienen de
   * admintools-api (configuración del sistema en la base de datos).
   * Este módulo es de SOLO LECTURA: extrae la configuración y la
   * aplica; NO la modifica desde aquí.
   *
   * Reemplazar este objeto por el resultado de, p. ej.:
   *     GET /api/config/facturacion
   * y mapear sus campos a estas banderas.
   * ---------------------------------------------------------- */
  window.POS_CONFIG = {
    facturacion: {
      pedirVendedor: true,        // ← config.facturacion.pedirVendedor (API)
      pedirObservaciones: true,   // ← config.facturacion.pedirObservaciones (API)
    },
  };
})();
