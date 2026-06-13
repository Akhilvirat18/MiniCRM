import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import {
  Send, Plus, ChevronRight, BarChart2, Users, 
  MessageSquare, Layers, Play, CheckCircle, Clock
} from 'lucide-react';

const CHANNELS = [
  { id: 'sms',      label: 'SMS',      icon: '📱', desc: 'High open rates, fast reach' },
  { id: 'email',    label: 'Email',    icon: '📧', desc: 'Rich content, lower cost' },
  { id: 'whatsapp', label: 'WhatsApp', icon: '💬', desc: 'Conversational, high CTR' },
  { id: 'rcs',      label: 'RCS',      icon: '✨', desc: 'Rich cards, interactive buttons' },
];

const TONES = ['Friendly', 'Urgent', 'Sophisticated', 'Humorous'];

function StatusBadge({ status }) {
  const config = {
    Draft: 'bg-zinc-700/50 text-zinc-300 border-zinc-600',
    Sending: 'bg-blue-500/10 text-blue-300 border-blue-500/20 animate-pulse',
    Sent: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20',
  };
  return (
    <span className={`text-[10px] px-2 py-0.5 rounded-full border font-semibold uppercase tracking-wider ${config[status] || config.Draft}`}>
      {status}
    </span>
  );
}

function MetricBar({ label, value, max, color }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div>
      <div className="flex justify-between text-xs mb-1">
        <span className="text-zinc-500">{label}</span>
        <span className="text-white font-semibold">{value} <span className="text-zinc-500">({pct}%)</span></span>
      </div>
      <div className="h-1.5 bg-dark-950 rounded-full overflow-hidden">
        <div className={`h-full rounded-full transition-all duration-700 ${color}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}

export default function CampaignManager() {
  const [campaigns, setCampaigns] = useState([]);
  const [segments, setSegments] = useState([]);
  const [selectedCampaign, setSelectedCampaign] = useState(null);
  const [creating, setCreating] = useState(false);
  const [sending, setSending] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    name: '',
    segmentId: '',
    channel: 'sms',
    tone: 'Friendly',
    messageTemplate: '',
    variantBTemplate: '',
    enableAB: false,
  });

  const loadData = async () => {
    try {
      const [camps, segs] = await Promise.all([api.getCampaigns(), api.getSegments()]);
      setCampaigns(camps);
      setSegments(segs);
      if (selectedCampaign) {
        const refreshed = camps.find(c => c.id === selectedCampaign.id);
        if (refreshed) setSelectedCampaign(refreshed);
      }
    } catch (e) { console.error(e); }
  };

  useEffect(() => {
    loadData();
    const timer = setInterval(loadData, 3000);
    return () => clearInterval(timer);
  }, []);

  const handleCreate = async () => {
    if (!form.name || !form.segmentId || !form.messageTemplate) return;
    setCreating(true);
    try {
      const variants = form.enableAB && form.variantBTemplate ? [
        { variantId: 'A', template: form.messageTemplate },
        { variantId: 'B', template: form.variantBTemplate },
      ] : [];
      const campaign = await api.createCampaign({
        name: form.name,
        segmentId: form.segmentId,
        channel: form.channel,
        messageTemplate: form.messageTemplate,
        variants,
      });
      setShowForm(false);
      setForm({ name: '', segmentId: '', channel: 'sms', tone: 'Friendly', messageTemplate: '', variantBTemplate: '', enableAB: false });
      await loadData();
      setSelectedCampaign(campaign);
    } finally {
      setCreating(false);
    }
  };

  const handleSend = async (id) => {
    setSending(id);
    try {
      await api.sendCampaign(id);
      await loadData();
    } catch (e) {
      alert(e.response?.data?.message || e.message);
    } finally {
      setSending(null);
    }
  };

  const seg = segments.find(s => s.id === selectedCampaign?.segmentId);
  const m = selectedCampaign?.metrics;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight text-white">Campaign Manager</h1>
          <p className="text-zinc-400 text-sm mt-1">Create, configure, and launch marketing campaigns.</p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold rounded-lg transition-all active:scale-95"
        >
          <Plus className="h-4 w-4" />
          New Campaign
        </button>
      </div>

      {/* Create Form */}
      {showForm && (
        <div className="glass-panel p-6 space-y-5 border-indigo-500/20 bg-gradient-to-br from-indigo-900/20 to-dark-900 shadow-[0_0_30px_rgba(99,102,241,0.1)] relative overflow-hidden">
          <div className="absolute -top-20 -right-20 w-40 h-40 bg-indigo-500/20 rounded-full blur-3xl" />
          <h3 className="text-white font-bold text-sm flex items-center gap-2 relative z-10"><Plus className="h-4 w-4 text-indigo-400" />Create Campaign</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-zinc-400 text-xs font-semibold mb-1.5">Campaign Name</label>
              <input className="w-full bg-dark-950 border border-zinc-800 focus:border-indigo-500 rounded-lg px-3 py-2.5 text-sm text-white placeholder-zinc-600 outline-none transition-colors"
                placeholder="e.g. Coffee Lovers Win-back" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div>
              <label className="block text-zinc-400 text-xs font-semibold mb-1.5">Target Segment</label>
              <select className="w-full bg-dark-950 border border-zinc-800 focus:border-indigo-500 rounded-lg px-3 py-2.5 text-sm text-white outline-none transition-colors"
                value={form.segmentId} onChange={e => setForm(f => ({ ...f, segmentId: e.target.value }))}>
                <option value="">Select a segment...</option>
                {segments.map(s => <option key={s.id} value={s.id}>{s.name} ({s.previewCount} shoppers)</option>)}
              </select>
            </div>
          </div>

          {/* Channel picker */}
          <div>
            <label className="block text-zinc-400 text-xs font-semibold mb-2">Delivery Channel</label>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              {CHANNELS.map(ch => (
                <button key={ch.id} onClick={() => setForm(f => ({ ...f, channel: ch.id }))}
                  className={`p-3 rounded-lg border text-left transition-all ${form.channel === ch.id ? 'border-indigo-500 bg-indigo-500/10' : 'border-zinc-800 bg-dark-800/30 hover:border-zinc-700'}`}>
                  <div className="text-lg mb-1">{ch.icon}</div>
                  <div className="text-white text-xs font-semibold">{ch.label}</div>
                  <div className="text-zinc-500 text-[10px] hidden sm:block">{ch.desc}</div>
                </button>
              ))}
            </div>
          </div>

          {/* Message Template */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-zinc-400 text-xs font-semibold">Message Template</label>
              <div className="flex gap-1.5 flex-wrap">
                {['{{name}}', '{{city}}', '{{last_purchased_category}}'].map(tok => (
                  <button key={tok} onClick={() => setForm(f => ({ ...f, messageTemplate: f.messageTemplate + tok }))}
                    className="text-[10px] px-2 py-0.5 bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 rounded font-mono hover:bg-indigo-500/20 transition-colors">
                    {tok}
                  </button>
                ))}
              </div>
            </div>
            <textarea className="w-full bg-dark-950 border border-zinc-800 focus:border-indigo-500 rounded-lg px-3 py-2.5 text-sm text-white placeholder-zinc-600 outline-none transition-colors font-mono resize-none"
              rows={3} placeholder="Hi {{name}}, we have a special offer just for you..."
              value={form.messageTemplate} onChange={e => setForm(f => ({ ...f, messageTemplate: e.target.value }))} />
          </div>

          {/* A/B Toggle */}
          <div>
            <label className="flex items-center gap-3 cursor-pointer">
              <div className={`w-10 h-5 rounded-full transition-colors relative ${form.enableAB ? 'bg-indigo-600' : 'bg-zinc-700'}`}
                onClick={() => setForm(f => ({ ...f, enableAB: !f.enableAB }))}>
                <div className={`absolute top-0.5 w-4 h-4 bg-white rounded-full shadow transition-transform ${form.enableAB ? 'translate-x-5' : 'translate-x-0.5'}`} />
              </div>
              <span className="text-zinc-300 text-sm font-medium">Enable A/B Split Test (50/50 split)</span>
            </label>
            {form.enableAB && (
              <div className="mt-3">
                <label className="block text-zinc-400 text-xs font-semibold mb-1.5">Variant B Template</label>
                <textarea className="w-full bg-dark-950 border border-zinc-800 focus:border-indigo-500 rounded-lg px-3 py-2.5 text-sm text-white placeholder-zinc-600 outline-none transition-colors font-mono resize-none"
                  rows={2} placeholder="Alternative message for 50% of audience..."
                  value={form.variantBTemplate} onChange={e => setForm(f => ({ ...f, variantBTemplate: e.target.value }))} />
              </div>
            )}
          </div>

          <div className="flex gap-3">
            <button onClick={handleCreate} disabled={creating || !form.name || !form.segmentId || !form.messageTemplate}
              className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-semibold rounded-lg transition-all active:scale-95 flex items-center gap-2">
              {creating ? <><div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Creating...</> : <><Plus className="h-4 w-4" />Create Draft</>}
            </button>
            <button onClick={() => setShowForm(false)} className="px-4 py-2.5 border border-zinc-700 text-zinc-400 hover:text-white hover:border-zinc-600 text-sm rounded-lg transition-all">Cancel</button>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Campaign List */}
        <div className="glass-panel p-4 lg:col-span-1 bg-gradient-to-b from-dark-800/40 to-dark-900 border-zinc-800/50">
          <h3 className="text-white font-semibold text-sm mb-3 flex items-center gap-2"><Layers className="h-4 w-4 text-indigo-400" />All Campaigns</h3>
          {campaigns.length === 0 ? (
            <div className="text-center py-8 text-zinc-500 text-xs">No campaigns yet. Create one or use the AI Agent.</div>
          ) : (
            <div className="space-y-2">
              {campaigns.map(c => (
                <div key={c.id} onClick={() => setSelectedCampaign(c)}
                  className={`p-3 rounded-lg border cursor-pointer transition-all ${selectedCampaign?.id === c.id ? 'border-indigo-500/50 bg-indigo-500/5' : 'border-zinc-800 hover:border-zinc-700'}`}>
                  <div className="flex items-start justify-between gap-2 mb-1.5">
                    <p className="text-white text-xs font-semibold leading-tight line-clamp-2">{c.name}</p>
                    <StatusBadge status={c.status} />
                  </div>
                  <div className="flex items-center gap-2 text-[10px] text-zinc-500">
                    <span className="uppercase">{c.channel}</span>
                    <span>·</span>
                    <span>{c.metrics?.sent || 0} sent</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Campaign Detail */}
        <div className="lg:col-span-2 space-y-4">
          {selectedCampaign ? (
            <>
              <div className="glass-panel p-6 bg-gradient-to-br from-dark-800/60 to-dark-900 border-indigo-500/10 shadow-[0_0_20px_rgba(99,102,241,0.05)] relative overflow-hidden">
                <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/5 rounded-full blur-3xl pointer-events-none" />
                <div className="flex items-start justify-between mb-6 relative z-10">
                  <div>
                    <h2 className="text-white font-bold text-lg">{selectedCampaign.name}</h2>
                    <div className="flex items-center gap-2 mt-1">
                      <StatusBadge status={selectedCampaign.status} />
                      <span className="text-zinc-500 text-xs uppercase">{selectedCampaign.channel}</span>
                      {seg && <span className="text-zinc-500 text-xs">· {seg.name}</span>}
                    </div>
                  </div>
                  {selectedCampaign.status === 'Draft' && (
                    <button onClick={() => handleSend(selectedCampaign.id)} disabled={sending === selectedCampaign.id}
                      className="flex items-center gap-2 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white text-sm font-semibold rounded-lg transition-all active:scale-95">
                      {sending === selectedCampaign.id ? <><div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Sending...</> : <><Play className="h-4 w-4" />Send Now</>}
                    </button>
                  )}
                </div>

                {/* Message template */}
                <div className="p-3 bg-dark-950 rounded-lg border border-zinc-800 font-mono text-sm text-zinc-300 mb-4">
                  {selectedCampaign.messageTemplate}
                </div>

                {/* A/B Variants */}
                {selectedCampaign.variants?.length >= 2 && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
                    {selectedCampaign.variants.map(v => (
                      <div key={v.variantId} className="p-3 bg-dark-800/40 rounded-lg border border-zinc-800">
                        <p className="text-indigo-400 text-[10px] font-bold uppercase mb-1">Variant {v.variantId}</p>
                        <p className="text-zinc-300 text-xs font-mono">{v.template}</p>
                      </div>
                    ))}
                  </div>
                )}

                {/* Metrics */}
                {m && m.sent > 0 && (
                  <div className="space-y-2.5">
                    <MetricBar label="Delivered" value={m.delivered} max={m.sent} color="bg-blue-500" />
                    <MetricBar label="Read" value={m.read || 0} max={m.sent} color="bg-violet-500" />
                    <MetricBar label="Opened" value={m.opened} max={m.sent} color="bg-emerald-500" />
                    <MetricBar label="Clicked" value={m.clicked} max={m.sent} color="bg-amber-500" />
                    {(m.conversions || 0) > 0 && (
                      <div className="mt-1 p-2.5 rounded-lg bg-emerald-500/5 border border-emerald-500/20 flex items-center justify-between">
                        <span className="text-xs text-emerald-300 font-semibold flex items-center gap-1.5">
                          🏆 Conversions (Orders Attributed)
                        </span>
                        <span className="text-emerald-400 font-bold text-sm">{m.conversions}</span>
                      </div>
                    )}
                    {m.failed > 0 && <MetricBar label="Failed" value={m.failed} max={m.sent} color="bg-rose-500" />}
                  </div>
                )}
                {m && m.sent === 0 && selectedCampaign.status === 'Draft' && (
                  <div className="flex items-center gap-2 text-zinc-500 text-xs mt-2">
                    <Clock className="h-3.5 w-3.5" />
                    <span>Click "Send Now" to dispatch this campaign and start tracking events.</span>
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="glass-panel p-10 flex flex-col items-center justify-center text-center h-full min-h-[300px] bg-gradient-to-b from-dark-800/30 to-dark-900 border-dashed border-zinc-800/50">
              <div className="p-4 bg-indigo-500/5 rounded-full mb-4">
                <MessageSquare className="h-10 w-10 text-indigo-400/50" />
              </div>
              <h3 className="text-white font-semibold text-sm">Select a Campaign</h3>
              <p className="text-zinc-500 text-xs mt-2 max-w-[200px]">Click a campaign from the list to view details and send controls.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
