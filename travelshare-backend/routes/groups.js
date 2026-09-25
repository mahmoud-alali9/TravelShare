const express = require('express');
const router  = express.Router();
const { protect } = require('../middleware/authMiddleware');
const {
    getMyGroups,
    createGroup,
    joinGroup,
    leaveGroup,
    deleteGroup, // 1. Ajoute l'import ici
    getGroupPhotos,
    addPhotoToGroup,
    removePhotoFromGroup,
    discoverGroups
} = require('../controllers/groupController');

router.get('/',                          protect, getMyGroups);
router.post('/',                         protect, createGroup);
router.get('/discover',                  protect, discoverGroups);
router.put('/:id/join',                  protect, joinGroup);
router.put('/:id/leave',                 protect, leaveGroup); 
router.delete('/:id',                    protect, deleteGroup); // 2. Ajoute cette ligne
router.get('/:id/photos',                protect, getGroupPhotos);
router.post('/:id/photos',               protect, addPhotoToGroup);
router.delete('/:id/photos/:photoId',    protect, removePhotoFromGroup);

module.exports = router;