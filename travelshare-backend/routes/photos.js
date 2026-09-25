const express = require('express');
const router  = express.Router();
const Photo = require('../models/Photo');
const {
    getPhotos,
    getPhotoById,
    createPhoto,
    toggleLike,
    reportPhoto,
    getRandomPhotos,
    getComments,
    addComment,
    deleteComment
} = require('../controllers/photoController');
const { protect, optionalAuth } = require('../middleware/authMiddleware');
const { upload } = require('../config/cloudinary');

// GET /api/photos — toutes les photos (anonyme OK)
router.get('/',        optionalAuth, getPhotos);

// GET /api/photos/random — flux aléatoire (anonyme OK)
router.get('/random',  getRandomPhotos);

// GET /api/photos/me — mes photos (connecté)
router.get('/me', protect, async (req, res) => {
    try {
        const photos = await Photo.find({ author: req.user._id })
            .populate('author', 'fullName username avatar')
            .sort({ createdAt: -1 });
        res.json({ photos });
    } catch (error) {
        res.status(500).json({ message: 'Erreur serveur', error: error.message });
    }
});

// POST /api/photos/upload — uploader une image vers Cloudinary
router.post('/upload', protect, upload.single('image'), (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ message: 'Aucune image reçue' });
        }
        res.json({ imageUrl: req.file.path });
    } catch (error) {
        res.status(500).json({ message: 'Erreur upload', error: error.message });
    }
});

// GET /api/photos/:id — une photo (anonyme OK)
router.get('/:id',     optionalAuth, getPhotoById);

// POST /api/photos — publier (connecté)
router.post('/', protect, createPhoto);

router.post('/:id/toggle-like', protect, toggleLike);

// POST /api/photos/:id/report — signaler (connecté)
router.post('/:id/report', protect, reportPhoto);

router.get('/:id/comments',               optionalAuth, getComments);
router.post('/:id/comments',              protect,      addComment);
router.delete('/:id/comments/:commentId', protect,      deleteComment);

// DELETE /api/photos/:id — supprimer une photo
router.delete('/:id', protect, async (req, res) => {
    try {
        const photo = await Photo.findById(req.params.id);
        if (!photo) return res.status(404).json({ message: 'Photo non trouvée' });
        if (photo.author.toString() !== req.user._id.toString()) {
            return res.status(401).json({ message: 'Non autorisé' });
        }

        const Group = require('../models/Group');
        await Group.updateMany(
            { photos: photo._id },
            { $pull: { photos: photo._id } }
        );

        const Notification = require('../models/Notification');
        await Notification.deleteMany({ photo: photo._id });

        await photo.deleteOne();
        res.json({ message: 'Photo supprimée' });
    } catch (error) {
        res.status(500).json({ message: 'Erreur serveur', error: error.message });
    }
});

module.exports = router;