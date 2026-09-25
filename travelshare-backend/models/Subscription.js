const mongoose = require('mongoose');

const SubscriptionSchema = new mongoose.Schema({
  user: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  type: {
    type: String,
    enum: ['author', 'group', 'location', 'theme'],
    required: true
  },
  targetId: {
    type: String,
    required: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  }
});

SubscriptionSchema.index({ user: 1, type: 1, targetId: 1 }, { unique: true });

module.exports = mongoose.model('Subscription', SubscriptionSchema);
