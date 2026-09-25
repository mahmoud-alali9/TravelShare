const jwt  = require('jsonwebtoken');
const User = require('../models/User');

// Génère un token JWT
const generateToken = (id) => {
  return jwt.sign({ id }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN
  });
};

// POST /api/auth/register
const register = async (req, res) => {
  try {
    const { fullName, username, email, password } = req.body;

    // Vérifier si l'email ou le username existe déjà
    const existingUser = await User.findOne({ $or: [{ email }, { username }] });
    if (existingUser) {
      return res.status(400).json({
        message: existingUser.email === email
          ? 'Cet email est déjà utilisé'
          : "Ce nom d'utilisateur est déjà pris"
      });
    }

    // Créer l'utilisateur
    const user = await User.create({ fullName, username, email, password });

    res.status(201).json({
      message: 'Compte créé avec succès',
      token: generateToken(user._id),
      user: {
        _id:      user._id,
        fullName: user.fullName,
        username: user.username,
        email:    user.email,
        avatar:   user.avatar
      }
    });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// POST /api/auth/login
const login = async (req, res) => {
  try {
    const { email, password } = req.body;

    // Vérifier si l'utilisateur existe (inclure le mot de passe)
    const user = await User.findOne({ email }).select('+password');
    if (!user) {
      return res.status(401).json({ message: 'Email ou mot de passe incorrect' });
    }

    // Vérifier le mot de passe
    const isMatch = await user.matchPassword(password);
    if (!isMatch) {
      return res.status(401).json({ message: 'Email ou mot de passe incorrect' });
    }

    res.json({
      message: 'Connexion réussie',
      token: generateToken(user._id),
      user: {
        _id:      user._id,
        fullName: user.fullName,
        username: user.username,
        email:    user.email,
        avatar:   user.avatar
      }
    });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// GET /api/auth/me
const getMe = async (req, res) => {
  res.json({
    user: {
      _id:       req.user._id,
      fullName:  req.user.fullName,
      username:  req.user.username,
      email:     req.user.email,
      avatar:    req.user.avatar,
      createdAt: req.user.createdAt
    }
  });
};

// PUT /api/auth/fcm-token
const saveFcmToken = async (req, res) => {
  try {
    const { fcmToken } = req.body;
    if (!fcmToken) return res.status(400).json({ message: 'fcmToken manquant' });
    await User.findByIdAndUpdate(req.user._id, { fcmToken });
    res.json({ message: 'FCM token enregistré' });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

module.exports = { register, login, getMe, saveFcmToken };
