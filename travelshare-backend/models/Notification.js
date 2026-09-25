const mongoose = require('mongoose');

const NotificationSchema = new mongoose.Schema({
  recipient: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  type: {
    type: String,
    enum: ['new_photo', 'new_like', 'group_photo', 'new_comment', 'group_invite'],
    required: true
  },
  title: {
    type: String,
    default: ''
  },
  // L'utilisateur qui a déclenché la notification
  sender: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  },
  photo: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Photo'
  },
  group: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Group'
  },
  message: {
    type: String,
    required: true
  },
  isRead: {
    type: Boolean,
    default: false
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  relatedId: {
    type: String,
    default: ''
  }
});

NotificationSchema.post('save', async function (doc) {
  try {
    const User       = require('./User');
    const { sendPush } = require('../config/firebase');
    const recipient  = await User.findById(doc.recipient).select('fcmToken');
    if (recipient && recipient.fcmToken) {
      await sendPush(recipient.fcmToken, doc.title || 'TravelShare', doc.message, {
        type:      doc.type,
        relatedId: doc.relatedId || ''
      });
    }
  } catch (err) {
    console.error('[FCM] post-save hook error:', err.message);
  }
});

module.exports = mongoose.model('Notification', NotificationSchema);
