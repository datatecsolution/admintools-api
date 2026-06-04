/* ============================================================
   Facturación Táctil — app principal (POS retail).
   ============================================================ */
const { useState, useEffect, useRef, useMemo, useCallback } = React;

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "dark": false,
  "density": "Cómodo",
  "thumb": "Monograma",
  "cfgVendedor": true,
  "cfgObservaciones": true
}/*EDITMODE-END*/;

const ACCENT_MAP = {
  '#16a34a': 'green',
  '#2563eb': 'blue',
  '#7c3aed': 'violet',
};

/* Botones de funciones de caja (barra inferior del catálogo).
   `kind` distingue acciones de ticket vs. operaciones de caja. */
const FUNC_BUTTONS = [
  { key: 'descuentos', label: 'Descuentos', icon: 'percent', kind: 'ticket',
    desc: 'Aplicar un descuento por línea o al total del ticket (porcentaje o monto).' },
  { key: 'precios', label: 'Precios', icon: 'tag', kind: 'ticket',
    desc: 'Cambiar el tipo de precio del ticket (Público / Mayorista) o consultar precios.' },
  { key: 'cantidad', label: 'Cantidad', icon: 'hash', kind: 'ticket',
    desc: 'Editar la cantidad de la línea seleccionada con el teclado numérico.' },
  { key: 'guardar', label: 'Guardar', icon: 'save', kind: 'ticket',
    desc: 'Guardar la factura actual como una orden en la base de datos, en su estado actual, para retomarla luego.' },
  { key: 'listaOrdenes', label: 'Lista de órdenes', icon: 'orderList', kind: 'ticket',
    desc: 'Recuperar una orden guardada (se almacena en una tabla distinta a las cotizaciones) para cargarla al ticket.' },
];

/* Funciones secundarias / periódicas — viven en el panel "Más"
   para no saturar la barra principal. */
const FUNC_MORE = [
  { key: 'cotizacion', label: 'Crear cotización', icon: 'docPlus', group: 'Documento',
    desc: 'Generar una cotización con el ticket configurado (sin cobrar) y guardarla en la base de datos.' },
  { key: 'listaCotizaciones', label: 'Lista de cotizaciones', icon: 'list', group: 'Documento',
    desc: 'Buscar una cotización guardada para cargarla al ticket y convertirla en factura.' },
  { key: 'pagoCliente', label: 'Pago cliente', icon: 'cashIn', group: 'Caja',
    desc: 'Registrar un abono o pago de cliente a cuenta por cobrar.' },
  { key: 'pagoProveedores', label: 'Pago proveedores', icon: 'truck', group: 'Caja',
    desc: 'Registrar un pago a proveedor (egreso) desde la caja.' },
  { key: 'salida', label: 'Salida de efectivo', icon: 'cashOut', group: 'Caja',
    desc: 'Registrar una salida o retiro de efectivo de la caja (gasto, vale).' },
  { key: 'cierreCaja', label: 'Cierre de caja', icon: 'drawer', group: 'Caja',
    desc: 'Proceso contable de cierre del período: arqueo de efectivo, totales y corte de caja.' },
];

/* ---------- Stage scaler ----------------------------------- */
function useScale(ref) {
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const fit = () => {
      const pad = 28;
      const s = Math.min(
        (window.innerWidth - pad) / 1024,
        (window.innerHeight - pad) / 1024,
      );
      el.style.transform = `scale(${s})`;
    };
    fit();
    window.addEventListener('resize', fit);
    return () => window.removeEventListener('resize', fit);
  }, [ref]);
}

