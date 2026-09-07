const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

initializeApp();
const db = getFirestore();

exports.inviteProjectMember = onCall(async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign in is required.');
  const projectId = String(request.data?.projectId || '').trim();
  const email = String(request.data?.email || '').trim().toLowerCase();
  if (!projectId || !email || !email.includes('@')) throw new HttpsError('invalid-argument', 'A project and valid email are required.');

  const projectRef = db.collection('shared_projects').doc(projectId);
  const project = await projectRef.get();
  if (!project.exists || project.data().ownerUid !== request.auth.uid) {
    throw new HttpsError('permission-denied', 'Only the project owner can invite members.');
  }
  let user;
  try { user = await getAuth().getUserByEmail(email); }
  catch { throw new HttpsError('not-found', 'No LifeOS account exists for this email.'); }

  await projectRef.update({
    memberUids: FieldValue.arrayUnion(user.uid),
    members: FieldValue.arrayUnion(email)
  });
  return { ok: true, uid: user.uid };
});
