import React, { useState, useRef } from 'react';
import { api } from '../services/api';
import { Upload, CheckCircle, AlertCircle, Users, ShoppingBag, X, FileText, RotateCcw } from 'lucide-react';

const SAMPLE_CUSTOMERS = [
  { name: "Alice Smith", email: "alice@example.com", phone: "+15550101", city: "Seattle", tier: "Gold", consent: true },
  { name: "Bob Johnson", email: "bob@example.com", phone: "+15550102", city: "Boston", tier: "Silver", consent: true },
  { name: "Diana Prince", email: "diana@example.com", phone: "+15550104", city: "Los Angeles", tier: "Gold", consent: true },
  { name: "Evan Wright", email: "evan@example.com", phone: "+15550105", city: "Boston", tier: "Silver", consent: true },
  { name: "Fiona Gallagher", email: "fiona@example.com", phone: "+15550106", city: "Chicago", tier: "Gold", consent: true },
  { name: "George Costanza", email: "george@example.com", phone: "+15550107", city: "New York", tier: "Bronze", consent: true },
  { name: "Hannah Lee", email: "hannah@example.com", phone: "+15550108", city: "Seattle", tier: "Silver", consent: true },
  { name: "Ivan Torres", email: "ivan@example.com", phone: "+15550109", city: "Chicago", tier: "Gold", consent: true },
  { name: "Julia Chen", email: "julia@example.com", phone: "+15550110", city: "San Francisco", tier: "Gold", consent: true },
  { name: "Kyle Park", email: "kyle@example.com", phone: "+15550111", city: "Boston", tier: "Bronze", consent: false },
];

const SAMPLE_ORDERS = [
  { customer_email: "alice@example.com", amount: 120.0, channel: "online", order_date: "2026-04-10T12:00:00", items: [{ product_id: "p1", category: "Coffee Beans", quantity: 2, price: 45.0 }, { product_id: "p2", category: "Filters", quantity: 1, price: 30.0 }] },
  { customer_email: "bob@example.com", amount: 65.5, channel: "retail", order_date: "2026-04-15T15:30:00", items: [{ product_id: "p3", category: "Mugs", quantity: 2, price: 20.0 }] },
  { customer_email: "diana@example.com", amount: 350.0, channel: "online", order_date: "2026-03-01T09:00:00", items: [{ product_id: "p4", category: "Coffee Machines", quantity: 1, price: 350.0 }] },
  { customer_email: "evan@example.com", amount: 90.0, channel: "online", order_date: "2026-04-28T10:45:00", items: [{ product_id: "p1", category: "Coffee Beans", quantity: 2, price: 45.0 }] },
  { customer_email: "fiona@example.com", amount: 250.0, channel: "online", order_date: "2026-02-15T11:00:00", items: [{ product_id: "p1", category: "Coffee Beans", quantity: 4, price: 50.0 }] },
  { customer_email: "hannah@example.com", amount: 78.0, channel: "online", order_date: "2026-04-20T14:00:00", items: [{ product_id: "p5", category: "Coffee Beans", quantity: 1, price: 45.0 }, { product_id: "p2", category: "Filters", quantity: 1, price: 33.0 }] },
  { customer_email: "ivan@example.com", amount: 500.0, channel: "online", order_date: "2026-02-01T08:00:00", items: [{ product_id: "p4", category: "Coffee Machines", quantity: 1, price: 500.0 }] },
  { customer_email: "julia@example.com", amount: 95.0, channel: "online", order_date: "2026-05-01T09:30:00", items: [{ product_id: "p1", category: "Coffee Beans", quantity: 2, price: 47.5 }] },
];

function ResultBadge({ result, label }) {
  if (!result) return null;
  const hasErrors = result.errors && result.errors.length > 0;
  return (
    <div className={`p-4 rounded-lg border ${hasErrors ? 'bg-amber-500/10 border-amber-500/20' : 'bg-emerald-500/10 border-emerald-500/20'}`}>
      <div className="flex items-center gap-2 mb-2">
        {hasErrors ? <AlertCircle className="h-4 w-4 text-amber-400" /> : <CheckCircle className="h-4 w-4 text-emerald-400" />}
        <span className={`text-sm font-semibold ${hasErrors ? 'text-amber-300' : 'text-emerald-300'}`}>
          {label} — {result.addedCount} added{result.updatedCount > 0 ? `, ${result.updatedCount} updated` : ''}
        </span>
      </div>
      {hasErrors && (
        <ul className="mt-2 space-y-1">
          {result.errors.slice(0, 3).map((e, i) => (
            <li key={i} className="text-xs text-zinc-400 font-mono">Row {e.row}: {e.message}</li>
          ))}
          {result.errors.length > 3 && <li className="text-xs text-zinc-500">...and {result.errors.length - 3} more</li>}
        </ul>
      )}
    </div>
  );
}

