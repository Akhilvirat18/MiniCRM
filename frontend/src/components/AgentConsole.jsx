import React, { useState, useEffect, useRef } from 'react';
import { api } from '../services/api';
import {
  Sparkles, Send, CheckCircle, ChevronRight, Cpu,
  Database, Radio, MessageSquare, BarChart2, ThumbsUp,
  RefreshCw, ExternalLink, Zap
} from 'lucide-react';

const EXAMPLE_GOALS = [
  "Increase repeat purchases this weekend",
  "Win back customers who haven't purchased in 60 days",
  "Promote coffee products to frequent buyers",
  "Re-engage Gold tier customers lapsed over 30 days",
];

// Simulated thought steps for the AI Thought Process Visualizer
function buildThoughtSteps(goal) {
  return [
    { icon: '🔍', label: 'Scanning Customer Database', detail: 'Analyzing recency, frequency, and category patterns...', delay: 400 },
    { icon: '📊', label: 'Computing RFM Scores', detail: 'Calculating lapse windows, total spend, and order counts...', delay: 900 },
    { icon: '🎯', label: 'Identifying Target Segment', detail: null, delay: 1500 },
    { icon: '📡', label: 'Evaluating Delivery Channels', detail: 'Comparing phone coverage, email availability, and engagement rates...', delay: 2200 },
    { icon: '✍️', label: 'Generating Message Templates', detail: 'Applying tone controls and personalisation tokens...', delay: 3000 },
    { icon: '📈', label: 'Estimating Reach & Engagement', detail: 'Running probability models on lapsed cohort data...', delay: 3700 },
    { icon: '✅', label: 'Campaign Plan Ready', detail: 'Review the proposal below and approve to save as draft.', delay: 4200 },
  ];
}

function ThoughtLine({ step, revealed, isLast }) {
  if (!revealed) return null;
  return (
    <div className={`flex items-start gap-3 transition-all duration-500 ${revealed ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-2'}`}>
      <div className="flex flex-col items-center">
        <div className="text-lg leading-none w-7 text-center">{step.icon}</div>
        {!isLast && <div className="w-px h-full min-h-[20px] bg-zinc-800 mt-1 flex-1" />}
      </div>
      <div className="pb-4 flex-1">
        <p className="text-sm text-white font-semibold">{step.label}</p>
        {step.detail && <p className="text-xs text-zinc-500 mt-0.5">{step.detail}</p>}
      </div>
      <div className="mt-0.5">
        {isLast
          ? <div className="h-2 w-2 rounded-full bg-emerald-500 animate-ping" />
          : <CheckCircle className="h-4 w-4 text-emerald-500" />}
      </div>
    </div>
  );
}

