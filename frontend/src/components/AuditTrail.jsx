import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { ScrollText, Clock, ChevronDown, ChevronRight, FileCode } from 'lucide-react';

export default function AuditTrail() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);

  useEffect(() => {
    api.getAuditLogs().then(data => {
      setLogs([...data].reverse());
    }).finally(() => setLoading(false));
  }, []);

  const typeColor = {
    segment_translation: 'text-indigo-400 border-indigo-500/20 bg-indigo-500/10',
    campaign_planning: 'text-emerald-400 border-emerald-500/20 bg-emerald-500/10',
    message_generation: 'text-amber-400 border-amber-500/20 bg-amber-500/10',
    MOCK_SYSTEM: 'text-zinc-400 border-zinc-700 bg-zinc-800/50',
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-extrabold tracking-tight text-white">AI Audit Trail</h1>
        <p className="text-zinc-400 text-sm mt-1">Full logs of every AI prompt, response, and decision trace for explainability.</p>
      </div>

      <div className="glass-panel p-5">
        {loading ? (
          <div className="flex justify-center py-10">
            <div className="h-6 w-6 border-2 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin" />
          </div>
        ) : logs.length === 0 ? (
          <div className="text-center py-10 text-zinc-500">
            <ScrollText className="h-10 w-10 mx-auto mb-2 text-zinc-700" />
            <p className="text-sm">No audit logs yet. Use the AI Agent or Segment Builder to generate logs.</p>
          </div>
        ) : (
          <div className="space-y-2">
            {logs.map((log, i) => {
              const isOpen = expanded === i;
              const colorClass = typeColor[log.promptType] || typeColor.MOCK_SYSTEM;
              return (
                <div key={log.id || i} className="border border-zinc-800 rounded-lg overflow-hidden">
                  <button
                    onClick={() => setExpanded(isOpen ? null : i)}
                    className="w-full flex items-center gap-3 p-3.5 text-left hover:bg-dark-800/30 transition-colors"
                  >
                    <span className={`text-[10px] px-2 py-0.5 rounded border font-mono font-bold uppercase flex-shrink-0 ${colorClass}`}>
                      {log.promptType?.replace('_', ' ')}
                    </span>
                    <span className="text-white text-xs font-medium flex-1 truncate">{log.userPrompt}</span>
                    <span className="text-zinc-600 text-[10px] font-mono flex-shrink-0 flex items-center gap-1">
                      <Clock className="h-2.5 w-2.5" />
                      {log.timestamp ? new Date(log.timestamp).toLocaleTimeString() : '—'}
                    </span>
                    {isOpen ? <ChevronDown className="h-3.5 w-3.5 text-zinc-500" /> : <ChevronRight className="h-3.5 w-3.5 text-zinc-500" />}
                  </button>

                  {isOpen && (
                    <div className="border-t border-zinc-800 p-4 bg-dark-950/60 space-y-3">
                      <div>
                        <p className="text-zinc-500 text-[10px] uppercase tracking-widest font-semibold mb-1.5">Explanation</p>
                        <p className="text-zinc-300 text-xs">{log.explanation}</p>
                      </div>
                      {log.parsedOutput && (
                        <div>
                          <p className="text-zinc-500 text-[10px] uppercase tracking-widest font-semibold mb-1.5 flex items-center gap-1">
                            <FileCode className="h-3 w-3" /> Parsed Output (Structured JSON)
                          </p>
                          <pre className="bg-dark-950 border border-zinc-800 rounded-lg p-3 text-[11px] text-zinc-300 font-mono overflow-x-auto max-h-64 whitespace-pre-wrap">
                            {(() => {
                              try { return JSON.stringify(JSON.parse(log.parsedOutput), null, 2); }
                              catch { return log.parsedOutput; }
                            })()}
                          </pre>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
