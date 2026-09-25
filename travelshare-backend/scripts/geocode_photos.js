/**
 * Script de géocodage des photos existantes.
 * Trouve toutes les photos avec coordonnées [0,0] et les met à jour
 * via l'API Nominatim (OpenStreetMap) en utilisant le champ location/country.
 *
 * Usage : node scripts/geocode_photos.js
 */

const https    = require('https');
const mongoose = require('mongoose');
const dotenv   = require('dotenv');

dotenv.config({ path: require('path').join(__dirname, '../.env') });

const Photo = require('../models/Photo');

// ── Nominatim geocoding ────────────────────────────────────────

function geocode(location, country) {
    const query = [location, country].filter(Boolean).join(', ');
    const encoded = encodeURIComponent(query);
    const url = `https://nominatim.openstreetmap.org/search?q=${encoded}&format=json&limit=1`;

    return new Promise((resolve) => {
        const options = {
            headers: { 'User-Agent': 'TravelShare-Geocoder/1.0' }
        };
        https.get(url, options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const results = JSON.parse(data);
                    if (results.length > 0) {
                        resolve({
                            lat: parseFloat(results[0].lat),
                            lng: parseFloat(results[0].lon)
                        });
                    } else {
                        resolve(null);
                    }
                } catch {
                    resolve(null);
                }
            });
        }).on('error', () => resolve(null));
    });
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

// ── Main ───────────────────────────────────────────────────────

async function run() {
    await mongoose.connect(process.env.MONGO_URI);
    console.log('MongoDB connecté');

    // Photos avec coordonnées à [0,0] ou nulles
    const photos = await Photo.find({
        'coordinates.coordinates': [0, 0]
    }).select('_id location country coordinates');

    console.log(`${photos.length} photo(s) à géocoder\n`);

    let updated = 0;
    let failed  = 0;

    for (const photo of photos) {
        const label = `${photo.location || '?'}, ${photo.country || '?'}`;
        process.stdout.write(`[${photo._id}] ${label} → `);

        if (!photo.location) {
            console.log('ignoré (pas de lieu)');
            failed++;
            continue;
        }

        // Respect du rate-limit Nominatim (1 req/s)
        await sleep(1100);

        const coords = await geocode(photo.location, photo.country);

        if (!coords) {
            console.log('introuvable');
            failed++;
            continue;
        }

        await Photo.findByIdAndUpdate(photo._id, {
            coordinates: {
                type: 'Point',
                coordinates: [coords.lng, coords.lat]
            }
        });

        console.log(`${coords.lat.toFixed(4)}, ${coords.lng.toFixed(4)} ✓`);
        updated++;
    }

    console.log(`\nTerminé : ${updated} mis à jour, ${failed} non trouvés`);
    await mongoose.disconnect();
}

run().catch(err => {
    console.error(err);
    process.exit(1);
});
