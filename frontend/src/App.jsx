import React, { useState } from 'react';
import Dashboard from './components/Dashboard';
import Ingestion from './components/Ingestion';
import SegmentBuilder from './components/SegmentBuilder';
import CampaignManager from './components/CampaignManager';
import AgentConsole from './components/AgentConsole';
import AuditTrail from './components/AuditTrail';
import LoginPage from './components/auth/LoginPage';
import SignupPage from './components/auth/SignupPage';
import { useAuth } from './AuthContext';
import {
  LayoutDashboard, Upload, Filter, Megaphone,
  Cpu, ScrollText, ChevronRight, Zap, Menu, X, LogOut
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
  const { token, user, logout } = useAuth();
  const [active, setActive] = useState('agent');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [authView, setAuthView] = useState('login'); // 'login' | 'signup'

  // ── Route Guard ────────────────────────────────────────────────────────
  if (!token) {
    return authView === 'login'
      ? <LoginPage  onSwitchToSignup={() => setAuthView('signup')} />
      : <SignupPage onSwitchToLogin={()  => setAuthView('login')}  />;
  }

  const views = {
    dashboard: <Dashboard />,
    ingestion: <Ingestion />,
    segments: <SegmentBuilder />,
    campaigns: <CampaignManager />,
    agent: <AgentConsole />,
    audit: <AuditTrail />,
  };

  const handleNav = (id) => {
    setActive(id);
    setSidebarOpen(false);
  };

  const SidebarContent = () => (
    <>
      {/* Decorative sidebar glow */}
      <div className="absolute top-0 -left-20 w-40 h-40 bg-indigo-500/20 rounded-full blur-3xl" />

      {/* Brand */}
      <div className="px-5 py-5 border-b border-indigo-500/10 relative z-10">
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-xl shadow-[0_0_20px_rgba(99,102,241,0.4)] animate-pulse">
              <Zap className="h-5 w-5 text-white" />
            </div>
            <div>
              <h1 className="text-white font-extrabold text-base leading-tight tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-indigo-200 to-fuchsia-200">MiniCRM</h1>
              <p className="text-indigo-400 text-[10px] font-bold tracking-widest uppercase opacity-80">AI-Native</p>
            </div>
          </div>
          {/* Close button — mobile only */}
          <button
            onClick={() => setSidebarOpen(false)}
            className="md:hidden p-1.5 rounded-lg text-zinc-400 hover:text-white hover:bg-white/10 transition-colors"
            aria-label="Close menu"
          >
            <X className="h-5 w-5" />
          </button>
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
              onClick={() => handleNav(id)}
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
              {isActive && <ChevronRight className="h-4 w-4 text-indigo-400" />}
            </button>
          );
        })}
      </nav>

      {/* User info + Logout */}
      <div className="px-3 pb-4 pt-3 border-t border-indigo-500/10 relative z-10">
        <div className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-dark-800/40 border border-zinc-800/50">
          {/* Avatar */}
          <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
            {user?.name?.charAt(0)?.toUpperCase() ?? user?.email?.charAt(0)?.toUpperCase() ?? '?'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-white text-xs font-semibold truncate">{user?.name ?? 'User'}</p>
            <p className="text-zinc-500 text-[10px] truncate">{user?.email}</p>
          </div>
          <button
            id="logout-btn"
            onClick={logout}
            title="Sign out"
            className="p-1.5 text-zinc-500 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors flex-shrink-0"
          >
            <LogOut className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    </>
  );

  return (
    <div className="flex h-screen overflow-hidden bg-dark-950 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-indigo-900/20 via-dark-950 to-dark-950">

      {/* ── Desktop Sidebar (hidden on mobile) ── */}
      <aside className="hidden md:flex w-64 flex-shrink-0 bg-dark-900/50 backdrop-blur-xl border-r border-indigo-500/10 flex-col relative overflow-hidden">
        <SidebarContent />
      </aside>

      {/* ── Mobile Backdrop ── */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* ── Mobile Slide-in Sidebar ── */}
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-72 flex flex-col bg-dark-900/95 backdrop-blur-xl border-r border-indigo-500/10 overflow-hidden
          transform transition-transform duration-300 ease-in-out md:hidden
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <SidebarContent />
      </aside>

      {/* ── Main Content ── */}
      <div className="flex-1 flex flex-col overflow-hidden">

        {/* Mobile Top Bar */}
        <header className="md:hidden flex items-center justify-between px-4 py-3 bg-dark-900/80 backdrop-blur-md border-b border-indigo-500/10 flex-shrink-0">
          <button
            onClick={() => setSidebarOpen(true)}
            className="p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-white/10 transition-colors"
            aria-label="Open menu"
          >
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex items-center gap-2">
            <div className="p-1.5 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-lg shadow-[0_0_12px_rgba(99,102,241,0.4)]">
              <Zap className="h-4 w-4 text-white" />
            </div>
            <span className="text-white font-extrabold text-sm bg-clip-text text-transparent bg-gradient-to-r from-indigo-200 to-fuchsia-200">MiniCRM</span>
          </div>
          <div className="w-9" /> {/* spacer to center brand */}
        </header>

        {/* Scrollable Page Content */}
        <main className="flex-1 overflow-y-auto relative scroll-smooth">
          <div className="max-w-6xl mx-auto px-4 md:px-6 py-6 md:py-8 relative z-10 pb-24 md:pb-8">
            {views[active]}
          </div>
        </main>

        {/* ── Mobile Bottom Nav ── */}
        <nav className="md:hidden flex-shrink-0 border-t border-indigo-500/10 bg-dark-900/90 backdrop-blur-xl pb-safe">
          <div className="flex overflow-x-auto hide-scrollbar snap-x">
            {NAV.map(({ id, label, icon: Icon, badge }) => {
              const isActive = active === id;
              return (
                <button
                  key={id}
                  onClick={() => handleNav(id)}
                  className={`flex-none w-[72px] snap-start flex flex-col items-center justify-center py-3 px-1 gap-1 transition-all duration-200 relative ${
                    isActive ? 'text-indigo-400' : 'text-zinc-600 hover:text-zinc-400'
                  }`}
                >
                  {isActive && (
                    <span className="absolute top-0 left-1/2 -translate-x-1/2 w-8 h-0.5 bg-indigo-500 rounded-full" />
                  )}
                  <Icon className={`h-5 w-5 transition-transform duration-200 ${isActive ? 'scale-110' : ''}`} />
                  <span className="text-[9px] font-semibold leading-none truncate w-full text-center px-1">
                    {label.split(' ')[0]}
                  </span>
                  {badge && (
                    <span className="absolute top-1.5 right-1 text-[7px] px-1 py-0.5 bg-indigo-500/30 text-indigo-300 border border-indigo-500/40 rounded-full font-bold uppercase leading-none">
                      AI
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </nav>
      </div>
    </div>
  );
}
