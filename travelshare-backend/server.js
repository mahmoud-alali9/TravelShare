const express   = require('express');
const cors      = require('cors');
const dotenv    = require('dotenv');
const os        = require('os');
const connectDB = require('./config/db');

dotenv.config();
connectDB();

const app = express();

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ── Verbose request logger ─────────────────────────────────────
app.use((req, res, next) => {
  const start = Date.now();
  const sep = '─'.repeat(60);
  console.log(`\n${sep}`);
  console.log(`▶  ${req.method} ${req.originalUrl}`);
  console.log(`   IP      : ${req.ip}`);
  if (Object.keys(req.params).length)  console.log(`   Params  :`, req.params);
  if (Object.keys(req.query).length)   console.log(`   Query   :`, req.query);
  if (req.body && Object.keys(req.body).length) {
    const safe = { ...req.body };
    if (safe.password) safe.password = '***';
    console.log(`   Body    :`, safe);
  }
  res.on('finish', () => {
    const ms = Date.now() - start;
    console.log(`◀  ${res.statusCode} ${req.method} ${req.originalUrl}  (${ms}ms)`);
    console.log(sep);
  });
  next();
});

// Exemple de route qui reçoit uniquement une URL (photoUrl) dans le JSON
app.post('/api/travels', async (req, res) => {
  try {
    const travelData = req.body;
    // travelData.photoUrl doit être fourni par le client (ex: Cloudinary)
    if (!travelData.photoUrl) {
      return res.status(400).json({ message: 'photoUrl manquant' });
    }
    // ...enregistrer travelData dans la base de données...
    res.status(201).json({ message: 'Travel créé', travel: travelData });
  } catch (err) {
    res.status(500).json({ message: 'Erreur serveur', error: err.message });
  }
});

// ── Routes ────────────────────────────────────────────────────
app.use('/api/auth',          require('./routes/auth'));
app.use('/api/photos',        require('./routes/photos'));
app.use('/api/groups',        require('./routes/groups'));
app.use('/api/notifications', require('./routes/notifications'));
app.use('/api/subscriptions', require('./routes/subscriptions'));

app.get('/', (req, res) => {
  res.json({ message: 'TravelShare API fonctionne' });
});

app.use((req, res) => {
  res.status(404).json({ message: 'Route introuvable' });
});

function getLanIp() {
  for (const ifaces of Object.values(os.networkInterfaces())) {
    for (const iface of ifaces) {
      if (iface.family === 'IPv4' && !iface.internal) return iface.address;
    }
  }
  return '0.0.0.0';
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
  const lan = getLanIp();
  console.log(`\nTravelShare API running`);
  console.log(`  Local   : http://localhost:${PORT}`);
  console.log(`  Network : http://${lan}:${PORT}  ← use this on Android\n`);
});
