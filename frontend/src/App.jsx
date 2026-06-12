import React, { useState } from 'react';
import Dashboard from './components/Dashboard';
import Ingestion from './components/Ingestion';
import SegmentBuilder from './components/SegmentBuilder';
import CampaignManager from './components/CampaignManager';
import AgentConsole from './components/AgentConsole';
import AuditTrail from './components/AuditTrail';
import {
  LayoutDashboard, Upload, Filter, Megaphone,
  Cpu, ScrollText, ChevronRight, Zap
} from 'lucide-react';

const NAV = [
  { id: 'agent', label: 'AI Campaign Agent', icon: Cpu, badge: 'AI', badgeColor: 'bg-indigo-500/20 text-indigo-300 border-indigo-500/30' },
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { id: 'ingestion', label: 'Data Ingestion', icon: Upload },
  { id: 'segments', label: 'Segment Builder', icon: Filter },
  { id: 'campaigns', label: 'Campaigns', icon: Megaphone },
  { id: 'audit', label: 'Audit Trail', icon: ScrollText },
];

export default function App() {
  const [active, setActive] = useState('agent');

  const views = {
    dashboard: <Dashboard />,
    ingestion: <Ingestion />,
    segments: <SegmentBuilder />,
    campaigns: <CampaignManager />,
    agent: <AgentConsole />,
    audit: <AuditTrail />,
  };

  return (
    <div className="flex h-screen overflow-hidden bg-dark-950 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-indigo-900/20 via-dark-950 to-dark-950">
      {/* Sidebar */}
      <aside className="w-64 flex-shrink-0 bg-dark-900/50 backdrop-blur-xl border-r border-indigo-500/10 flex flex-col relative overflow-hidden">
        {/* Decorative sidebar glow */}
        <div className="absolute top-0 -left-20 w-40 h-40 bg-indigo-500/20 rounded-full blur-3xl" />
        
        {/* Brand */}
        <div className="px-5 py-5 border-b border-indigo-500/10 relative z-10">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-xl shadow-[0_0_20px_rgba(99,102,241,0.4)] animate-pulse">
              <Zap className="h-5 w-5 text-white" />
            </div>
            <div>
              <h1 className="text-white font-extrabold text-base leading-tight tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-indigo-200 to-fuchsia-200">MiniCRM</h1>
              <p className="text-indigo-400 text-[10px] font-bold tracking-widest uppercase opacity-80">AI-Native</p>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto relative z-10">
          <p className="text-indigo-300/50 text-[10px] uppercase tracking-widest font-bold px-2 mb-3">Navigation</p>
          {NAV.map(({ id, label, icon: Icon, badge, badgeColor }) => {
            const isActive = active === id;
            return (
              <button
                key={id}
                onClick={() => setActive(id)}
                className={`w-full flex items-center gap-3 px-3 py-3 rounded-xl text-sm font-semibold transition-all duration-300 group ${
                  isActive
                    ? 'bg-gradient-to-r from-indigo-500/20 to-purple-500/10 text-white border border-indigo-500/30 shadow-[0_0_15px_rgba(99,102,241,0.15)]'
                    : 'text-zinc-400 hover:text-white hover:bg-white/5'
                }`}
              >
                <Icon className={`h-4 w-4 flex-shrink-0 transition-transform duration-300 ${isActive ? 'text-indigo-400 scale-110' : 'text-zinc-500 group-hover:text-indigo-300 group-hover:scale-110'}`} />
                <span className="flex-1 text-left">{label}</span>
                {badge && (
                  <span className={`text-[9px] px-2 py-0.5 rounded-full border font-bold uppercase ${badgeColor} shadow-sm`}>{badge}</span>
                )}
                {isActive && <ChevronRight className="h-4 w-4 text-indigo-400 animate-[bounce-x_1s_infinite]" />}
              </button>
            );
          })}
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto relative scroll-smooth">
        <div className="max-w-6xl mx-auto px-6 py-8 relative z-10">
          {views[active]}
        </div>
      </main>
    </div>
  );
}
