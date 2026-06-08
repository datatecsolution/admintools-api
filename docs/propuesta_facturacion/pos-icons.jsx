/* ============================================================
   Iconos (Heroicons outline, stroke 1.7) + utilidades compartidas.
   ============================================================ */

function Icon({ path, size = 22, stroke = 1.7, fill = false, style }) {
  return (
    <svg
      width={size} height={size} viewBox="0 0 24 24"
      fill={fill ? 'currentColor' : 'none'}
      stroke={fill ? 'none' : 'currentColor'}
      strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"
      style={style} aria-hidden="true"
    >
      {Array.isArray(path) ? path.map((d, i) => <path key={i} d={d} />) : <path d={path} />}
    </svg>
  );
}

const ICONS = {
  search: 'M21 21l-4.35-4.35M11 18a7 7 0 100-14 7 7 0 000 14z',
  barcode: ['M3.75 4.5v15', 'M6.75 4.5v15', 'M9.75 4.5v15', 'M13.5 4.5v15', 'M17.25 4.5v15', 'M20.25 4.5v15'],
  plus: 'M12 5v14M5 12h14',
  minus: 'M5 12h14',
  trash: 'M14.74 9l-.34 9m-4.8 0L9.26 9M5 7h14m-1 0l-.87 12.14A2 2 0 0115.14 21H8.86a2 2 0 01-1.99-1.86L6 7m3 0V4a1 1 0 011-1h4a1 1 0 011 1v3',
  x: 'M6 18L18 6M6 6l12 12',
  check: 'M5 13l4 4L19 7',
  checkCircle: 'M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z',
  sun: ['M12 3v1.5M12 19.5V21M4.22 4.22l1.06 1.06M18.72 18.72l1.06 1.06M3 12h1.5M19.5 12H21M4.22 19.78l1.06-1.06M18.72 5.28l1.06-1.06', 'M16 12a4 4 0 11-8 0 4 4 0 018 0z'],
  moon: 'M21.75 15.5A9 9 0 118.5 2.25a7 7 0 0013.25 13.25z',
  user: 'M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.5 19.5a7.5 7.5 0 0115 0',
  cash: ['M2.25 8.25h19.5v9.5H2.25z', 'M12 15.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z', 'M5.5 8.25v9.5M18.5 8.25v9.5'],
  card: ['M2.5 6.75h19v10.5h-19z', 'M2.5 10.5h19'],
  receipt: 'M6 3.75h12v16.5l-2-1.2-2 1.2-2-1.2-2 1.2-2-1.2V3.75zM9 8.25h6M9 11.25h6M9 14.25h3',
  tag: ['M9.6 3.75H5.25A1.5 1.5 0 003.75 5.25V9.6a1.5 1.5 0 00.44 1.06l8.69 8.69a1.5 1.5 0 002.12 0l4.35-4.35a1.5 1.5 0 000-2.12l-8.69-8.69A1.5 1.5 0 009.6 3.75z', 'M7.5 7.5h.01'],
  back: 'M9 15L4 10l5-5M4 10h11a5 5 0 010 10h-1',
  grid: ['M3.75 3.75h6.5v6.5h-6.5z', 'M13.75 3.75h6.5v6.5h-6.5z', 'M3.75 13.75h6.5v6.5h-6.5z', 'M13.75 13.75h6.5v6.5h-6.5z'],
  layoutRight: ['M3.75 4.75h16.5v14.5H3.75z', 'M14.5 4.75v14.5'],
  layoutTop: ['M3.75 4.75h16.5v14.5H3.75z', 'M3.75 9.5h16.5'],
  chevDown: 'M6 9l6 6 6-6',
  idcard: ['M3 5.25h18v13.5H3z', 'M7.5 9.75a1.75 1.75 0 100 3.5 1.75 1.75 0 000-3.5z', 'M13.5 10.5h4M13.5 14h4M6 16.5c.4-1.3 1.4-2 2.5-2s2.1.7 2.5 2'],
  note: ['M5 3.75h9.5L19 8.25V20.25H5z', 'M14 3.75V8.5h4.75', 'M8 12.5h7M8 15.5h7'],
  backspace: ['M9 5.25h10.5a1.5 1.5 0 011.5 1.5v10.5a1.5 1.5 0 01-1.5 1.5H9l-5.5-6a1 1 0 010-1.3z', 'M16 9.5l-4.5 5M11.5 9.5l4.5 5'],
  percent: ['M18.5 5.5l-13 13', 'M7.25 9.5a2 2 0 100-4 2 2 0 000 4z', 'M16.75 18.5a2 2 0 100-4 2 2 0 000 4z'],
  hash: ['M8.75 4l-1.5 16', 'M16.75 4l-1.5 16', 'M4.5 9h15', 'M3.5 15h15'],
  truck: ['M2.75 6.5h11.5v8.5H2.75z', 'M14.25 9.25h3.4l3 3.25v2.5h-6.4z', 'M7 17.9a1.9 1.9 0 100-3.8 1.9 1.9 0 000 3.8z', 'M17.5 17.9a1.9 1.9 0 100-3.8 1.9 1.9 0 000 3.8z'],
  cashIn: ['M3 10.5h18v8.25H3z', 'M12 16.5a2 2 0 100-4 2 2 0 000 4z', 'M12 3.25v4.5', 'M9.75 5.75L12 8l2.25-2.25'],
  cashOut: ['M3 10.5h18v8.25H3z', 'M12 16.5a2 2 0 100-4 2 2 0 000 4z', 'M12 8.25v-4.5', 'M9.75 6L12 3.75 14.25 6'],
  save: ['M5 4.75h9.2L19 9.5V19.25H5z', 'M8 4.75v4h6v-4', 'M8 13h8v6.25H8z'],
  docPlus: ['M6 3.75h8L17.5 7.25V20.25H6z', 'M13.5 3.75V7.5h4', 'M11.75 11.25v5M9.25 13.75h5'],
  list: ['M8.5 6.5h11.5', 'M8.5 12h11.5', 'M8.5 17.5h11.5', 'M4.25 6.5h.01', 'M4.25 12h.01', 'M4.25 17.5h.01'],
  drawer: ['M3.5 10h17v8.5h-17z', 'M5.25 10L7 5.5h10L18.75 10', 'M10 13.75h4'],
  dots: ['M6 12h.01', 'M12 12h.01', 'M18 12h.01'],
  orderList: ['M6 3.75h8L17.5 7.25V20.25H6z', 'M13.5 3.75V7.5h4', 'M8.75 11.5h6.5M8.75 14.5h6.5M8.75 17.5h4'],
};

function I(props) {
  return <Icon path={ICONS[props.name]} {...props} />;
}

// Formato moneda Lempira
const fmtL = new Intl.NumberFormat('es-HN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
function money(n) { return 'L ' + fmtL.format(n || 0); }

Object.assign(window, { Icon, I, ICONS, money });