export default function Ingestion() {
  const [customerResult, setCustomerResult] = useState(null);
  const [orderResult, setOrderResult] = useState(null);
  const [loading, setLoading] = useState({ customers: false, orders: false, reset: false });
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);

  const loadSeedData = async () => {
    setLoading({ customers: true, orders: false, reset: false });
    try {
      const custResult = await api.ingestCustomers(SAMPLE_CUSTOMERS);
      setCustomerResult(custResult);
      setLoading({ customers: false, orders: true, reset: false });
      const ordResult = await api.ingestOrders(SAMPLE_ORDERS);
      setOrderResult(ordResult);
    } catch (e) {
      setCustomerResult({ success: false, addedCount: 0, errors: [{ row: 0, message: e.message }] });
    } finally {
      setLoading({ customers: false, orders: false, reset: false });
    }
  };

  const handleReset = async () => {
    setLoading({ customers: false, orders: false, reset: true });
    try {
      await api.resetDatabase();
      setCustomerResult(null);
      setOrderResult(null);
    } finally {
      setLoading({ customers: false, orders: false, reset: false });
    }
  };

  const handleFileDrop = async (e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer?.files?.[0] || e.target?.files?.[0];
    if (!file) return;

    const text = await file.text();
    try {
      const data = JSON.parse(text);
      if (Array.isArray(data) && data[0]?.customer_email !== undefined) {
        setLoading({ customers: false, orders: true, reset: false });
        const res = await api.ingestOrders(data);
        setOrderResult(res);
      } else if (Array.isArray(data)) {
        setLoading({ customers: true, orders: false, reset: false });
        const res = await api.ingestCustomers(data);
        setCustomerResult(res);
      }
    } catch {
      // Try CSV parsing
      const lines = text.trim().split('\n');
      const headers = lines[0].split(',').map(h => h.trim());
      const rows = lines.slice(1).map(line => {
        const vals = line.split(',');
        return Object.fromEntries(headers.map((h, i) => [h, vals[i]?.trim()]));
      });
      if (headers.includes('email') || headers.includes('name')) {
        setLoading({ customers: true, orders: false, reset: false });
        const res = await api.ingestCustomers(rows);
        setCustomerResult(res);
      }
    } finally {
      setLoading({ customers: false, orders: false, reset: false });
    }
  };

  const isLoading = loading.customers || loading.orders || loading.reset;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight text-white">Data Ingestion</h1>
        <p className="text-zinc-400 text-sm mt-1">Upload customer and order data with automatic deduplication.</p>
      </div>

      {/* Upload Zone */}
      <div
        className={`glass-panel p-8 border-2 border-dashed text-center cursor-pointer transition-all duration-300 ${dragOver ? 'border-indigo-500 bg-indigo-500/5' : 'border-zinc-700 hover:border-zinc-600'}`}
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleFileDrop}
        onClick={() => fileInputRef.current?.click()}
      >
        <input ref={fileInputRef} type="file" accept=".json,.csv" className="hidden" onChange={handleFileDrop} />
        <Upload className={`h-10 w-10 mx-auto mb-3 transition-colors ${dragOver ? 'text-indigo-400' : 'text-zinc-600'}`} />
        <p className="text-white font-semibold">Drop your CSV or JSON file here</p>
        <p className="text-zinc-500 text-sm mt-1">Auto-detected as customers or orders. Click to browse.</p>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Seed Data Card */}
        <div className="glass-panel p-5">
          <div className="flex items-start gap-4 mb-4">
            <div className="p-2 bg-indigo-500/10 border border-indigo-500/20 rounded-lg text-indigo-400 flex-shrink-0">
              <FileText className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-white font-semibold text-sm">Load Retail Seed Dataset</h3>
              <p className="text-zinc-500 text-xs mt-1">Loads 10 customers + 8 orders (Coffee Beans, Mugs, Machines) with historical purchase dates for full AI demonstration.</p>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3 text-xs mb-4">
            <div className="bg-dark-800/50 rounded-lg p-3 border border-zinc-800">
              <div className="flex items-center gap-1.5 text-zinc-400 mb-1"><Users className="h-3 w-3" /> Customers</div>
              <span className="text-white font-bold text-lg">10</span>
            </div>
            <div className="bg-dark-800/50 rounded-lg p-3 border border-zinc-800">
              <div className="flex items-center gap-1.5 text-zinc-400 mb-1"><ShoppingBag className="h-3 w-3" /> Orders</div>
              <span className="text-white font-bold text-lg">8</span>
            </div>
          </div>
          <button
            onClick={loadSeedData}
            disabled={isLoading}
            className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed text-white text-sm font-semibold rounded-lg transition-all active:scale-95 flex items-center justify-center gap-2"
          >
            {(loading.customers || loading.orders) ? (
              <>
                <div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                {loading.customers ? 'Ingesting Customers...' : 'Ingesting Orders...'}
              </>
            ) : (
              <>
                <Upload className="h-4 w-4" />
                Load Seed Data
              </>
            )}
          </button>
        </div>

        {/* Reset */}
        <div className="glass-panel p-5">
          <div className="flex items-start gap-4 mb-4">
            <div className="p-2 bg-rose-500/10 border border-rose-500/20 rounded-lg text-rose-400 flex-shrink-0">
              <RotateCcw className="h-5 w-5" />
            </div>
            <div>
              <h3 className="text-white font-semibold text-sm">Reset All Data</h3>
              <p className="text-zinc-500 text-xs mt-1">Clears all customers and orders. Segments and campaigns are preserved. Use before re-seeding a fresh demo.</p>
            </div>
          </div>
          <div className="p-3 bg-rose-500/5 border border-rose-500/20 rounded-lg mb-4">
            <p className="text-rose-400 text-xs"><strong>Warning:</strong> This action cannot be undone. All ingested data will be wiped.</p>
          </div>
          <button
            onClick={handleReset}
            disabled={isLoading}
            className="w-full py-2.5 px-4 bg-rose-600/20 hover:bg-rose-600/30 border border-rose-500/30 disabled:opacity-50 text-rose-300 text-sm font-semibold rounded-lg transition-all active:scale-95 flex items-center justify-center gap-2"
          >
            {loading.reset ? (
              <><div className="h-4 w-4 border-2 border-rose-400/30 border-t-rose-400 rounded-full animate-spin" /> Resetting...</>
            ) : (
              <><RotateCcw className="h-4 w-4" /> Reset Database</>
            )}
          </button>
        </div>
      </div>

      {/* Ingestion Results */}
      {(customerResult || orderResult) && (
        <div className="glass-panel p-5 space-y-3">
          <h3 className="text-white font-semibold text-sm border-b border-zinc-800 pb-3">Ingestion Summary</h3>
          <ResultBadge result={customerResult} label="Customers" />
          <ResultBadge result={orderResult} label="Orders" />
        </div>
      )}

      {/* Schema Reference */}
      <div className="glass-panel p-5">
        <h3 className="text-white font-semibold text-sm mb-3">Schema Reference</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <p className="text-zinc-500 text-xs font-semibold uppercase tracking-widest mb-2">Customer JSON</p>
            <pre className="bg-dark-950 rounded-lg p-3 text-[11px] text-zinc-300 font-mono overflow-x-auto border border-zinc-800">
{`{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "phone": "+15550101",
  "city": "Seattle",
  "tier": "Gold",
  "consent": true
}`}
            </pre>
          </div>
          <div>
            <p className="text-zinc-500 text-xs font-semibold uppercase tracking-widest mb-2">Order JSON</p>
            <pre className="bg-dark-950 rounded-lg p-3 text-[11px] text-zinc-300 font-mono overflow-x-auto border border-zinc-800">
{`{
  "customer_email": "alice@example.com",
  "amount": 120.0,
  "channel": "online",
  "order_date": "2026-05-10T12:00:00",
  "items": [
    { "category": "Coffee Beans",
      "quantity": 2, "price": 45.0 }
  ]
}`}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
}
