const https  = require('https');
const jwt    = require('jsonwebtoken');

const FCM_SCOPE = 'https://www.googleapis.com/auth/firebase.messaging';

let _cachedToken  = null;
let _tokenExpiry  = 0;

function serviceAccount() {
  try {
    return JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT || '{}');
  } catch {
    return {};
  }
}

function post(hostname, path, headers, body) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(body);
    const req = https.request(
      { hostname, path, method: 'POST', headers: { ...headers, 'Content-Length': Buffer.byteLength(payload) } },
      (res) => {
        let data = '';
        res.on('data', c => data += c);
        res.on('end', () => resolve({ status: res.statusCode, body: JSON.parse(data) }));
      }
    );
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

async function getAccessToken(sa) {
  if (_cachedToken && Date.now() < _tokenExpiry - 60000) return _cachedToken;

  const now = Math.floor(Date.now() / 1000);
  const assertion = jwt.sign(
    { iss: sa.client_email, sub: sa.client_email, aud: 'https://oauth2.googleapis.com/token',
      iat: now, exp: now + 3600, scope: FCM_SCOPE },
    sa.private_key,
    { algorithm: 'RS256' }
  );

  const formBody = `grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=${assertion}`;
  const result = await new Promise((resolve, reject) => {
    const req = https.request(
      { hostname: 'oauth2.googleapis.com', path: '/token', method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded',
                   'Content-Length': Buffer.byteLength(formBody) } },
      (res) => { let d = ''; res.on('data', c => d += c); res.on('end', () => resolve(JSON.parse(d))); }
    );
    req.on('error', reject);
    req.write(formBody);
    req.end();
  });

  _cachedToken = result.access_token;
  _tokenExpiry = Date.now() + 3600000;
  return _cachedToken;
}

async function sendPush(fcmToken, title, body, data = {}) {
  const sa = serviceAccount();
  if (!sa.project_id || !fcmToken) return;

  try {
    const accessToken = await getAccessToken(sa);
    const stringData  = {};
    for (const [k, v] of Object.entries(data)) stringData[k] = String(v);

    const result = await post(
      'fcm.googleapis.com',
      `/v1/projects/${sa.project_id}/messages:send`,
      { 'Authorization': `Bearer ${accessToken}`, 'Content-Type': 'application/json' },
      { message: { token: fcmToken, notification: { title, body }, data: stringData } }
    );

    if (result.status >= 400) {
      console.error('[FCM] Error:', result.body);
    }
  } catch (err) {
    console.error('[FCM] Send failed:', err.message);
  }
}

module.exports = { sendPush };
