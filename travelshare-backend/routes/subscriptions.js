const express = require('express');
const router  = express.Router();
const { protect } = require('../middleware/authMiddleware');
const { getSubscriptions, subscribe, unsubscribe, unsubscribeByTarget } = require('../controllers/subscriptionController');

router.get('/',         protect, getSubscriptions);
router.post('/',        protect, subscribe);
router.delete('/by',    protect, unsubscribeByTarget);
router.delete('/:id',   protect, unsubscribe);

module.exports = router;