/* ---------- Scroll "hay más" indicator --------------------- */
function useMoreIndicator(deps = []) {
  const ref = useRef(null);
  const [more, setMore] = useState(false);
  const recompute = useCallback(() => {
    const el = ref.current;
    if (!el) { setMore(false); return; }
    setMore(el.scrollHeight - el.clientHeight - el.scrollTop > 8);
  }, []);
  useEffect(() => {
    const id = requestAnimationFrame(recompute);
    return () => cancelAnimationFrame(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);
  return { ref, more, onScroll: recompute };
}

/* ---------- Product row ------------------------------------ */
function ProductRow({ p, onAdd, thumb }) {
  return (
    <div className="pos-prow" onClick={() => onAdd(p)}>
      {thumb === 'Monograma' && (
        <div className="pos-mono" style={{ background: p.color }}>{p.mono}</div>
      )}
      {thumb === 'Foto' && (
        <img className="pos-thumb-img" src={`images/cat-${p.categoryId}.png`} alt="" draggable="false" />
      )}
      <div className="pos-pinfo">
        <div className="pos-pname">{p.name}</div>
        <div className="pos-pmeta">
          <span className="code">{p.code === '0000000000000' ? '—' : p.code.slice(-6)}</span>
          <span className="sep">·</span>
          <span>{p.catName}</span>
          {p.tax === 0 && <span className="pos-tax-badge">EXENTO</span>}
          {p.tax === 0.18 && <span className="pos-tax-badge t18">ISV 18%</span>}
        </div>
      </div>
      <div className="pos-pright">
        <div className="pos-pprice">{money(p.price)}</div>
        <div className="pos-addbtn"><I name="plus" size={22} stroke={2.2} /></div>
      </div>
    </div>
  );
}

/* ---------- Ticket line ------------------------------------ */
function TicketLine({ line, selected, onSelect, onInc, onDec, onRemove, onEditQty }) {
  const { p, qty } = line;
  const stop = (fn) => (e) => { e.stopPropagation(); fn(); };
  return (
    <div className={`pos-line ${selected ? 'sel' : ''}`} onClick={() => onSelect(p.id)}>
      <div className="pos-line-top">
        <div>
          <div className="pos-line-name">{p.name}</div>
          <div className="pos-line-unit">{money(p.price)} / {p.unit}</div>
        </div>
        <div className="pos-line-total">{money(p.price * qty)}</div>
      </div>
      <div className="pos-line-bottom">
        <div className="pos-stepper">
          <button onClick={stop(() => onDec(p.id))} aria-label="Quitar uno"><I name="minus" size={16} stroke={2.4} /></button>
          <button className="qty" onClick={stop(() => onEditQty(p.id))} aria-label="Escribir cantidad">{qty}</button>
          <button onClick={stop(() => onInc(p.id))} aria-label="Agregar uno"><I name="plus" size={16} stroke={2.4} /></button>
        </div>
        <button className="pos-line-remove" onClick={stop(() => onRemove(p.id))} aria-label="Eliminar línea">
          <I name="trash" size={16} />
        </button>
      </div>
    </div>
  );
}

/* ---------- Cash payment (campo editable + teclado) -------- */
function round2(v) { return Math.round((v + Number.EPSILON) * 100) / 100; }
function numToBuf(v) {
  const r = round2(v);
  return Number.isInteger(r) ? String(r) : String(r);
}

function CashPayment({ total, received, setReceived }) {
  const suggestions = useMemo(() => {
    const set = new Set();
    [50, 100, 200, 500].forEach((d) => set.add(Math.ceil(total / d) * d));
    return [...set].filter((v) => v > total + 0.001).sort((a, b) => a - b).slice(0, 3);
  }, [total]);

  const [buf, setBuf] = useState(received ? numToBuf(received) : '');

  const apply = (next) => {
    setBuf(next);
    const n = parseFloat(next);
    setReceived(isNaN(n) ? 0 : n);
  };

  const press = (k) => {
    if (k === 'back') return apply(buf.slice(0, -1));
    if (k === '.') {
      if (buf.includes('.')) return;
      return apply((buf || '0') + '.');
    }
    if (buf.includes('.') && buf.split('.')[1].length >= 2) return;
    if (buf === '0' && k !== '.') return apply(k);
    if (buf.replace('.', '').length >= 7) return; // tope sano
    apply(buf + k);
  };

  const enough = received >= total && received > 0;
  const change = received - total;
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', 'back'];

  return (
    <div className="pos-cash">
      <div className={`pos-recv ${buf && !enough ? 'short' : ''}`}>
        <span className="rk">Recibido del cliente</span>
        <span className="rv">
          <span className="cur">L</span>
          <span className={`amt ${buf ? '' : 'ph'}`}>{buf || '0.00'}</span>
          {buf ? (
            <button className="rclear" onClick={() => apply('')} aria-label="Borrar"><I name="x" size={16} /></button>
          ) : null}
        </span>
      </div>

      <div className="pos-quickrow">
        <button className="pos-cash-btn exact" onClick={() => apply(numToBuf(total))}>Exacto</button>
        {suggestions.map((v) => (
          <button key={v} className="pos-cash-btn" onClick={() => apply(numToBuf(v))}>{money(v).replace('L ', 'L')}</button>
        ))}
      </div>

      <div className="pos-keypad">
        {keys.map((k) => (
          <button
            key={k}
            className={`pos-key ${k === 'back' ? 'fn' : ''}`}
            onClick={() => press(k)}
            aria-label={k === 'back' ? 'Borrar dígito' : k}
          >
            {k === 'back' ? <I name="backspace" size={24} /> : k}
          </button>
        ))}
      </div>

      <div className={`pos-change ${enough ? 'ok' : ''}`}>
        <div>
          <div className="k">Cambio a entregar</div>
          {buf && !enough ? <div className="recv short">Faltan {money(total - received)}</div> : null}
        </div>
        <div className="v">{money(enough ? change : 0)}</div>
      </div>
    </div>
  );
}

/* ---------- Success ---------------------------------------- */
function SuccessView({ total, isv15, isv18, method, change, received, observaciones, customer, onClose }) {
  return (
    <div className="pos-overlay" onClick={onClose}>
      <div className="pos-modal" onClick={(e) => e.stopPropagation()} style={{ width: 440 }}>
        <div className="pos-modal-head">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 9 }}>
            <span style={{ color: 'var(--primary)' }}><I name="checkCircle" size={22} /></span>
            Venta confirmada
          </h2>
          <button className="pos-iconbtn" onClick={onClose} aria-label="Cerrar"><I name="x" size={20} /></button>
        </div>
        <div className="pos-modal-body">
          <div className="pos-success">
            <div className="ring"><I name="check" size={46} stroke={2.6} /></div>
            <h2>¡Pago recibido!</h2>
            <p>{method === 'efectivo' ? 'Pago en efectivo' : 'Pago con tarjeta aprobado'}</p>
          </div>

          <div className="pos-pay-recap">
            <div className="prr tax">
              <span className="k">ISV 15%</span>
              <span className="v">{money(isv15)}</span>
            </div>
            <div className="prr tax">
              <span className="k">ISV 18%</span>
              <span className="v">{money(isv18)}</span>
            </div>
            <div className="prr total">
              <span className="k">Total de la venta</span>
              <span className="v">{money(total)}</span>
            </div>
            {method === 'efectivo' ? (
              <>
                <div className="prr">
                  <span className="k"><I name="cash" size={15} /> Pagó con (recibido)</span>
                  <span className="v">{money(received)}</span>
                </div>
                <div className="prr change">
                  <span className="k">Cambio a entregar</span>
                  <span className="v">{money(change)}</span>
                </div>
              </>
            ) : (
              <div className="prr">
                <span className="k"><I name="card" size={15} /> Método</span>
                <span className="v">Tarjeta</span>
              </div>
            )}
          </div>

          {(customer || observaciones) && (
            <div className="pos-doc-recap">
              {customer ? <div className="dr"><span>Cliente</span><b>{customer}</b></div> : null}
              {observaciones ? <div className="dr"><span>Observaciones</span><b>{observaciones}</b></div> : null}
            </div>
          )}

          <div className="pos-modal-actions" style={{ marginTop: 20 }}>
            <button className="pos-btn flex1" onClick={onClose}>
              <I name="receipt" size={20} /> Imprimir
            </button>
            <button className="pos-btn primary flex1" onClick={onClose}>
              <I name="plus" size={20} stroke={2.4} /> Nueva venta
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ---------- Customer modal --------------------------------- */
function custInitials(name) {
  const w = name.trim().split(/\s+/).filter(Boolean);
  if (!w.length) return '?';
  if (w.length === 1) return w[0].slice(0, 2).toUpperCase();
  return (w[0][0] + w[1][0]).toUpperCase();
}
function formatRTN(v) {
  const d = v.replace(/\D/g, '').slice(0, 14);
  const parts = [d.slice(0, 4), d.slice(4, 8), d.slice(8, 14)].filter(Boolean);
  return parts.join('-');
}

function CustomerModal({ customers, current, onClose, onSelect, onCreate }) {
  const [mode, setMode] = useState('search');
  const [q, setQ] = useState('');
  const [form, setForm] = useState({ name: '', rtn: '', phone: '', address: '' });
  const [errs, setErrs] = useState({});

  const filtered = useMemo(() => {
    const s = q.trim().toLowerCase();
    if (!s) return customers;
    const sd = s.replace(/\D/g, '');
    return customers.filter(
      (c) =>
        c.name.toLowerCase().includes(s) ||
        (sd && c.rtn.replace(/\D/g, '').includes(sd)),
    );
  }, [q, customers]);

  const setF = (k) => (e) => {
    const val = k === 'rtn' ? formatRTN(e.target.value) : e.target.value;
    setForm((f) => ({ ...f, [k]: val }));
  };

  const submit = () => {
    const e = {};
    if (!form.name.trim()) e.name = 'Requerido';
    const rd = form.rtn.replace(/\D/g, '');
    if (rd && rd.length !== 14) e.rtn = 'El RTN debe tener 14 dígitos';
    setErrs(e);
    if (Object.keys(e).length) return;
    onCreate({
      name: form.name.trim(), rtn: form.rtn.trim(),
      phone: form.phone.trim(), address: form.address.trim(),
    });
  };

  return (
    <div className="pos-overlay" onClick={onClose}>
      <div className="pos-modal wide" onClick={(e) => e.stopPropagation()}>
        <div className="pos-modal-head">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            {mode === 'new' && (
              <button className="pos-iconbtn" style={{ width: 36, height: 36 }} onClick={() => setMode('search')} aria-label="Volver">
                <I name="back" size={18} />
              </button>
            )}
            {mode === 'search' ? 'Seleccionar cliente' : 'Nuevo cliente'}
          </h2>
          <button className="pos-iconbtn" onClick={onClose} aria-label="Cerrar"><I name="x" size={20} /></button>
        </div>

        {mode === 'search' ? (
          <>
            <div className="pos-modal-body" style={{ paddingBottom: 8 }}>
              <div className="pos-cust-search">
                <span className="ic"><I name="search" size={20} /></span>
                <input
                  autoFocus value={q} onChange={(e) => setQ(e.target.value)}
                  placeholder="Buscar por nombre o RTN…"
                />
              </div>
              <div className="pos-cust-list">
                <button className={`pos-cust-item ${current == null ? 'sel' : ''}`} onClick={() => onSelect(null)}>
                  <div className="cav"><I name="user" size={20} /></div>
                  <div className="cmeta">
                    <div className="cn">Consumidor Final</div>
                    <div className="cd">Venta sin identificar</div>
                  </div>
                  <span className="pick"><I name="check" size={20} stroke={2.4} /></span>
                </button>
                {filtered.map((c) => (
                  <button key={c.id} className={`pos-cust-item ${current?.id === c.id ? 'sel' : ''}`} onClick={() => onSelect(c)}>
                    <div className="cav">{custInitials(c.name)}</div>
                    <div className="cmeta">
                      <div className="cn">{c.name}</div>
                      <div className="cd">
                        <span>{c.rtn || 'Sin RTN'}</span>
                        {c.phone && <span className="sep">·</span>}
                        {c.phone && <span>{c.phone}</span>}
                      </div>
                    </div>
                    <span className="pick"><I name="check" size={20} stroke={2.4} /></span>
                  </button>
                ))}
                {filtered.length === 0 && (
                  <div className="pos-cust-empty">
                    No hay clientes para «{q}».<br />Podés crearlo como nuevo.
                  </div>
                )}
              </div>
            </div>
            <div className="pos-modal-foot">
              <button className="pos-newcust-btn" onClick={() => { setForm((f) => ({ ...f, name: q })); setMode('new'); }}>
                <I name="plus" size={20} stroke={2.2} /> Nuevo cliente
              </button>
            </div>
          </>
        ) : (
          <>
            <div className="pos-modal-body">
              <div className="pos-form-grid">
                <div className="pos-field col2">
                  <label htmlFor="nc-name">Nombre / Razón social <span className="req">*</span></label>
                  <input id="nc-name" className={errs.name ? 'bad' : ''} value={form.name} onChange={setF('name')} placeholder="Ej. Comercial La Bendición" autoFocus />
                  {errs.name && <span className="err">{errs.name}</span>}
                </div>
                <div className="pos-field">
                  <label htmlFor="nc-rtn">RTN</label>
                  <input id="nc-rtn" className={errs.rtn ? 'bad' : ''} value={form.rtn} onChange={setF('rtn')} placeholder="0801-1990-000000" inputMode="numeric" />
                  {errs.rtn ? <span className="err">{errs.rtn}</span> : <span className="hint">14 dígitos · opcional</span>}
                </div>
                <div className="pos-field">
                  <label htmlFor="nc-phone">Teléfono</label>
                  <input id="nc-phone" value={form.phone} onChange={setF('phone')} placeholder="9988-7766" inputMode="tel" />
                </div>
                <div className="pos-field col2">
                  <label htmlFor="nc-addr">Dirección</label>
                  <input id="nc-addr" value={form.address} onChange={setF('address')} placeholder="Colonia, ciudad" />
                </div>
              </div>
            </div>
            <div className="pos-modal-foot">
              <button className="pos-btn flex1" onClick={() => setMode('search')}>Cancelar</button>
              <button className="pos-btn primary flex1" onClick={submit}>
                <I name="check" size={20} stroke={2.4} /> Guardar y seleccionar
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

/* ---------- Checkout wizard (flujo guiado, estilo Swing) --- */
function CheckoutWizard({ total, vendedores, defaultVendedorId, initialObs, pedirVendedor, pedirObservaciones, onClose, onConfirm }) {
  const [step, setStep] = useState(0);
  const [vendedorId, setVendedorId] = useState(defaultVendedorId || vendedores[0].id);
  const [obs, setObs] = useState(initialObs || '');
  const [method, setMethod] = useState('efectivo');
  const [received, setReceived] = useState(0);

  // Los pasos se arman según la configuración extraída de la API.
  const steps = useMemo(() => {
    const arr = [];
    if (pedirVendedor) arr.push({ key: 'vendedor', label: 'Vendedor', icon: 'idcard' });
    if (pedirObservaciones) arr.push({ key: 'obs', label: 'Observaciones', icon: 'note' });
    arr.push({ key: 'pago', label: 'Pago', icon: 'cash' });
    return arr;
  }, [pedirVendedor, pedirObservaciones]);

  const lastIndex = steps.length - 1;
  const current = steps[Math.min(step, lastIndex)].key;
  const isLast = step >= lastIndex;

  const suggestions = useMemo(() => {
    const set = new Set();
    [50, 100, 200, 500].forEach((d) => set.add(Math.ceil(total / d) * d));
    return [...set].filter((v) => v > total + 0.001).sort((a, b) => a - b).slice(0, 3);
  }, [total]);

  const change = received - total;
  const canPay = method === 'tarjeta' || received >= total;
  const vendScroll = useMoreIndicator([step]);

  const next = () => setStep((s) => Math.min(lastIndex, s + 1));
  const back = () => (step === 0 ? onClose() : setStep((s) => s - 1));

  const finish = () =>
    onConfirm({ vendedorId, observaciones: obs.trim(), method, received });

  return (
    <div className="pos-overlay" onClick={onClose}>
      <div className="pos-modal wizard" onClick={(e) => e.stopPropagation()}>
        <div className="pos-wiz-head">
          <button className="pos-iconbtn" onClick={back} aria-label="Atrás">
            <I name="back" size={18} />
          </button>
          <span className="wtitle">Cobrar · {money(total)}</span>
          <button className="pos-iconbtn" onClick={onClose} aria-label="Cerrar"><I name="x" size={20} /></button>
        </div>

        {/* progress — oculto si solo queda el paso de pago */}
        {steps.length > 1 && (
          <div className="pos-steps">
            {steps.map((s, i) => (
              <React.Fragment key={s.key}>
                {i > 0 && <div className={`pos-step-line ${i <= step ? 'done' : ''}`} />}
                <div className={`pos-step ${i === step ? 'active' : ''} ${i < step ? 'done' : ''}`}>
                  <span className="num">{i < step ? <I name="check" size={15} stroke={2.6} /> : i + 1}</span>
                  <span className="lbl">{s.label}</span>
                </div>
              </React.Fragment>
            ))}
          </div>
        )}

        <div className="pos-wiz-body" style={steps.length === 1 ? { paddingTop: 16 } : undefined}>
          {current === 'vendedor' && (
            <div className="pos-wiz-panel">
              <div className="pos-wiz-steplabel"><I name="idcard" size={16} /> ¿Quién realiza la venta?</div>
              <div className="pos-vend-scroll">
                <div className="pos-vend-grid" ref={vendScroll.ref} onScroll={vendScroll.onScroll}>
                  {vendedores.map((v) => (
                    <button
                      key={v.id}
                      className={`pos-vend-item ${vendedorId === v.id ? 'sel' : ''}`}
                      onClick={() => setVendedorId(v.id)}
                    >
                      <span className="vav">{custInitials(v.nombreCompleto)}</span>
                      <span className="vmeta">
                        <span className="vn">{v.nombreCompleto}</span>
                        <span className="vc">Código de empleado #{v.codigoEmpleado}</span>
                      </span>
                      <span className="pick"><I name="check" size={20} stroke={2.4} /></span>
                    </button>
                  ))}
                </div>
                <div className="pos-vend-fade" data-show={vendScroll.more ? 'on' : 'off'}>
                  <span className="pos-list-more"><I name="chevDown" size={16} /> Desliza para ver más</span>
                </div>
              </div>
            </div>
          )}

          {current === 'obs' && (
            <div className="pos-wiz-panel">
              <div className="pos-wiz-steplabel"><I name="note" size={16} /> Observaciones de la venta <span style={{ color: 'var(--muted)', fontWeight: 500 }}>· opcional</span></div>
              <textarea
                className="pos-textarea" style={{ minHeight: 150 }}
                autoFocus value={obs} onChange={(e) => setObs(e.target.value)}
                placeholder="Nota para la venta (entrega, referencia, instrucciones…)"
                maxLength={240}
              />
              <div style={{ textAlign: 'right', fontSize: 12, color: 'var(--muted)', marginTop: 6 }}>{obs.length}/240</div>
            </div>
          )}

          {current === 'pago' && (
            <div className="pos-wiz-panel">
              <div className="pos-pay-total" style={{ marginBottom: 18 }}>
                <div className="k">Total a pagar</div>
                <div className="v">{money(total)}</div>
              </div>
              <div className="pos-methods">
                <button className={`pos-method ${method === 'efectivo' ? 'active' : ''}`} onClick={() => setMethod('efectivo')}>
                  <I name="cash" size={22} /> Efectivo
                </button>
                <button className={`pos-method ${method === 'tarjeta' ? 'active' : ''}`} onClick={() => { setMethod('tarjeta'); setReceived(0); }}>
                  <I name="card" size={22} /> Tarjeta
                </button>
              </div>
              {method === 'efectivo' && (
                <CashPayment total={total} received={received} setReceived={setReceived} />
              )}
            </div>
          )}
        </div>

        <div className="pos-wiz-foot">
          <button className="pos-btn" style={{ paddingInline: 22 }} onClick={back}>
            {step === 0 ? 'Cancelar' : 'Atrás'}
          </button>
          {current === 'obs' && !obs.trim() && (
            <button className="pos-wiz-skip" onClick={next}>Omitir</button>
          )}
          <div style={{ flex: 1 }} />
          {!isLast ? (
            <button className="pos-btn primary" style={{ paddingInline: 28 }} onClick={next}>
              Continuar <I name="back" size={18} style={{ transform: 'scaleX(-1)' }} />
            </button>
          ) : (
            <button className="pos-btn primary" style={{ paddingInline: 28 }} disabled={!canPay} onClick={finish}>
              <I name="check" size={20} stroke={2.4} /> Confirmar venta
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

/* ---------- Function modal (placeholder de operación) ------ */
function FunctionModal({ fn, onClose }) {
  return (
    <div className="pos-overlay" onClick={onClose}>
      <div className="pos-modal" onClick={(e) => e.stopPropagation()} style={{ width: 460 }}>
        <div className="pos-modal-head">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="pos-fnmodal-ic"><I name={fn.icon} size={20} /></span>
            {fn.label}
          </h2>
          <button className="pos-iconbtn" onClick={onClose} aria-label="Cerrar"><I name="x" size={20} /></button>
        </div>
        <div className="pos-modal-body">
          <p className="pos-fnmodal-desc">{fn.desc}</p>
          <div className="pos-fnmodal-note">
            <I name="note" size={16} />
            <span>Flujo por definir — confirmemos los campos y reglas de esta operación.</span>
          </div>
          <div className="pos-modal-actions" style={{ marginTop: 20 }}>
            <button className="pos-btn primary flex1" onClick={onClose}>Entendido</button>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ---------- More functions sheet --------------------------- */
function MoreFunctionsModal({ items, onPick, onClose }) {
  const groups = items.reduce((acc, it) => {
    (acc[it.group] = acc[it.group] || []).push(it);
    return acc;
  }, {});
  return (
    <div className="pos-overlay" onClick={onClose}>
      <div className="pos-modal" onClick={(e) => e.stopPropagation()} style={{ width: 500 }}>
        <div className="pos-modal-head">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="pos-fnmodal-ic"><I name="dots" size={20} /></span>
            Más funciones
          </h2>
          <button className="pos-iconbtn" onClick={onClose} aria-label="Cerrar"><I name="x" size={20} /></button>
        </div>
        <div className="pos-modal-body">
          {Object.keys(groups).map((g) => (
            <div key={g} className="pos-more-group">
              <div className="pos-more-glabel">{g}</div>
              <div className="pos-more-grid">
                {groups[g].map((it) => (
                  <button key={it.key} className="pos-func" onClick={() => onPick(it)}>
                    <span className="fic"><I name={it.icon} size={22} /></span>
                    <span className="flbl">{it.label}</span>
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ---------- Quantity modal (teclado numérico de cantidad) -- */
function QtyModal({ line, onClose, onSave }) {
  const { p, qty } = line;
  const [buf, setBuf] = useState(String(qty));
  const press = (k) => {
    if (k === 'back') return setBuf((b) => b.slice(0, -1));
    if (k === 'clear') return setBuf('');
    if (k === '.') { setBuf((b) => (b.includes('.') ? b : (b || '0') + '.')); return; }
    setBuf((b) => {
      if (b === '0') return k;
      if (b.replace('.', '').length >= 5) return b;
      return b + k;
    });
  };
  const val = parseFloat(buf);
  const valid = !isNaN(val) && val > 0;
  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '0', 'back'];
  const newTotal = valid ? p.price * val : 0;

  return (
    <div className="pos-overlay" onClick={onClose}>
      <div className="pos-modal" onClick={(e) => e.stopPropagation()} style={{ width: 440 }}>
        <div className="pos-modal-head">
          <h2 style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span className="pos-fnmodal-ic"><I name="hash" size={20} /></span>
            Cantidad
          </h2>
          <button className="pos-iconbtn" onClick={onClose} aria-label="Cerrar"><I name="x" size={20} /></button>
        </div>
        <div className="pos-modal-body">
          <div className="pos-qty-prod">
            <div className="qn">{p.name}</div>
            <div className="qm">{money(p.price)} / {p.unit}</div>
          </div>

          <div className="pos-recv" style={{ marginBottom: 12 }}>
            <span className="rk">Cantidad</span>
            <span className="rv">
              <span className={`amt ${buf ? '' : 'ph'}`}>{buf || '0'}</span>
              <span style={{ marginLeft: 'auto', alignSelf: 'center', fontSize: 13, color: 'var(--muted)', fontWeight: 600 }}>
                = {money(newTotal)}
              </span>
            </span>
          </div>

          <div className="pos-keypad">
            {keys.map((k) => (
              <button
                key={k}
                className={`pos-key ${k === 'back' ? 'fn' : ''}`}
                onClick={() => press(k)}
                aria-label={k === 'back' ? 'Borrar dígito' : k}
              >
                {k === 'back' ? <I name="backspace" size={24} /> : k}
              </button>
            ))}
          </div>

          <div className="pos-modal-actions">
            <button className="pos-btn flex1" onClick={onClose}>Cancelar</button>
            <button className="pos-btn primary flex1" disabled={!valid} onClick={() => onSave(p.id, round2(val))}>
              <I name="check" size={20} stroke={2.4} /> Aplicar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   App
   ============================================================ */
function POSApp() {
  const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);
  const { categories, products, cajero } = window.POS_DATA;

  const wrapRef = useRef(null);
  useScale(wrapRef);

  const [search, setSearch] = useState('');
  const [cat, setCat] = useState('all');
  const [cart, setCart] = useState([]); // [{p, qty}]
  const [customers, setCustomers] = useState(window.POS_DATA.customers);
  const [customer, setCustomer] = useState(null); // null = Consumidor Final
  const [custOpen, setCustOpen] = useState(false);
  const [vendedorId, setVendedorId] = useState(window.POS_DATA.vendedores[0].id);
  const [observaciones, setObservaciones] = useState('');
  const [wizardOpen, setWizardOpen] = useState(false);
  const [activeFn, setActiveFn] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [qtyOpen, setQtyOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const [success, setSuccess] = useState(null);
  const [toast, setToast] = useState(null);
  const [clock, setClock] = useState('');
  const correlativo = useRef(1284);
  const nextCustId = useRef(900);
  const searchRef = useRef(null);
  const toastTimer = useRef(null);
  const listRef = useRef(null);
  const [listMore, setListMore] = useState(false);
  const [hiddenCount, setHiddenCount] = useState(0);

  // reloj
  useEffect(() => {
    const tick = () => setClock(new Date().toLocaleTimeString('es-HN', { hour: '2-digit', minute: '2-digit' }));
    tick();
    const id = setInterval(tick, 10000);
    return () => clearInterval(id);
  }, []);

  const showToast = useCallback((msg) => {
    setToast(msg);
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 1600);
  }, []);

  // filtrado
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    return products.filter((p) => {
      if (cat !== 'all' && p.categoryId !== cat) return false;
      if (!q) return true;
      return p.name.toLowerCase().includes(q) || p.code.includes(q);
    });
  }, [search, cat, products]);

  const catCounts = useMemo(() => {
    const m = {};
    products.forEach((p) => { m[p.categoryId] = (m[p.categoryId] || 0) + 1; });
    return m;
  }, [products]);

  // Indicador "hay más": mide cuántas filas quedan bajo el área visible.
  const recomputeMore = useCallback(() => {
    const el = listRef.current;
    if (!el) return;
    const remaining = el.scrollHeight - el.clientHeight - el.scrollTop;
    const rowH = 84; // alto aproximado de fila (cómodo)
    const more = remaining > 8;
    setListMore(more);
    setHiddenCount(more ? Math.max(1, Math.round(remaining / rowH)) : 0);
  }, []);

  const onListScroll = recomputeMore;

  useEffect(() => {
    // recalcular al cambiar filtro/categoría y al montar
    const id = requestAnimationFrame(() => {
      if (listRef.current) listRef.current.scrollTop = 0;
      recomputeMore();
    });
    return () => cancelAnimationFrame(id);
  }, [filtered, recomputeMore]);

  // carrito
  const addProduct = useCallback((p) => {
    setCart((prev) => {
      const i = prev.findIndex((l) => l.p.id === p.id);
      if (i === -1) return [...prev, { p, qty: 1 }];
      const next = [...prev];
      next[i] = { ...next[i], qty: next[i].qty + 1 };
      return next;
    });
    setSelectedId(p.id); // el último agregado queda seleccionado
    setSearch(''); // limpiar la búsqueda al agregar al ticket
    showToast(`${p.name} agregado`);
  }, [showToast]);

  const inc = (id) => { setSelectedId(id); setCart((prev) => prev.map((l) => l.p.id === id ? { ...l, qty: l.qty + 1 } : l)); };
  const dec = (id) => { setSelectedId(id); setCart((prev) => prev.flatMap((l) => l.p.id === id ? (l.qty <= 1 ? [] : [{ ...l, qty: l.qty - 1 }]) : [l])); };
  const setQty = (id, q) => setCart((prev) => prev.map((l) => l.p.id === id ? { ...l, qty: q } : l));
  const removeLine = (id) => setCart((prev) => {
    const next = prev.filter((l) => l.p.id !== id);
    if (selectedId === id) setSelectedId(next.length ? next[next.length - 1].p.id : null);
    return next;
  });
  const clearCart = () => { setCart([]); setCustomer(null); setObservaciones(''); setSelectedId(null); };

  // Línea seleccionada (por defecto la última del ticket).
  const selectedLine = cart.find((l) => l.p.id === selectedId) || cart[cart.length - 1] || null;

  // Funciones de caja
  const handleFunc = (fn) => {
    if (fn.key === 'cantidad') {
      if (!selectedLine) { showToast('Agrega un producto al ticket'); return; }
      setQtyOpen(true);
      return;
    }
    setActiveFn(fn);
  };

  // clientes
  const selectCustomer = (c) => { setCustomer(c); setCustOpen(false); };
  const createCustomer = (data) => {
    const c = { id: nextCustId.current++, ...data };
    setCustomers((prev) => [c, ...prev]);
    setCustomer(c);
    setCustOpen(false);
    showToast('Cliente creado y asignado');
  };

  // barcode: enter exacto en search agrega producto si match único de código
  const onSearchKey = (e) => {
    if (e.key !== 'Enter') return;
    const q = search.trim();
    if (!q) return;
    const byCode = products.find((p) => p.code === q || p.code.endsWith(q));
    if (byCode) { addProduct(byCode); setSearch(''); }
    else if (filtered.length === 1) { addProduct(filtered[0]); setSearch(''); }
  };

  // totales (precios IVA incluido al público)
  const totals = useMemo(() => {
    let total = 0, isv15 = 0, isv18 = 0, exento = 0, gravado = 0, items = 0;
    cart.forEach(({ p, qty }) => {
      const lt = p.price * qty;
      total += lt; items += qty;
      if (p.tax > 0) {
        const base = lt / (1 + p.tax);
        gravado += base;
        const imp = lt - base;
        if (p.tax === 0.18) isv18 += imp; else isv15 += imp;
      } else { exento += lt; }
    });
    const isv = isv15 + isv18;
    return { total, isv, isv15, isv18, exento, gravado, subtotal: gravado + exento, items };
  }, [cart]);

  const handleConfirm = ({ method, received, vendedorId: vId, observaciones: obsOverride }) => {
    const n = correlativo.current++;
    const factura = `000-001-01-${String(n).padStart(8, '0')}`;
    const finalVendId = vId != null ? vId : vendedorId;
    const finalObs = obsOverride != null ? obsOverride : observaciones;
    const vName = (window.POS_DATA.vendedores.find((v) => v.id === finalVendId) || {}).nombreCompleto || '—';
    // sincronizar selección hecha en el asistente con el ticket
    if (vId != null) setVendedorId(vId);
    setWizardOpen(false);
    setSuccess({
      doc: { factura, cai: 'A1B2-9F3C-…-2026' },
      total: totals.total,
      isv15: totals.isv15,
      isv18: totals.isv18,
      method,
      received: method === 'efectivo' ? received : totals.total,
      change: method === 'efectivo' ? received - totals.total : 0,
      vendedor: vName,
      observaciones: finalObs,
      customer: customer ? customer.name : null,
    });
    setCart([]);
    setCustomer(null);
    setObservaciones('');
  };

  const compact = t.density === 'Compacto';
  const thumb = t.thumb || 'Monograma';
  const catsRight = true; // Categorías siempre en riel lateral.
  // Config extraída de la API (window.POS_CONFIG). El Tweak permite
  // simular distintas respuestas del backend para revisión.
  const pedirVendedor = t.cfgVendedor;
  const pedirObservaciones = t.cfgObservaciones;

  return (
    <div className="pos-stage">
      <div className="pos-screen-wrap" ref={wrapRef}>
        <div
          className={`pos-screen ${t.dark ? 'dark' : ''}`}
          data-density={compact ? 'compact' : 'comfortable'}
          data-accent="green"
        >
          {/* ---------- Top bar ---------- */}
          <div className="pos-topbar">
            <div className="pos-brand">
              <div className="pos-brand-badge">POS</div>
              <div className="pos-brand-meta">
                <span className="t">Datatec</span>
                <span className="s">Facturación</span>
              </div>
            </div>
            <div className="pos-caja">
              <div className="pair">
                <span className="k">Caja</span>
                <span className="v">{cajero.caja}</span>
              </div>
              <div className="pair">
                <span className="k">Sucursal</span>
                <span className="v">Tegucigalpa · Centro</span>
              </div>
            </div>
            <div className="pos-topbar-spacer" />
            <div className="pos-clock">{clock}</div>
            <button
              className="pos-iconbtn"
              onClick={() => setTweak('dark', !t.dark)}
              aria-label="Cambiar tema"
              title={t.dark ? 'Modo claro' : 'Modo oscuro'}
            >
              <I name={t.dark ? 'sun' : 'moon'} size={21} />
            </button>
            <div className="pos-user">
              <div className="av">{cajero.initials}</div>
              <div>
                <div className="nm">{cajero.name}</div>
                <div className="rl">{cajero.role}</div>
              </div>
            </div>
          </div>

          {/* ---------- Body ---------- */}
          <div className="pos-body">
            {/* Catalog */}
            <div className="pos-catalog">
              <div className="pos-search">
                <span className="ic"><I name="search" size={21} /></span>
                <input
                  ref={searchRef}
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  onKeyDown={onSearchKey}
                  placeholder="Buscar por nombre o escanear código de barras…"
                  autoFocus
                />
                <span className="scan"><I name="barcode" size={18} /> Escáner activo</span>
              </div>

              <div className="pos-catalog-main">
                {catsRight && (
                  <div className="pos-cat-rail">
                    <button className={`pos-cat-btn ${cat === 'all' ? 'active' : ''}`} onClick={() => setCat('all')}>
                      <I name="grid" size={16} />
                      <span className="nm">Todas</span>
                      <span className="ct">{products.length}</span>
                    </button>
                    {categories.map((c) => (
                      <button key={c.id} className={`pos-cat-btn ${cat === c.id ? 'active' : ''}`} onClick={() => setCat(c.id)}>
                        <span className="dot" style={{ background: c.color }} />
                        <span className="nm">{c.name}</span>
                        <span className="ct">{catCounts[c.id] || 0}</span>
                      </button>
                    ))}
                  </div>
                )}

                <div className="pos-listcol">
                  <div className="pos-list-head">
                    <span className="lh-title">
                      {cat === 'all' ? 'Todos los productos' : (categories.find((c) => c.id === cat) || {}).name}
                    </span>
                    <span className="lh-count">
                      {filtered.length} {filtered.length === 1 ? 'producto' : 'productos'}
                    </span>
                  </div>
                  <div className="pos-list-scroll">
                    <div className="pos-list" ref={listRef} onScroll={onListScroll}>
                      {filtered.length === 0 ? (
                        <div className="pos-empty">
                          <I name="search" size={34} />
                          <div className="big">Sin resultados</div>
                          <div className="sm">No hay productos para «{search}».</div>
                        </div>
                      ) : (
                        filtered.map((p) => <ProductRow key={p.id} p={p} onAdd={addProduct} thumb={thumb} />)
                      )}
                    </div>
                    <div className="pos-list-fade" data-show={listMore ? 'on' : 'off'}>
                      <span className="pos-list-more"><I name="chevDown" size={16} /> {hiddenCount} más</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="pos-funcbar">
                {FUNC_BUTTONS.map((fn) => (
                  <button
                    key={fn.key}
                    className={`pos-func ${fn.kind === 'caja' ? 'caja' : ''}`}
                    onClick={() => handleFunc(fn)}
                  >
                    <span className="fic"><I name={fn.icon} size={22} /></span>
                    <span className="flbl">{fn.label}</span>
                  </button>
                ))}
                <button className="pos-func more" onClick={() => setMoreOpen(true)}>
                  <span className="fic"><I name="dots" size={22} /></span>
                  <span className="flbl">Más</span>
                </button>
              </div>
            </div>

            {/* Ticket */}
            <div className="pos-ticket">
              <div className="pos-ticket-head">
                <div className="pos-ticket-title-row">
                  <div className="pos-ticket-title">
                    <I name="receipt" size={20} /> Ticket
                    {totals.items > 0 && <span className="pos-ticket-count">{totals.items}</span>}
                  </div>
                  {cart.length > 0 && (
                    <button className="pos-clearbtn" onClick={clearCart}>
                      <I name="trash" size={15} /> Vaciar
                    </button>
                  )}
                </div>
                <div className="pos-customer" onClick={() => setCustOpen(true)}>
                  <div className="cav">
                    {customer ? custInitials(customer.name) : <I name="user" size={20} />}
                  </div>
                  <div className="cmeta">
                    <div className="cn">{customer ? customer.name : 'Consumidor Final'}</div>
                    <div className="cr">{customer ? (customer.rtn || 'Sin RTN') : 'Tocar para asignar cliente'}</div>
                  </div>
                  {customer ? (
                    <button
                      className="cclear"
                      onClick={(e) => { e.stopPropagation(); setCustomer(null); }}
                      aria-label="Quitar cliente"
                    >
                      <I name="x" size={18} />
                    </button>
                  ) : (
                    <I name="back" size={18} style={{ transform: 'scaleX(-1)', color: 'var(--muted)' }} />
                  )}
                </div>

              </div>

              <div className="pos-lines">
                {cart.length === 0 ? (
                  <div className="pos-empty">
                    <I name="receipt" size={38} />
                    <div className="big">Ticket vacío</div>
                    <div className="sm">Toca un producto o escanea un código para comenzar.</div>
                  </div>
                ) : (
                  cart.map((line) => (
                    <TicketLine
                      key={line.p.id}
                      line={line}
                      selected={selectedLine && selectedLine.p.id === line.p.id}
                      onSelect={setSelectedId}
                      onInc={inc}
                      onDec={dec}
                      onRemove={removeLine}
                      onEditQty={(id) => { setSelectedId(id); setQtyOpen(true); }}
                    />
                  ))
                )}
              </div>

              <div className="pos-totals">
                <div className="pos-trow">
                  <span className="l">Subtotal</span>
                  <span className="v">{money(totals.subtotal)}</span>
                </div>
                {totals.exento > 0 && (
                  <div className="pos-trow">
                    <span className="l">Importe exento</span>
                    <span className="v">{money(totals.exento)}</span>
                  </div>
                )}
                {totals.isv15 > 0 && (
                  <div className="pos-trow">
                    <span className="l">ISV 15%</span>
                    <span className="v">{money(totals.isv15)}</span>
                  </div>
                )}
                {totals.isv18 > 0 && (
                  <div className="pos-trow">
                    <span className="l">ISV 18%</span>
                    <span className="v">{money(totals.isv18)}</span>
                  </div>
                )}
                <div className="pos-trow grand">
                  <span className="l">Total</span>
                  <span className="v">{money(totals.total)}</span>
                </div>
                <button className="pos-pay" disabled={cart.length === 0} onClick={() => setWizardOpen(true)}>
                  <I name="cash" size={24} /> Cobrar <span className="amt">{money(totals.total)}</span>
                </button>
              </div>
            </div>
          </div>

          {/* ---------- Overlays ---------- */}
          {custOpen && (
            <CustomerModal
              customers={customers}
              current={customer}
              onClose={() => setCustOpen(false)}
              onSelect={selectCustomer}
              onCreate={createCustomer}
            />
          )}
          {wizardOpen && (
            <CheckoutWizard
              total={totals.total}
              vendedores={window.POS_DATA.vendedores}
              defaultVendedorId={vendedorId}
              initialObs={observaciones}
              pedirVendedor={pedirVendedor}
              pedirObservaciones={pedirObservaciones}
              onClose={() => setWizardOpen(false)}
              onConfirm={handleConfirm}
            />
          )}
          {activeFn && (
            <FunctionModal fn={activeFn} onClose={() => setActiveFn(null)} />
          )}
          {qtyOpen && selectedLine && (
            <QtyModal
              line={selectedLine}
              onClose={() => setQtyOpen(false)}
              onSave={(id, q) => { setQty(id, q); setQtyOpen(false); }}
            />
          )}
          {moreOpen && (
            <MoreFunctionsModal
              items={FUNC_MORE}
              onClose={() => setMoreOpen(false)}
              onPick={(it) => { setMoreOpen(false); setActiveFn(it); }}
            />
          )}
          {success && (
            <SuccessView {...success} onClose={() => setSuccess(null)} />
          )}
          {toast && (
            <div className="pos-toast"><I name="checkCircle" size={18} /> {toast}</div>
          )}
        </div>
      </div>

      {/* ---------- Tweaks ---------- */}
      <TweaksPanel>
        <TweakSection label="Apariencia" />
        <TweakToggle label="Modo oscuro" value={t.dark} onChange={(v) => setTweak('dark', v)} />
        <TweakSection label="Densidad del catálogo" />
        <TweakRadio
          label="Filas" value={t.density}
          options={['Cómodo', 'Compacto']}
          onChange={(v) => setTweak('density', v)}
        />
        <TweakRadio
          label="Miniatura" value={t.thumb}
          options={['Monograma', 'Foto', 'Ninguna']}
          onChange={(v) => setTweak('thumb', v)}
        />
        <TweakSection label="Configuración (simula API)" />
        <TweakToggle label="Pedir vendedor" value={t.cfgVendedor} onChange={(v) => setTweak('cfgVendedor', v)} />
        <TweakToggle label="Pedir observaciones" value={t.cfgObservaciones} onChange={(v) => setTweak('cfgObservaciones', v)} />
      </TweaksPanel>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<POSApp />);
