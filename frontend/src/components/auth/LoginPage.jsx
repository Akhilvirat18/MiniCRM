import React, { useState } from 'react';
import { useAuth } from '../../AuthContext';
import { Zap, Eye, EyeOff, ArrowRight, Loader2, AlertCircle } from 'lucide-react';

export default function LoginPage({ onSwitchToSignup }) {
  const { login } = useAuth();
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw]     = useState(false);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
    } catch (err) {
      setError(err?.response?.data?.error ?? 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-950 relative overflow-hidden px-4">
      {/* Background glows */}
      <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-indigo-600/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[400px] h-[400px] bg-purple-600/10 rounded-full blur-[100px] pointer-events-none" />

      <div className="w-full max-w-md relative z-10">
        {/* Brand */}
        <div className="flex flex-col items-center mb-8 gap-3">
          <div className="p-3 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-2xl shadow-[0_0_40px_rgba(99,102,241,0.5)] animate-pulse">
            <Zap className="h-7 w-7 text-white" />
          </div>
          <div className="text-center">
            <h1 className="text-3xl font-extrabold bg-clip-text text-transparent bg-gradient-to-r from-indigo-200 via-purple-200 to-fuchsia-200">
              MiniCRM
            </h1>
            <p className="text-zinc-500 text-sm mt-1">AI-Native CRM Platform</p>
          </div>
        </div>

        {/* Card */}
        <div className="bg-dark-900/70 backdrop-blur-xl border border-zinc-800/80 rounded-2xl p-8 shadow-[0_0_60px_rgba(0,0,0,0.5)]">
          <h2 className="text-xl font-bold text-white mb-1">Welcome back</h2>
          <p className="text-zinc-500 text-sm mb-6">Sign in to your workspace</p>

          {error && (
            <div className="mb-5 flex items-start gap-2.5 p-3.5 bg-rose-500/10 border border-rose-500/20 rounded-xl">
              <AlertCircle className="h-4 w-4 text-rose-400 flex-shrink-0 mt-0.5" />
              <p className="text-rose-300 text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Email */}
            <div className="space-y-1.5">
              <label className="text-zinc-400 text-xs font-semibold uppercase tracking-wider">Email</label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="you@company.com"
                className="w-full bg-dark-800/60 border border-zinc-700/60 text-white placeholder-zinc-600 rounded-xl px-4 py-3 text-sm outline-none focus:border-indigo-500/70 focus:ring-2 focus:ring-indigo-500/20 transition-all"
              />
            </div>

            {/* Password */}
            <div className="space-y-1.5">
              <label className="text-zinc-400 text-xs font-semibold uppercase tracking-wider">Password</label>
              <div className="relative">
                <input
                  id="login-password"
                  type={showPw ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-dark-800/60 border border-zinc-700/60 text-white placeholder-zinc-600 rounded-xl px-4 py-3 pr-11 text-sm outline-none focus:border-indigo-500/70 focus:ring-2 focus:ring-indigo-500/20 transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPw(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300 transition-colors"
                  aria-label={showPw ? 'Hide password' : 'Show password'}
                >
                  {showPw ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            {/* Submit */}
            <button
              id="login-submit"
              type="submit"
              disabled={loading}
              className="w-full flex items-center justify-center gap-2 mt-2 px-6 py-3.5 bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 disabled:opacity-60 disabled:cursor-not-allowed text-white font-semibold rounded-xl text-sm shadow-[0_0_20px_rgba(99,102,241,0.3)] hover:shadow-[0_0_30px_rgba(99,102,241,0.45)] transition-all duration-300 active:scale-[0.98]"
            >
              {loading ? (
                <><Loader2 className="h-4 w-4 animate-spin" /> Signing in…</>
              ) : (
                <>Sign In <ArrowRight className="h-4 w-4" /></>
              )}
            </button>
          </form>

          {/* Switch to Signup */}
          <p className="text-center text-zinc-500 text-sm mt-6">
            Don't have an account?{' '}
            <button
              id="go-to-signup"
              onClick={onSwitchToSignup}
              className="text-indigo-400 hover:text-indigo-300 font-semibold transition-colors"
            >
              Create one
            </button>
          </p>
        </div>
      </div>
    </div>
  );
}
