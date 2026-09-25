const express = require('express');
const router  = express.Router();
const { register, login, getMe, saveFcmToken } = require('../controllers/authController');
const { protect } = require('../middleware/authMiddleware');

// POST /api/auth/register
router.post('/register', register);

// POST /api/auth/login
router.post('/login', login);

// GET /api/auth/me  (connecté)
router.get('/me', protect, getMe);

// PUT /api/auth/fcm-token  (connecté)
router.put('/fcm-token', protect, saveFcmToken);

module.exports = router;
