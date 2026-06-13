import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { 
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, 
  AreaChart, Area, Legend, FunnelChart, Funnel, LabelList 
} from 'recharts';
import { 
  TrendingUp, Users, Send, Target, RefreshCw, 
  AlertTriangle, AlertCircle, FileSpreadsheet, Activity 
} from 'lucide-react';

export default function Dashboard() {
  const [campaigns, setCampaigns] = useState([]);
  const [stats, setStats] = useState({ totalCustomers: 0, totalOrders: 0, totalCampaigns: 0 });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [recentEvents, setRecentEvents] = useState([]);

  const loadData = async () => {
    try {
      const camps = await api.getCampaigns();
      const custs = await api.getCustomers();
      const ords = await api.getOrders();
      
      setCampaigns(camps);
      setStats({
        totalCustomers: custs.length,
        totalOrders: ords.length,
        totalCampaigns: camps.length
      });

      // Assemble recent events ticker from communications status updates
      let allComms = [];
      for (const camp of camps) {
        try {
          const comms = await api.getCampaignCommunications(camp.id);
          allComms = [...allComms, ...comms];
        } catch (e) {
          // ignore
        }
      }

      // Sort and pick top 6 recent non-pending statuses
      const activeEvents = allComms
        .filter(c => c.status !== 'pending' && c.status !== 'sent')
        .map(c => {
          const campaignName = campaigns.find(camp => camp.id === c.campaignId)?.name || 'Campaign';
          return {
            id: c.id,
            recipient: c.recipient,
            status: c.status,
            campaign: campaignName,
            time: c.updatedAt ? new Date(c.updatedAt).toLocaleTimeString() : 'Just now'
          };
        })
        .reverse()
        .slice(0, 6);

      setRecentEvents(activeEvents);
    } catch (e) {
      console.error("Failed to load dashboard data", e);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
    // Auto polling every 3 seconds to update statistics dynamically
    const timer = setInterval(() => {
      loadData();
    }, 3000);
    return () => clearInterval(timer);
  }, []);

  const aggregateMetrics = () => {
    let sent = 0, delivered = 0, read = 0, opened = 0, clicked = 0, failed = 0, conversions = 0;
    campaigns.forEach(c => {
      if (c.metrics) {
        sent        += c.metrics.sent;
        delivered   += c.metrics.delivered;
        read        += c.metrics.read || 0;
        opened      += c.metrics.opened;
        clicked     += c.metrics.clicked;
        failed      += c.metrics.failed;
        conversions += c.metrics.conversions || 0;
      }
    });

    return {
      funnel: [
        { value: sent,      name: 'Sent',      fill: '#6366f1' },
        { value: delivered, name: 'Delivered', fill: '#3b82f6' },
        { value: read,      name: 'Read',      fill: '#8b5cf6' },
        { value: opened,    name: 'Opened',    fill: '#10b981' },
        { value: clicked,   name: 'Clicked',   fill: '#f59e0b' },
      ],
      conversions,
    };
  };

  const { funnel: funnelData, conversions: totalConversions } = aggregateMetrics();
  const totalSent    = funnelData[0].value;
  const totalOpened  = funnelData[3].value;
  const totalClicked = funnelData[4].value;

  const averageCTR      = totalSent > 0 ? ((totalClicked / totalSent) * 100).toFixed(1) : 0;
  const averageOpenRate = totalSent > 0 ? ((totalOpened  / totalSent) * 100).toFixed(1) : 0;

  // Chart data — only include campaigns that actually sent to someone
  const campaignChartData = campaigns
    .filter(c => c.metrics?.sent > 0)
    .slice(-10) // Show 10 most recent with real sends
    .map(c => ({
      name: c.name.replace('Campaign: ', '').substring(0, 15) + '...',
      sent: c.metrics?.sent || 0,
      opened: c.metrics?.opened || 0,
      clicked: c.metrics?.clicked || 0,
    }));

  // P1 Anomaly Detection: Check if any active campaign has underperforming stats
  const anomalies = campaigns.filter(c => {
    if (!c.metrics || c.metrics.sent < 5) return false;
    const clickRate = c.metrics.clicked / c.metrics.sent;
    // Anomaly flag: Click rate lower than 2% for campaigns with real sends
    return clickRate < 0.02 && c.status === 'Sent' && c.metrics.sent > 0;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap justify-between items-start gap-3">
        <div>
          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white">Performance Insights</h1>
          <p className="text-zinc-400 text-sm mt-1">Real-time activation analytics and delivery logs.</p>
        </div>
        <button 
          onClick={() => { setRefreshing(true); loadData(); }} 
          className="flex items-center gap-2 px-4 py-2 bg-dark-900 border border-zinc-800 rounded-lg hover:bg-zinc-800 text-sm text-zinc-300 transition-all active:scale-95"
        >
          <RefreshCw className={`h-4 w-4 ${refreshing || loading ? 'animate-spin' : ''}`} />
          {refreshing ? 'Syncing...' : 'Sync Now'}
        </button>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3 md:gap-4">
        <div className="glass-panel p-5 relative overflow-hidden bg-gradient-to-br from-indigo-900/40 to-dark-900 border border-indigo-500/20 shadow-[0_0_25px_rgba(99,102,241,0.15)] group hover:shadow-[0_0_35px_rgba(99,102,241,0.25)] transition-all duration-300">
          <div className="flex justify-between items-start relative z-10">
            <div>
              <p className="text-indigo-200/60 text-xs font-semibold uppercase tracking-wider">Total Shoppers</p>
              <h3 className="text-3xl font-extrabold text-white mt-2 group-hover:scale-105 origin-left transition-transform duration-300">
                {loading ? '...' : stats.totalCustomers.toLocaleString()}
              </h3>
            </div>
            <div className="p-2.5 bg-gradient-to-br from-indigo-500 to-purple-600 shadow-[0_0_15px_rgba(99,102,241,0.4)] text-white rounded-xl">
              <Users className="h-5 w-5" />
            </div>
          </div>
          <div className="absolute -bottom-10 -right-10 w-32 h-32 bg-indigo-500/20 rounded-full blur-3xl group-hover:bg-indigo-500/30 transition-all duration-500" />
          <div className="text-indigo-300/60 text-xs mt-3 flex items-center gap-1 relative z-10">
            <TrendingUp className="h-3 w-3 text-emerald-400" />
            <span>Active subscriber profiles</span>
          </div>
        </div>

        <div className="glass-panel p-5 relative overflow-hidden bg-gradient-to-br from-blue-900/40 to-dark-900 border border-blue-500/20 shadow-[0_0_25px_rgba(59,130,246,0.1)] group hover:shadow-[0_0_35px_rgba(59,130,246,0.2)] transition-all duration-300">
          <div className="flex justify-between items-start relative z-10">
            <div>
              <p className="text-blue-200/60 text-xs font-semibold uppercase tracking-wider">Total Campaigns</p>
              <h3 className="text-3xl font-extrabold text-white mt-2 group-hover:scale-105 origin-left transition-transform duration-300">
                {loading ? '...' : stats.totalCampaigns}
              </h3>
            </div>
            <div className="p-2.5 bg-gradient-to-br from-blue-500 to-cyan-500 shadow-[0_0_15px_rgba(59,130,246,0.4)] text-white rounded-xl">
              <Send className="h-5 w-5" />
            </div>
          </div>
          <div className="absolute -bottom-10 -right-10 w-32 h-32 bg-blue-500/10 rounded-full blur-3xl group-hover:bg-blue-500/20 transition-all duration-500" />
          <div className="text-blue-300/60 text-xs mt-3 flex items-center gap-1 relative z-10">
            <span>Conversational and manual runs</span>
          </div>
        </div>

        <div className="glass-panel p-5 relative overflow-hidden bg-gradient-to-br from-emerald-900/30 to-dark-900 border border-emerald-500/20 shadow-[0_0_25px_rgba(16,185,129,0.1)] group hover:shadow-[0_0_35px_rgba(16,185,129,0.2)] transition-all duration-300">
          <div className="flex justify-between items-start relative z-10">
            <div>
              <p className="text-emerald-200/60 text-xs font-semibold uppercase tracking-wider">Avg. Open Rate</p>
              <h3 className="text-3xl font-extrabold text-white mt-2 group-hover:scale-105 origin-left transition-transform duration-300">
                {loading ? '...' : `${averageOpenRate}%`}
              </h3>
            </div>
            <div className="p-2.5 bg-gradient-to-br from-emerald-400 to-teal-500 shadow-[0_0_15px_rgba(16,185,129,0.4)] text-white rounded-xl">
              <Target className="h-5 w-5" />
            </div>
          </div>
          <div className="absolute -bottom-10 -right-10 w-32 h-32 bg-emerald-500/10 rounded-full blur-3xl group-hover:bg-emerald-500/20 transition-all duration-500" />
          <div className="text-emerald-300/60 text-xs mt-3 flex items-center gap-1 relative z-10">
            <span>Aggregated reach response</span>
          </div>
        </div>

        <div className="glass-panel p-5 relative overflow-hidden bg-gradient-to-br from-amber-900/30 to-dark-900 border border-amber-500/20 shadow-[0_0_25px_rgba(245,158,11,0.1)] group hover:shadow-[0_0_35px_rgba(245,158,11,0.2)] transition-all duration-300">
          <div className="flex justify-between items-start relative z-10">
            <div>
              <p className="text-amber-200/60 text-xs font-semibold uppercase tracking-wider">Avg. CTR</p>
              <h3 className="text-3xl font-extrabold text-white mt-2 group-hover:scale-105 origin-left transition-transform duration-300">
                {loading ? '...' : `${averageCTR}%`}
              </h3>
            </div>
            <div className="p-2.5 bg-gradient-to-br from-amber-400 to-orange-500 shadow-[0_0_15px_rgba(245,158,11,0.4)] text-white rounded-xl">
              <Activity className="h-5 w-5" />
            </div>
          </div>
          <div className="absolute -bottom-10 -right-10 w-32 h-32 bg-amber-500/10 rounded-full blur-3xl group-hover:bg-amber-500/20 transition-all duration-500" />
          <div className="text-amber-300/60 text-xs mt-3 flex items-center gap-1 relative z-10">
            <span>Click conversion rate</span>
          </div>
        </div>

        {/* Conversions KPI */}
        <div className="glass-panel p-5 relative overflow-hidden bg-gradient-to-br from-fuchsia-900/30 to-dark-900 border border-fuchsia-500/30 shadow-[0_0_25px_rgba(217,70,239,0.15)] group hover:shadow-[0_0_35px_rgba(217,70,239,0.25)] transition-all duration-300">
          <div className="absolute inset-0 bg-gradient-to-br from-fuchsia-500/10 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
          <div className="flex justify-between items-start relative z-10">
            <div>
              <p className="text-fuchsia-200/70 text-xs font-semibold uppercase tracking-wider">Conversions</p>
              <h3 className="text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-fuchsia-400 to-pink-400 mt-2 group-hover:scale-105 origin-left transition-transform duration-300">
                {loading ? '...' : totalConversions}
              </h3>
            </div>
            <div className="p-2.5 bg-gradient-to-br from-fuchsia-500 to-pink-600 shadow-[0_0_15px_rgba(217,70,239,0.4)] text-white rounded-xl text-lg animate-[pulse_3s_ease-in-out_infinite]">
              🏆
            </div>
          </div>
          <div className="absolute -bottom-10 -right-10 w-32 h-32 bg-fuchsia-500/20 rounded-full blur-3xl group-hover:bg-fuchsia-500/30 transition-all duration-500" />
          <div className="text-fuchsia-300/60 text-xs mt-3 flex items-center gap-1 relative z-10">
            <span>Orders attributed to campaigns</span>
          </div>
        </div>
      </div>

      {/* Anomaly Detection Alerts (P1 Requirement) */}
      {anomalies.length > 0 && (
        <div className="p-4 bg-amber-500/10 border border-amber-500/20 rounded-xl flex items-start gap-3">
          <AlertTriangle className="h-5 w-5 text-amber-400 flex-shrink-0 mt-0.5" />
          <div>
            <h4 className="text-sm font-semibold text-amber-300">Underperforming Campaign Detected</h4>
            <p className="text-zinc-400 text-xs mt-1">
              The campaign <strong>"{anomalies[0].name}"</strong> is recording unusually low click-through rates ({((anomalies[0].metrics.clicked / anomalies[0].metrics.sent)*100).toFixed(1)}%). Recommended action: adjust variant copy or swap template channel.
            </p>
          </div>
        </div>
      )}

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 md:gap-6">
        {/* Cumulative Funnel */}
        <div className="glass-panel p-5 lg:col-span-1 flex flex-col justify-between">
          <div>
            <h3 className="text-md font-bold text-white mb-1">Conversion Funnel</h3>
            <p className="text-zinc-400 text-xs mb-4">Total performance drop-off aggregates.</p>
          </div>
          <div className="h-[250px] w-full flex justify-center items-center min-w-0 overflow-hidden">
            {totalSent === 0 ? (
              <p className="text-zinc-500 text-sm">No campaigns sent yet.</p>
            ) : (
              <ResponsiveContainer width="100%" height="90%">
                <FunnelChart>
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#18181b', borderColor: '#27272a', borderRadius: '8px' }}
                    itemStyle={{ color: '#fff' }}
                  />
                  <Funnel
                    dataKey="value"
                    data={funnelData}
                    isAnimationActive
                  >
                    <LabelList position="right" fill="#a1a1aa" stroke="none" dataKey="name" />
                  </Funnel>
                </FunnelChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>

        {/* Campaign Comparison Chart */}
        <div className="glass-panel p-5 lg:col-span-2">
          <h3 className="text-md font-bold text-white mb-1">Campaign Comparative Analytics</h3>
          <p className="text-zinc-400 text-xs mb-4">Sent vs Opened vs Clicked counts across campaigns.</p>
          <div className="h-[250px] w-full min-w-0 overflow-hidden">
            {campaignChartData.length === 0 ? (
              <div className="h-full flex flex-col justify-center items-center gap-2">
                <p className="text-zinc-500 text-sm">No campaigns with sent data yet.</p>
                <p className="text-zinc-600 text-xs">Send a campaign to see comparative analytics.</p>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={campaignChartData}>
                  <XAxis dataKey="name" stroke="#71717a" fontSize={10} />
                  <YAxis stroke="#71717a" fontSize={10} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#18181b', borderColor: '#27272a', borderRadius: '8px' }}
                    labelStyle={{ color: '#fff' }}
                  />
                  <Legend wrapperStyle={{ fontSize: '11px', paddingTop: '10px' }} />
                  <Bar dataKey="sent" fill="#6366f1" name="Sent" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="opened" fill="#10b981" name="Opened" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="clicked" fill="#f59e0b" name="Clicked" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </div>
      </div>

      {/* Live Activity Webhook Ticker */}
      <div className="glass-panel p-5">
        <div className="flex items-center justify-between mb-4 border-b border-zinc-800 pb-3">
          <div className="flex items-center gap-2">
            <div className="h-2 w-2 bg-emerald-500 rounded-full animate-ping" />
            <h3 className="text-md font-bold text-white">Live Event Ticker</h3>
          </div>
          <span className="text-[10px] text-zinc-500 uppercase tracking-widest font-mono">Loopback webhook feed</span>
        </div>
        
        {recentEvents.length === 0 ? (
          <div className="p-8 text-center text-zinc-500 text-sm flex flex-col items-center gap-2">
            <Activity className="h-8 w-8 text-zinc-700 animate-pulse" />
            <p>Awaiting loopback webhook events...</p>
            <p className="text-xs text-zinc-650">Create and send a campaign to see live click logs populate.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {recentEvents.map((ev, i) => (
              <div key={ev.id || i} className="p-3 bg-dark-800/30 border border-zinc-800/50 rounded-lg flex justify-between items-center">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold text-white">{ev.recipient}</span>
                    <span className={`text-[9px] px-2 py-0.5 rounded font-mono uppercase font-bold
                       ${ev.status === 'clicked'   ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20' : ''}
                       ${ev.status === 'opened'    ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : ''}
                       ${ev.status === 'read'      ? 'bg-violet-500/10 text-violet-400 border border-violet-500/20' : ''}
                       ${ev.status === 'delivered' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' : ''}
                       ${ev.status === 'failed'    ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20' : ''}
                       ${ev.status === 'converted' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-400/40 ring-1 ring-emerald-500/20' : ''}
                     `}>
                      {ev.status}
                    </span>
                  </div>
                  <p className="text-[10px] text-zinc-500 truncate max-w-[200px]">Campaign: {ev.campaign}</p>
                </div>
                <span className="text-[10px] text-zinc-600 font-mono">{ev.time}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
