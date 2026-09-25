const mongoose = require('mongoose');

const PhotoSchema = new mongoose.Schema({
  author: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  imageUrl: {
    type: String,
    required: [true, "L'image est obligatoire"]
  },
  description: {
    type: String,
    required: [true, 'La description est obligatoire'],
    trim: true
  },
  location: {
    type: String,
    required: [true, 'Le lieu est obligatoire'],
    trim: true
  },
  country: {
    type: String,
    required: [true, 'Le pays est obligatoire'],
    trim: true
  },
  locationType: {
    type: String,
    enum: ['Nature', 'Urbain', 'Plage', 'Montagne', 'Musee', 'Restaurant', 'Magasin', 'Rue', 'Autre'],
    default: 'Autre'
  },
  coordinates: {
    type: {
        type: String,
        enum: ['Point'],
        default: 'Point'
    },
    coordinates: {
        type: [Number],  // [longitude, latitude]
        default: [0, 0]
    }
  },
  tags: [{
    type: String,
    trim: true
  }],
  howToGetThere: {
    type: String,
    default: ''
  },
  tips: {
    type: String,
    default: ''
  },
  likes: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User'
  }],
  isPublic: {
    type: Boolean,
    default: true
  },
  groups: [{
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Group'
  }],
  reports: [{
    user:   { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
    reason: { type: String, default: 'Contenu inapproprie' },
    date:   { type: Date, default: Date.now }
  }],
  comments: [{
    user: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    text: {
        type: String,
        required: true,
        trim: true
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
  }],
  createdAt: {
    type: Date,
    default: Date.now
  }
});


PhotoSchema.index({ coordinates: '2dsphere' });
PhotoSchema.index({ location: 'text', description: 'text', tags: 'text' });

PhotoSchema.virtual('latitude').get(function() {
    return this.coordinates.coordinates[1];
});

PhotoSchema.virtual('longitude').get(function() {
    return this.coordinates.coordinates[0];
});

PhotoSchema.set('toJSON', { virtuals: true });

module.exports = mongoose.model('Photo', PhotoSchema);
