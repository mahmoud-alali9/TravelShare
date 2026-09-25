const mongoose = require('mongoose');

const GroupSchema = new mongoose.Schema({
  name: {
    type: String,
    required: [true, 'Le nom du groupe est obligatoire'],
    trim: true
  },
  description: {
    type: String,
    default: ''
  },
  creator: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  members: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  coverImage: {
    type: String,
    default: 'https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800'
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  photos: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Photo'
}]
});

module.exports = mongoose.model('Group', GroupSchema);
