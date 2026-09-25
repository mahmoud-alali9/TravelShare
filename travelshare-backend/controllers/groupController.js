const Group = require('../models/Group');
const Photo = require('../models/Photo');

// GET /api/groups/mine — groupes de l'utilisateur connecté
const getMyGroups = async (req, res) => {
  try {
    const groups = await Group.find({ members: req.user._id })
      .populate('creator', 'fullName username')
      .populate('members', 'fullName username avatar')
      .sort({ createdAt: -1 });

    res.json(groups);
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// POST /api/groups — créer un groupe
const createGroup = async (req, res) => {
  try {
    const { name, description, coverImage } = req.body;

    const group = await Group.create({
      name,
      description,
      coverImage: coverImage || 'https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800',
      creator: req.user._id,
      members: [req.user._id]
    });

    res.status(201).json({ message: 'Groupe créé', group });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// PUT /api/groups/:id/join — rejoindre un groupe
const joinGroup = async (req, res) => {
  try {
    const group = await Group.findById(req.params.id);
    if (!group) return res.status(404).json({ message: 'Groupe introuvable' });

    const alreadyMember = group.members
      .map(id => id.toString())
      .includes(req.user._id.toString());

    if (alreadyMember) {
      return res.status(400).json({ message: 'Vous êtes déjà membre de ce groupe' });
    }

    group.members.push(req.user._id);
    await group.save();

    res.json({ message: 'Vous avez rejoint le groupe', group });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// PUT /api/groups/:id/leave — quitter un groupe
const leaveGroup = async (req, res) => {
  try {
    const group = await Group.findById(req.params.id);
    if (!group) return res.status(404).json({ message: 'Groupe introuvable' });

    group.members = group.members.filter(
      id => id.toString() !== req.user._id.toString()
    );
    await group.save();

    res.json({ message: 'Vous avez quitté le groupe' });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// GET /api/photos/me — mes photos
const getMyPhotos = async (req, res) => {
  try {
    const photos = await Photo.find({ author: req.user._id })
      .populate('author', 'fullName username avatar')
      .sort({ createdAt: -1 });
    res.json({ photos });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// POST /api/groups/:id/photos — ajouter une photo à un groupe
const addPhotoToGroup = async (req, res) => {
  try {
    const group = await Group.findById(req.params.id);
    if (!group) {
      return res.status(404).json({ message: 'Groupe introuvable' });
    }

    // Vérifier que l'utilisateur est membre
    const isMember = group.members
      .map(id => id.toString())
      .includes(req.user._id.toString());

    if (!isMember) {
      return res.status(403).json({ message: 'Vous devez être membre du groupe' });
    }

    const { photoId } = req.body;
    if (!photoId) {
      return res.status(400).json({ message: 'photoId manquant' });
    }

    // Vérifier si la photo existe
    const photo = await Photo.findById(photoId);
    if (!photo) {
      return res.status(404).json({ message: 'Photo introuvable' });
    }

    // Vérifier si la photo est déjà dans le groupe
    const alreadyAdded = group.photos
      .map(id => id.toString())
      .includes(photoId);

    if (alreadyAdded) {
      return res.status(400).json({ message: 'Photo déjà dans le groupe' });
    }

    group.photos.push(photoId);
    await group.save();

    res.json({ message: 'Photo ajoutée au groupe', group });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// DELETE /api/groups/:id/photos/:photoId — retirer une photo d'un groupe
const removePhotoFromGroup = async (req, res) => {
  try {
    const { id: groupId, photoId } = req.params;
    const group = await Group.findById(groupId);
    if (!group) {
      return res.status(404).json({ message: 'Groupe introuvable' });
    }

    // Vérifier que l'utilisateur est membre ou créateur
    const userId = req.user._id.toString();
    const isMember = group.members.map(m => m.toString()).includes(userId);
    const isCreator = group.creator.toString() === userId;
    if (!isMember && !isCreator) {
      return res.status(403).json({ message: 'Vous devez être membre du groupe ou son créateur' });
    }

    const wasPresent = group.photos.map(p => p.toString()).includes(photoId);
    if (!wasPresent) {
      return res.status(404).json({ message: 'Photo non présente dans le groupe' });
    }

    group.photos = group.photos.filter(p => p.toString() !== photoId);
    await group.save();

    res.json({ message: 'Photo retirée du groupe', group });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// GET /api/groups/:id/photos — photos d'un groupe
const getGroupPhotos = async (req, res) => {
  try {
    const group = await Group.findById(req.params.id)
      .populate({
        path: 'photos',
        populate: { path: 'author', select: 'fullName username avatar' }
      });

    if (!group) {
      return res.status(404).json({ message: 'Groupe introuvable' });
    }

    res.json({ photos: group.photos });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

// GET /api/groups/discover — tous les groupes pour découvrir
const discoverGroups = async (req, res) => {
  try {
    const groups = await Group.find()
      .populate('creator', 'fullName username')
      .populate('members', 'fullName username avatar')
      .sort({ createdAt: -1 });
    res.json(groups);
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur', error: error.message });
  }
};

const deleteGroup = async (req, res) => {
  try {
    const group = await Group.findById(req.params.id);
    
    if (!group) {
      return res.status(404).json({ message: 'Groupe introuvable' });
    }

    // Sécurité : Seul le créateur peut supprimer le groupe
    if (group.creator.toString() !== req.user._id.toString()) {
      return res.status(403).json({ message: 'Action non autorisée : vous n\'êtes pas le créateur' });
    }

    await Group.findByIdAndDelete(req.params.id);
    
    res.status(200).json({ message: 'Groupe supprimé avec succès' });
  } catch (error) {
    res.status(500).json({ message: 'Erreur serveur lors de la suppression', error: error.message });
  }
};

module.exports = {
  getMyGroups,
  createGroup,
  joinGroup,
  leaveGroup,
  getMyPhotos,
  addPhotoToGroup,
  removePhotoFromGroup,
  getGroupPhotos,
  discoverGroups,
  deleteGroup
};