function PlanCard({ plan, explainability, onApprove, approving, approved }) {
  const [activeTab, setActiveTab] = useState('overview');
  if (!plan) return null;

  const seg = plan.segment_name || plan.segmentName || 'Recommended Segment';
  const channel = plan.channel || 'sms';
  const msg = plan.message?.template || '';
  const proj = plan.projections || plan.expected_outcomes || {};
  const variants = plan.message?.variants || [];

  const tabs = [
    { id: 'overview', label: 'Overview' },
    { id: 'message', label: 'Copy' },
    { id: 'why', label: '🧠 Explainability' },
  ];

  return (
    <div className="glass-panel border-indigo-500/30 overflow-hidden">
      {/* Plan header */}
      <div className="p-5 border-b border-zinc-800 bg-gradient-to-r from-indigo-500/5 to-transparent">
        <div className="flex items-center gap-2 mb-1">
          <Sparkles className="h-4 w-4 text-indigo-400" />
          <span className="text-indigo-400 text-xs font-semibold uppercase tracking-widest">AI Campaign Proposal</span>
        </div>
        <h3 className="text-white font-bold text-lg">{seg}</h3>
        <div className="flex items-center gap-3 mt-2 flex-wrap">
          <span className="text-xs px-2.5 py-1 bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 rounded-full uppercase font-bold">{channel}</span>
          {proj.expected_reach && (
            <span className="text-xs text-zinc-400">
              ~<span className="text-white font-bold">{proj.expected_reach.toLocaleString()}</span> estimated reach
            </span>
          )}
          {proj.open_rate && (
            <span className="text-xs text-zinc-400">
              <span className="text-emerald-400 font-bold">{(proj.open_rate * 100).toFixed(0)}%</span> open rate
            </span>
          )}
          {proj.click_rate && (
            <span className="text-xs text-zinc-400">
              <span className="text-amber-400 font-bold">{(proj.click_rate * 100).toFixed(0)}%</span> click rate
            </span>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-zinc-800">
        {tabs.map(t => (
          <button key={t.id} onClick={() => setActiveTab(t.id)}
            className={`px-5 py-2.5 text-xs font-semibold transition-colors ${activeTab === t.id ? 'text-white border-b-2 border-indigo-500' : 'text-zinc-500 hover:text-zinc-300'}`}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="p-5">
        {activeTab === 'overview' && (
          <div className="space-y-3">
            <div className="grid grid-cols-3 gap-3">
              {[
                { label: 'Reach', value: proj.expected_reach?.toLocaleString() || '—', color: 'text-indigo-400' },
                { label: 'Est. Open Rate', value: proj.open_rate ? `${(proj.open_rate * 100).toFixed(0)}%` : '—', color: 'text-emerald-400' },
                { label: 'Est. CTR', value: proj.click_rate ? `${(proj.click_rate * 100).toFixed(0)}%` : '—', color: 'text-amber-400' },
              ].map(m => (
                <div key={m.label} className="p-3 bg-dark-950 rounded-lg border border-zinc-800 text-center">
                  <p className={`text-xl font-extrabold ${m.color}`}>{m.value}</p>
                  <p className="text-zinc-500 text-[10px] mt-1">{m.label}</p>
                </div>
              ))}
            </div>
            <div className="p-3 bg-dark-950 rounded-lg border border-zinc-800">
              <p className="text-zinc-500 text-[10px] uppercase tracking-widest mb-1">Segment Filter Preview</p>
              <pre className="text-zinc-300 text-[11px] font-mono overflow-x-auto whitespace-pre-wrap">
                {JSON.stringify(plan.segment_filter || plan.segmentFilter, null, 2)}
              </pre>
            </div>
          </div>
        )}

        {activeTab === 'message' && (
          <div className="space-y-3">
            <div>
              <p className="text-zinc-500 text-[10px] uppercase tracking-widest mb-2">Primary Template</p>
              <div className="p-3 bg-dark-950 rounded-lg border border-zinc-800 font-mono text-sm text-zinc-200">{msg}</div>
            </div>
            {variants.length >= 2 && (
              <div>
                <p className="text-zinc-500 text-[10px] uppercase tracking-widest mb-2">A/B Variants (50/50 split)</p>
                <div className="space-y-2">
                  {variants.map(v => (
                    <div key={v.variant_id} className="p-3 bg-dark-800/40 rounded-lg border border-zinc-800">
                      <span className="text-indigo-400 text-[10px] font-bold uppercase mr-2">Variant {v.variant_id}</span>
                      <span className="text-zinc-300 text-xs font-mono">{v.template}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {activeTab === 'why' && explainability && (
          <div className="space-y-4">
            {[
              { key: 'why_segment', label: 'Why this Segment?', icon: Database },
              { key: 'why_channel', label: 'Why this Channel?', icon: Radio },
              { key: 'why_message', label: 'Why this Message?', icon: MessageSquare },
            ].map(({ key, label, icon: Icon }) => {
              const reasons = explainability[key] || [];
              return (
                <div key={key}>
                  <div className="flex items-center gap-2 mb-2">
                    <Icon className="h-3.5 w-3.5 text-indigo-400" />
                    <p className="text-zinc-300 text-xs font-semibold">{label}</p>
                  </div>
                  <ul className="space-y-1.5 pl-5">
                    {reasons.map((r, i) => (
                      <li key={i} className="text-zinc-400 text-xs flex items-start gap-2">
                        <span className="text-indigo-500 mt-0.5 flex-shrink-0">›</span>{r}
                      </li>
                    ))}
                  </ul>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Approve Footer */}
      {!approved && (
        <div className="p-4 border-t border-zinc-800 bg-dark-950/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <p className="text-zinc-500 text-xs">Review the plan above and approve to create a campaign draft.</p>
          <button onClick={onApprove} disabled={approving}
            className="w-full sm:w-auto justify-center flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white text-sm font-bold rounded-lg transition-all active:scale-95 whitespace-nowrap">
            {approving
              ? <><div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Creating Draft...</>
              : <><ThumbsUp className="h-4 w-4" />Approve & Create Draft</>}
          </button>
        </div>
      )}
      {approved && (
        <div className="p-4 border-t border-emerald-500/20 bg-emerald-500/5 flex items-center gap-2">
          <CheckCircle className="h-4 w-4 text-emerald-400" />
          <p className="text-emerald-300 text-sm font-semibold">Campaign draft created! Go to Campaign Manager to send it.</p>
        </div>
      )}
    </div>
  );
}

export default function AgentConsole() {
  const [goal, setGoal] = useState('');
  const [maxDiscount, setMaxDiscount] = useState(15);
  const [session, setSession] = useState(null);
  const [thinking, setThinking] = useState(false);
  const [revealedSteps, setRevealedSteps] = useState([]);
  const [thoughtSteps, setThoughtSteps] = useState([]);
  const [approving, setApproving] = useState(false);
  const [approved, setApproved] = useState(false);
  const [refineInput, setRefineInput] = useState('');
  const [refining, setRefining] = useState(false);
  const consoleRef = useRef(null);

  const runThoughtAnimation = (steps) => {
    setRevealedSteps([]);
    steps.forEach((step, i) => {
      setTimeout(() => {
        setRevealedSteps(prev => [...prev, i]);
      }, step.delay);
    });
  };

  const handleSubmitGoal = async () => {
    if (!goal.trim() || thinking) return;
    setThinking(true);
    setSession(null);
    setApproved(false);
    setRevealedSteps([]);

    const steps = buildThoughtSteps(goal);
    setThoughtSteps(steps);
    runThoughtAnimation(steps);

    try {
      const result = await api.initiateCampaignPlan(goal, maxDiscount);
      setSession(result);
    } catch (e) {
      console.error('Agent failed:', e);
    } finally {
      setThinking(false);
    }
  };

  const handleApprove = async () => {
    if (!session) return;
    setApproving(true);
    try {
      await api.approveCampaignPlan(session.id);
      setApproved(true);
    } finally {
      setApproving(false);
    }
  };

  const handleRefine = async () => {
    if (!refineInput.trim() || !session) return;
    setRefining(true);
    const steps = buildThoughtSteps(refineInput);
    setThoughtSteps(steps);
    runThoughtAnimation(steps);
    try {
      const updated = await api.refineCampaignPlan(session.id, refineInput);
      setSession(updated);
      setRefineInput('');
      setApproved(false);
    } finally {
      setRefining(false);
    }
  };

  const plan = session?.currentPlan;
  const explainability = session?.explainability;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <div className="flex items-center gap-3 mb-1">
          <div className="p-1.5 bg-indigo-500/10 border border-indigo-500/20 rounded-lg">
            <Zap className="h-5 w-5 text-indigo-400" />
          </div>
          <h1 className="text-3xl font-extrabold tracking-tight text-white">AI Campaign Agent</h1>
        </div>
        <p className="text-zinc-400 text-sm mt-1 ml-12">Enter a marketing goal. The AI will plan your entire campaign — audience, message, channel, and projections.</p>
      </div>

      {/* Goal input */}
      <div className="glass-panel p-5 space-y-4">
        <div className="flex flex-wrap gap-2 mb-1">
          {EXAMPLE_GOALS.map((eg, i) => (
            <button key={i} onClick={() => setGoal(eg)}
              className="text-xs px-3 py-1.5 bg-dark-800/60 border border-zinc-700 hover:border-indigo-500/50 hover:text-indigo-300 text-zinc-400 rounded-full transition-all">
              {eg}
            </button>
          ))}
        </div>
        <div className="flex flex-col md:flex-row gap-3">
          <input
            className="flex-1 bg-dark-950 border border-zinc-800 focus:border-indigo-500 rounded-lg px-4 py-3 text-sm text-white placeholder-zinc-600 outline-none transition-colors"
            placeholder="Describe your marketing goal in plain English..."
            value={goal}
            onChange={e => setGoal(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSubmitGoal()}
          />
          <div className="flex items-center justify-between md:justify-start gap-2">
            <label className="text-xs text-zinc-500 whitespace-nowrap">Max Discount</label>
            <select value={maxDiscount} onChange={e => setMaxDiscount(Number(e.target.value))}
              className="bg-dark-950 border border-zinc-800 rounded-lg px-2 py-3 text-sm text-white outline-none">
              {[5, 10, 15, 20, 25].map(v => <option key={v} value={v}>{v}%</option>)}
            </select>
          </div>
          <button onClick={handleSubmitGoal} disabled={thinking || !goal.trim()}
            className="w-full md:w-auto justify-center px-5 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-bold rounded-lg transition-all active:scale-95 flex items-center gap-2 whitespace-nowrap">
            {thinking
              ? <><div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />Thinking...</>
              : <><Sparkles className="h-4 w-4" />Plan Campaign</>}
          </button>
        </div>
      </div>

      {/* Thought Process Terminal */}
      {thoughtSteps.length > 0 && (
        <div className="glass-panel overflow-hidden">
          <div className="flex items-center gap-3 px-5 py-3 border-b border-zinc-800 bg-dark-950/70">
            <div className="flex gap-1.5">
              <div className="h-2.5 w-2.5 rounded-full bg-rose-500" />
              <div className="h-2.5 w-2.5 rounded-full bg-amber-500" />
              <div className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
            </div>
            <div className="flex items-center gap-2 flex-1">
              <Cpu className="h-3.5 w-3.5 text-indigo-400" />
              <span className="text-zinc-400 text-xs font-mono">AI Campaign Agent — Thought Process</span>
            </div>
            {thinking && (
              <div className="flex items-center gap-1.5 text-[10px] text-emerald-400 font-mono">
                <div className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping" />
                PROCESSING
              </div>
            )}
            {!thinking && session && (
              <div className="flex items-center gap-1.5 text-[10px] text-emerald-400 font-mono">
                <CheckCircle className="h-3 w-3" />
                COMPLETE
              </div>
            )}
          </div>
          <div className="p-5 font-mono space-y-0" ref={consoleRef}>
            <div className="mb-4 text-xs text-zinc-600 font-mono">
              $ agent run --goal "{goal.substring(0, 60)}{goal.length > 60 ? '...' : ''}" --max-discount {maxDiscount}%
            </div>
            {thoughtSteps.map((step, i) => (
              <ThoughtLine
                key={i}
                step={step}
                revealed={revealedSteps.includes(i)}
                isLast={i === thoughtSteps.length - 1 && thinking}
              />
            ))}
          </div>
        </div>
      )}

      {/* Campaign Plan Proposal */}
      {plan && !thinking && (
        <PlanCard
          plan={plan}
          explainability={explainability}
          onApprove={handleApprove}
          approving={approving}
          approved={approved}
        />
      )}

      {/* Refine Input */}
      {session && !approved && plan && (
        <div className="glass-panel p-4 flex gap-3">
          <input
            className="flex-1 bg-dark-950 border border-zinc-800 focus:border-zinc-600 rounded-lg px-3 py-2.5 text-sm text-white placeholder-zinc-600 outline-none transition-colors"
            placeholder="Ask to change something — e.g. 'Use email instead' or 'Target only Gold tier'..."
            value={refineInput}
            onChange={e => setRefineInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleRefine()}
          />
          <button onClick={handleRefine} disabled={refining || !refineInput.trim()}
            className="px-4 py-2.5 bg-zinc-800 hover:bg-zinc-700 disabled:opacity-50 text-white text-sm rounded-lg transition-all flex items-center gap-2">
            {refining ? <div className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : <RefreshCw className="h-4 w-4" />}
            Refine
          </button>
        </div>
      )}
    </div>
  );
}
