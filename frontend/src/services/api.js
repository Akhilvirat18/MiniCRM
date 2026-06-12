import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

const client = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const api = {
  // Ingestion & Basic Data
  getCustomers: () => client.get('/customers').then(res => res.data),
  getOrders: () => client.get('/orders').then(res => res.data),
  ingestCustomers: (data) => client.post('/customers/ingest', data).then(res => res.data),
  ingestOrders: (data) => client.post('/orders/ingest', data).then(res => res.data),
  resetDatabase: () => client.post('/reset').then(res => res.data),

  // Segments
  getSegments: () => client.get('/segments').then(res => res.data),
  getSegmentById: (id) => client.get(`/segments/${id}`).then(res => res.data),
  getSegmentCustomers: (id) => client.get(`/segments/${id}/customers`).then(res => res.data),
  createSegmentAI: (description, name) => client.post('/segments/ai', { description, name }).then(res => res.data),
  saveSegment: (segment) => client.post('/segments', segment).then(res => res.data),
  deleteSegment: (id) => client.delete(`/segments/${id}`).then(res => res.data),
  previewFilter: (filterRules) => client.post('/segments/preview', filterRules).then(res => res.data),
  getSegmentSuggestions: () => client.get('/segments/ai-suggestions').then(res => res.data),

  // Campaigns
  getCampaigns: () => client.get('/campaigns').then(res => res.data),
  getCampaignById: (id) => client.get(`/campaigns/${id}`).then(res => res.data),
  createCampaign: (campaign) => client.post('/campaigns', campaign).then(res => res.data),
  sendCampaign: (id) => client.post(`/campaigns/${id}/send`).then(res => res.data),
  getCampaignCommunications: (id) => client.get(`/campaigns/${id}/communications`).then(res => res.data),

  // AI Campaign Agent Sessions
  getAgentSessions: () => client.get('/agent/sessions').then(res => res.data),
  getAgentSessionById: (id) => client.get(`/agent/sessions/${id}`).then(res => res.data),
  initiateCampaignPlan: (goal, maxDiscountPercent) => 
    client.post('/agent/campaign-plan', { goal, constraints: { max_discount_percent: maxDiscountPercent } }).then(res => res.data),
  refineCampaignPlan: (sessionId, editGoal) => 
    client.post(`/agent/sessions/${sessionId}/refine`, { goal: editGoal }).then(res => res.data),
  approveCampaignPlan: (sessionId) => 
    client.post(`/agent/sessions/${sessionId}/approve`).then(res => res.data),
  
  // Explainability Logs
  getAuditLogs: () => client.get('/agent/audit-logs').then(res => res.data),
};
