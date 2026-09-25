const https = require('https');

async function annotatePhoto(imageUrl) {
  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey || !imageUrl) return null;

  const body = JSON.stringify({
    model: 'claude-haiku-4-5-20251001',
    max_tokens: 300,
    messages: [{
      role: 'user',
      content: [
        { type: 'image', source: { type: 'url', url: imageUrl } },
        {
          type: 'text',
          text: 'Analyze this travel photo. Reply with JSON only, no extra text: {"tags":["tag1","tag2","tag3","tag4","tag5"],"description":"one short sentence describing the scene"}'
        }
      ]
    }]
  });

  return new Promise((resolve) => {
    const req = https.request({
      hostname: 'api.anthropic.com',
      path: '/v1/messages',
      method: 'POST',
      headers: {
        'x-api-key': apiKey,
        'anthropic-version': '2023-06-01',
        'content-type': 'application/json',
        'content-length': Buffer.byteLength(body)
      }
    }, (res) => {
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => {
        try {
          const result = JSON.parse(data);
          const text   = result.content?.[0]?.text || '';
          const match  = text.match(/\{[\s\S]*\}/);
          resolve(match ? JSON.parse(match[0]) : null);
        } catch { resolve(null); }
      });
    });
    req.on('error', () => resolve(null));
    req.write(body);
    req.end();
  });
}

module.exports = { annotatePhoto };
