import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { Search, Sparkles, Users, Save, Trash2, Eye, ChevronRight, Filter } from 'lucide-react';

const QUICK_SUGGESTIONS = [
  "Customers who bought coffee beans but haven't ordered in 60 days",
  "High-value Gold tier shoppers lapsed over 30 days",
  "All customers from Seattle with consent",
  "Shoppers who spent more than 200 total",
];

function FilterChip({ rule, onRemove }) {
  const label = `${rule.field} ${rule.op} ${rule.value}`;
  return (
    <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-indigo-500/10 border border-indigo-500/20 rounded-full text-xs text-indigo-300 font-mono">
      {label}
      {onRemove && <button onClick={onRemove} className="hover:text-white transition-colors"><Trash2 className="h-2.5 w-2.5" /></button>}
    </span>
  );
}

function renderFilterTree(node) {
  if (!node) return null;
  if (node.and) {
    return node.and.map((rule, i) => <FilterChip key={i} rule={rule} />);
  }
  if (node.or) {
    return node.or.map((rule, i) => <FilterChip key={i} rule={rule} />);
  }
  if (node.field) {
    return <FilterChip rule={node} />;
  }
  return null;
}

export default function SegmentBuilder() {
  const [nlInput, setNlInput] = useState('');
  const [translating, setTranslating] = useState(false);
  const [translated, setTranslated] = useState(null);
  const [segments, setSegments] = useState([]);
  const [saving, setSaving] = useState(false);
  const [savedId, setSavedId] = useState(null);
  const [previewCustomers, setPreviewCustomers] = useState([]);
  const [loadingSegments, setLoadingSegments] = useState(false);
  const [selectedSegment, setSelectedSegment] = useState(null);

  const loadSegments = async () => {
    setLoadingSegments(true);
    try {
      const segs = await api.getSegments();
      setSegments(segs);
    } finally {
      setLoadingSegments(false);
    }
  };

  useEffect(() => { loadSegments(); }, []);

  const handleTranslate = async () => {
    if (!nlInput.trim()) return;
    setTranslating(true);
    setTranslated(null);
    setPreviewCustomers([]);
    try {
      const result = await api.createSegmentAI(nlInput, 'AI Segment');
      setTranslated(result);
      // Auto-load preview customers
      if (result.filter_rules) {
        const preview = await api.previewFilter(result.filter_rules);
        setPreviewCustomers(preview.samples || []);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setTranslating(false);
    }
  };

  const handleSave = async () => {
    if (!translated) return;
    setSaving(true);
    try {
      const saved = await api.saveSegment({
        name: nlInput.substring(0, 50),
        description: nlInput,
        filterRules: translated.filter_rules,
        previewCount: translated.preview_count,
      });
      setSavedId(saved.id);
      await loadSegments();
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteSegment = async (id, e) => {
    e.stopPropagation();
    await api.deleteSegment(id);
    await loadSegments();
    if (selectedSegment?.id === id) setSelectedSegment(null);
  };

  const handleViewSegment = async (seg) => {
    setSelectedSegment(seg);
    try {
      const custs = await api.getSegmentCustomers(seg.id);
      setPreviewCustomers(custs);
    } catch { setPreviewCustomers([]); }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight text-white">Segment Builder</h1>
        <p className="text-zinc-400 text-sm mt-1">Describe your audience in plain English — AI translates it to structured filters.</p>
      </div>

      {/* NL Input */}
      <div className="glass-panel p-5 space-y-4">
        <div className="flex items-center gap-2 mb-1">
          <Sparkles className="h-4 w-4 text-indigo-400" />
          <h3 className="text-white font-semibold text-sm">Natural Language to Segment</h3>
        </div>

        {/* Quick suggestions */}
        <div className="flex flex-wrap gap-2">
          {QUICK_SUGGESTIONS.map((s, i) => (
            <button
              key={i}
              onClick={() => setNlInput(s)}
              className="text-xs px-3 py-1.5 bg-dark-800/60 border border-zinc-700 hover:border-indigo-500/50 hover:text-indigo-300 text-zinc-400 rounded-full transition-all"
            >
              {s.length > 40 ? s.substring(0, 40) + '…' : s}
            </button>
          ))}
        </div>

        <div className="flex gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-500" />
            <input
              className="w-full bg-dark-950 border border-zinc-800 focus:border-indigo-500 rounded-lg pl-10 pr-4 py-3 text-sm text-white placeholder-zinc-600 outline-none transition-colors"
              placeholder="e.g. Gold customers who haven't bought in 60 days..."
              value={nlInput}
              onChange={e => setNlInput(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleTranslate()}
            />
          </div>
          <button
            onClick={handleTranslate}
            disabled={translating || !nlInput.trim()}
            className="px-5 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-semibold rounded-lg transition-all active:scale-95 flex items-center gap-2 whitespace-nowrap"
          >
            {translating ? <><div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Translating...</> : <><Sparkles className="h-4 w-4" />Translate</>}
          </button>
        </div>
      </div>

      {/* Translation Result */}
      {translated && (
        <div className="glass-panel p-5 space-y-4 border-indigo-500/20">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Filter className="h-4 w-4 text-indigo-400" />
              <h3 className="text-white font-semibold text-sm">Translated Segment Rules</h3>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-zinc-500 bg-dark-800 px-3 py-1 rounded-full border border-zinc-700">
                <span className="text-white font-bold">{translated.preview_count}</span> matching customers
              </span>
            </div>
          </div>

          {/* Filter chips */}
          <div className="flex flex-wrap gap-2 p-3 bg-dark-950 rounded-lg border border-zinc-800">
            {renderFilterTree(translated.filter_rules)}
          </div>

          {/* Preview customers */}
          {previewCustomers.length > 0 && (
            <div>
              <p className="text-zinc-500 text-xs uppercase tracking-widest font-semibold mb-2">Sample Matches</p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                {previewCustomers.slice(0, 4).map((c, i) => (
                  <div key={i} className="flex items-center gap-3 p-2.5 bg-dark-800/40 rounded-lg border border-zinc-800">
                    <div className="h-8 w-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-bold text-white flex-shrink-0">
                      {c.name?.[0] || '?'}
                    </div>
                    <div>
                      <p className="text-white text-xs font-semibold">{c.name}</p>
                      <p className="text-zinc-500 text-[10px]">{c.city} · {c.tier}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Save button */}
          <div className="flex gap-3">
            <button
              onClick={handleSave}
              disabled={saving || !!savedId}
              className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white text-sm font-semibold rounded-lg transition-all active:scale-95 flex items-center gap-2"
            >
              {saving ? <><div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Saving...</> : savedId ? <><Save className="h-4 w-4" />Saved!</> : <><Save className="h-4 w-4" />Save Segment</>}
            </button>
          </div>
        </div>
      )}

      {/* Saved Segments */}
      <div className="glass-panel p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-white font-semibold text-sm flex items-center gap-2"><Users className="h-4 w-4 text-zinc-400" /> Saved Segments</h3>
          <span className="text-xs text-zinc-500">{segments.length} total</span>
        </div>
        {segments.length === 0 ? (
          <div className="text-center py-8 text-zinc-500 text-sm">No segments saved yet. Translate one above to get started.</div>
        ) : (
          <div className="space-y-2">
            {segments.map(seg => (
              <div
                key={seg.id}
                onClick={() => handleViewSegment(seg)}
                className={`flex items-center justify-between p-3.5 rounded-lg border cursor-pointer transition-all ${selectedSegment?.id === seg.id ? 'border-indigo-500/50 bg-indigo-500/5' : 'border-zinc-800 bg-dark-800/30 hover:border-zinc-700'}`}
              >
                <div className="flex-1 min-w-0">
                  <p className="text-white text-sm font-semibold truncate">{seg.name}</p>
                  <p className="text-zinc-500 text-xs mt-0.5 truncate">{seg.description}</p>
                </div>
                <div className="flex items-center gap-3 flex-shrink-0 ml-3">
                  <span className="text-xs text-zinc-400 bg-dark-900 px-2 py-0.5 rounded-full border border-zinc-800">
                    {seg.previewCount?.toLocaleString() || 0} shoppers
                  </span>
                  <Eye className="h-4 w-4 text-zinc-500" />
                  <button onClick={e => handleDeleteSegment(seg.id, e)} className="text-zinc-600 hover:text-rose-400 transition-colors">
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Segment detail */}
        {selectedSegment && previewCustomers.length > 0 && (
          <div className="mt-4 p-4 bg-dark-950 rounded-lg border border-zinc-800">
            <p className="text-zinc-400 text-xs uppercase tracking-widest font-semibold mb-3">Customers in "{selectedSegment.name}"</p>
            <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
              {previewCustomers.slice(0, 6).map((c, i) => (
                <div key={i} className="flex items-center gap-2 p-2 bg-dark-800/40 rounded border border-zinc-800">
                  <div className="h-7 w-7 rounded-full bg-gradient-to-br from-purple-500 to-pink-600 flex items-center justify-center text-[10px] font-bold text-white flex-shrink-0">
                    {c.name?.[0] || '?'}
                  </div>
                  <div className="min-w-0">
                    <p className="text-white text-xs font-medium truncate">{c.name}</p>
                    <p className="text-zinc-600 text-[10px]">{c.tier}</p>
                  </div>
                </div>
              ))}
            </div>
            {previewCustomers.length > 6 && <p className="text-zinc-600 text-xs mt-2">+{previewCustomers.length - 6} more customers</p>}
          </div>
        )}
      </div>
    </div>
  );
}
