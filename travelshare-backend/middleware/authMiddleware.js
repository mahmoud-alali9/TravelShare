const jwt  = require('jsonwebtoken');
const User = require('../models/User');

// Middleware : vérifie le token JWT
const protect = async (req, res, next) => {
  let token;

  if (req.headers.authorization &&
      req.headers.authorization.startsWith('Bearer')) {
    try {
      // Extraire le token
      token = req.headers.authorization.split(' ')[1];

      // Vérifier et décoder
      const decoded = jwt.verify(token, process.env.JWT_SECRET);

      // Attacher l'utilisateur à la requête (sans le mot de passe)
      req.user = await User.findById(decoded.id).select('-password');

      next();
    } catch (error) {
      return res.status(401).json({ message: 'Token invalide ou expiré' });
    }
  }

  if (!token) {
    return res.status(401).json({ message: 'Accès refusé, token manquant' });
  }
};

// Middleware optionnel : attache l'utilisateur si connecté, sinon continue
const optionalAuth = async (req, res, next) => {
  let token;

  if (req.headers.authorization &&
      req.headers.authorization.startsWith('Bearer')) {
    try {
      token = req.headers.authorization.split(' ')[1];
      const decoded = jwt.verify(token, process.env.JWT_SECRET);
      req.user = await User.findById(decoded.id).select('-password');
    } catch (error) {
      req.user = null;
    }
  }

  next();
};

module.exports = { protect, optionalAuth };
