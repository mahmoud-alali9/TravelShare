const Photo      = require('../models/Photo');
const User       = require('../models/User');
const { annotatePhoto } = require('../config/ai');

// GET /api/photos — toutes les photos publiques
const getPhotos = async (req, res) => {
  try {
    const {
      query: searchQuery,
      type,
      author,
      startDate,
      endDate,
      lat,
      lng,
      radius,
      similarPhotoId,
      page = 1,
      limit = 50
    } = req.query;

    let query = { isPublic: true };

    // Filtre similarité : photos partageant au moins un tag avec la photo de référence
    if (similarPhotoId) {
      const ref = await Photo.findById(similarPhotoId).select('tags _id');
      if (ref && ref.tags.length) {
        query.tags = { $in: ref.tags };
        query._id  = { $ne: ref._id };
      }
    }

    // Recherche partielle (location, country, description, tags, auteur)
    if (searchQuery) {
      const escaped = searchQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const regex = new RegExp(escaped, 'i');

      const matchingUsers = await User.find({
        $or: [{ fullName: regex }, { username: regex }]
      }).select('_id');
      const authorIds = matchingUsers.map(u => u._id);

      query.$or = [
        { location:    regex },
        { country:     regex },
        { description: regex },
        { tags:        regex },
        ...(authorIds.length ? [{ author: { $in: authorIds } }] : [])
      ];
    }
    // Filtre type de lieu
    if (type && type !== 'Tous') {
      query.locationType = type;
    }

    // Filtre date
    if (startDate || endDate) {
      query.createdAt = {};
      if (startDate) query.createdAt.$gte = new Date(startDate);
      if (endDate)   query.createdAt.$lte = new Date(endDate);
    }

    // Filtre géospatial (rayon)
    if (lat && lng && radius) {
      const radiusInRadians = parseFloat(radius) / 6371; // rayon Terre en km
       query.coordinates = {
           $geoWithin: {
               $centerSphere: [
                   [parseFloat(lng), parseFloat(lat)],
                   radiusInRadians
               ]
           }
       };
    }

    // Pagination
    const skip  = (page - 1) * limit;
    const total = await Photo.countDocuments(query);

    const photos = await Photo.find(query)
      .populate('author', 'fullName username avatar')
      .sort({ createdAt: -1 })
      .skip(skip)
      .limit(Number(limit));

    const userId = req.user ? req.user._id.toString() : null;
    const photosWithLikes = photos.map(p => {
      const obj = p.toObject({ virtuals: true });
      obj.likedByMe = userId ? p.likes.map(id => id.toString()).includes(userId) : false;
      return obj;
    });

    res.json({
      total,
      page:       Number(page),
      totalPages: Math.ceil(total / limit),
      photos:     photosWithLikes
    });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// GET /api/photos/:id — une photo par id
const getPhotoById = async (req, res) => {
  try {
    const photo = await Photo.findById(req.params.id)
      .populate('author', 'fullName username avatar');

    if (!photo) {
      return res.status(404).json({ message: 'Photo introuvable' });
    }

    const obj = photo.toObject({ virtuals: true });
    const userId = req.user ? req.user._id.toString() : null;
    obj.likedByMe = userId ? photo.likes.map(id => id.toString()).includes(userId) : false;

    res.json(obj);
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// POST /api/photos — publier une photo (connecté)
const createPhoto = async (req, res) => {
  try {
    const { imageUrl, description, location, country, locationType, tags, howToGetThere, tips, isPublic,
            latitude, longitude, lat, lng } = req.body;

    let parsedTags = tags ? (Array.isArray(tags) ? tags : tags.split(',').map(t => t.trim())) : [];

    // Accepte latitude/longitude ou lat/lng
    const resolvedLat = parseFloat(latitude ?? lat);
    const resolvedLng = parseFloat(longitude ?? lng);
    const hasCoords   = !isNaN(resolvedLat) && !isNaN(resolvedLng);

    const photo = await Photo.create({
      author:       req.user._id,
      imageUrl:     imageUrl || '',
      description,
      location,
      country,
      locationType,
      tags:         parsedTags,
      howToGetThere: howToGetThere || '',
      tips:          tips || '',
      isPublic:      isPublic !== undefined ? isPublic : true,
      ...(hasCoords && {
        coordinates: { type: 'Point', coordinates: [resolvedLng, resolvedLat] }
      })
    });

    // Annotation IA en arrière-plan (ne bloque pas la réponse)
    if (photo.imageUrl) {
      annotatePhoto(photo.imageUrl).then(async (ai) => {
        if (!ai) return;
        const update = {};
        if (ai.tags?.length)   update.tags        = [...new Set([...photo.tags, ...ai.tags])];
        if (ai.description && !photo.description) update.description = ai.description;
        if (Object.keys(update).length) await Photo.findByIdAndUpdate(photo._id, update);
      }).catch(() => {});
    }

    // Notifier les abonnés à cet auteur
    const Subscription = require('../models/Subscription');
    const Notification = require('../models/Notification');
    const subs = await Subscription.find({ type: 'author', targetId: req.user._id.toString() });
    for (const sub of subs) {
      await Notification.create({
        recipient: sub.user,
        sender:    req.user._id,
        type:      'new_photo',
        title:     'Nouvelle photo',
        photo:     photo._id,
        relatedId: photo._id.toString(),
        message:   `${req.user.fullName} a publié une nouvelle photo`
      });
    }

    await photo.populate('author', 'fullName username avatar');
    res.status(201).json(photo);
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// PUT /api/photos/:id/like — liker / unliker une photo
const toggleLike = async (req, res) => {
    try {
        const Notification = require('../models/Notification');
        const User = require('../models/User');
        
        const photo = await Photo.findById(req.params.id);
        if (!photo) return res.status(404).json({ message: 'Photo introuvable' });

        // Charger l'utilisateur complet
        const currentUser = await User.findById(req.user._id);

        const userId = req.user._id.toString();
        const alreadyLiked = photo.likes.map(id => id.toString()).includes(userId);

        if (alreadyLiked) {
            photo.likes = photo.likes.filter(id => id.toString() !== userId);
        } else {
            photo.likes.push(req.user._id);

            if (photo.author.toString() !== req.user._id.toString()) {
                await Notification.create({
                    recipient: photo.author,
                    sender:    req.user._id,
                    type:      'new_like',
                    title:     'Nouveau like',
                    photo:     photo._id,
                    relatedId: photo._id.toString(),
                    message:   currentUser.fullName + ' a aimé votre photo'
                });
            }
        }

        await photo.save();

        res.json({
            liked:     !alreadyLiked,
            likeCount: photo.likes.length
        });
    } catch (error) {
        res.status(500).json({ message: 'Erreur serveur', error: error.message });
    }
};

// POST /api/photos/:id/report — signaler une photo
const reportPhoto = async (req, res) => {
  try {
    const photo = await Photo.findById(req.params.id);
    if (!photo) return res.status(404).json({ message: 'Photo introuvable' });

    const alreadyReported = photo.reports.some(
      r => r.user.toString() === req.user._id.toString()
    );

    if (alreadyReported) {
      return res.status(400).json({ message: 'Vous avez déjà signalé cette photo' });
    }

    photo.reports.push({
      user:   req.user._id,
      reason: req.body.reason || 'Contenu inapproprié'
    });

    await photo.save();

    res.json({ message: 'Photo signalée, merci pour votre vigilance' });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// GET /api/photos/random — flux aléatoire
const getRandomPhotos = async (req, res) => {
  try {
    const photos = await Photo.aggregate([
      { $match: { isPublic: true } },
      { $sample: { size: 10 } }
    ]);
    await Photo.populate(photos, { path: 'author', select: 'fullName username avatar' });
    res.json(photos);
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// GET /api/photos/:id/comments — liste des commentaires
const getComments = async (req, res) => {
    try {
        const photo = await Photo.findById(req.params.id)
            .populate('comments.user', 'fullName username avatar initial');

        if (!photo) {
            return res.status(404).json({ message: 'Photo introuvable' });
        }

        res.json({
            total: photo.comments.length,
            comments: photo.comments
        });
    } catch (error) {
        res.status(500).json({ message: 'Erreur serveur', error: error.message });
    }
};

// POST /api/photos/:id/comments — ajouter un commentaire
const addComment = async (req, res) => {
    try {
        const { text } = req.body;

        if (!text || text.trim() === '') {
            return res.status(400).json({ message: 'Le commentaire ne peut pas être vide' });
        }

        const photo = await Photo.findById(req.params.id);
        if (!photo) {
            return res.status(404).json({ message: 'Photo introuvable' });
        }

        // Ajouter le commentaire
        photo.comments.push({
            user: req.user._id,
            text: text.trim()
        });

        await photo.save();

        // Populer le dernier commentaire ajouté
        await photo.populate('comments.user', 'fullName username avatar initial');

        const newComment = photo.comments[photo.comments.length - 1];

        // Créer une notification pour l'auteur de la photo
        if (photo.author.toString() !== req.user._id.toString()) {
            const Notification = require('../models/Notification');
            await Notification.create({
                recipient: photo.author,
                sender:    req.user._id,
                type:      'new_comment',
                photo:     photo._id,
                relatedId: photo._id.toString(),
                message:   req.user.fullName + ' a commenté votre photo'
            });
        }

        res.status(201).json({
            message: 'Commentaire ajouté',
            comment: newComment
        });
    } catch (error) {
        res.status(500).json({ message: 'Erreur serveur', error: error.message });
    }
};

// DELETE /api/photos/:id/comments/:commentId — supprimer un commentaire
const deleteComment = async (req, res) => {
    try {
        const photo = await Photo.findById(req.params.id);
        if (!photo) {
            return res.status(404).json({ message: 'Photo introuvable' });
        }

        const comment = photo.comments.id(req.params.commentId);
        if (!comment) {
            return res.status(404).json({ message: 'Commentaire introuvable' });
        }

        // Vérifier que c'est bien l'auteur du commentaire
        if (comment.user.toString() !== req.user._id.toString()) {
            return res.status(403).json({ message: 'Non autorisé' });
        }

        comment.deleteOne();
        await photo.save();

        res.json({ message: 'Commentaire supprimé' });
    } catch (error) {
        res.status(500).json({ message: 'Erreur serveur', error: error.message });
    }
};



module.exports = {
    getPhotos,
    getPhotoById,
    createPhoto,
    toggleLike,
    reportPhoto,
    getRandomPhotos,
    getComments,
    addComment,
    deleteComment
};
