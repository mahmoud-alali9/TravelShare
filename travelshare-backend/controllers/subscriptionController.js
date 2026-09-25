const Subscription = require('../models/Subscription');

// GET /api/subscriptions
const getSubscriptions = async (req, res) => {
  try {
    const subs = await Subscription.find({ user: req.user._id });
    res.json(subs);
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// POST /api/subscriptions  { type, targetId }
const subscribe = async (req, res) => {
  try {
    const { type, targetId } = req.body;
    if (!type || !targetId) return res.status(400).json({ message: 'type et targetId requis' });

    const sub = await Subscription.findOneAndUpdate(
      { user: req.user._id, type, targetId },
      { user: req.user._id, type, targetId },
      { upsert: true, new: true }
    );
    res.status(201).json(sub);
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// DELETE /api/subscriptions/:id
const unsubscribe = async (req, res) => {
  try {
    await Subscription.findOneAndDelete({ _id: req.params.id, user: req.user._id });
    res.json({ message: 'Abonnement supprimé' });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// DELETE /api/subscriptions?type=author&targetId=xxx
const unsubscribeByTarget = async (req, res) => {
  try {
    const { type, targetId } = req.query;
    await Subscription.findOneAndDelete({ user: req.user._id, type, targetId });
    res.json({ message: 'Abonnement supprimé' });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

module.exports = { getSubscriptions, subscribe, unsubscribe, unsubscribeByTarget };
