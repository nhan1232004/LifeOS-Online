/*
 * One-time, explicit migration. Run `npm run migrate:projects` to inspect and
 * `npm run migrate:projects -- --apply` before deploying restrictive rules.
 * GOOGLE_APPLICATION_CREDENTIALS must point at an authorized service account.
 */
const { applicationDefault, initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore } = require('firebase-admin/firestore');

initializeApp({ credential: applicationDefault() });
const db = getFirestore();
const apply = process.argv.includes('--apply');

(async () => {
  const snapshot = await db.collection('shared_projects').get();
  let changed = 0;
  for (const doc of snapshot.docs) {
    const project = doc.data();
    if (project.ownerUid && Array.isArray(project.memberUids)) continue;
    const emails = Array.isArray(project.members) ? project.members : String(project.members || '').split(',').map(x => x.trim()).filter(Boolean);
    const members = [];
    for (const email of emails) {
      try { members.push((await getAuth().getUserByEmail(email)).uid); }
      catch { console.warn(`Skipping unresolved member ${email} in ${doc.id}`); }
    }
    const ownerUid = project.ownerUid || members[0];
    if (!ownerUid) { console.warn(`Skipping ${doc.id}: no resolvable owner`); continue; }
    const update = { ownerUid, memberUids: [...new Set([ownerUid, ...members])] };
    console.log(`${apply ? 'Migrating' : 'Would migrate'} ${doc.id}`, update);
    if (apply) await doc.ref.update(update);
    changed++;
  }
  // Existing task listeners also use UID membership. Copy the migrated project
  // members onto each legacy task before restrictive rules are deployed.
  const tasks = await db.collection('shared_tasks').get();
  let taskChanges = 0;
  for (const task of tasks.docs) {
    if (Array.isArray(task.data().memberUids)) continue;
    const projectId = task.data().projId;
    if (!projectId) { console.warn(`Skipping task ${task.id}: missing projId`); continue; }
    const project = await db.collection('shared_projects').doc(projectId).get();
    const memberUids = project.exists ? project.data().memberUids : null;
    if (!Array.isArray(memberUids) || !memberUids.length) { console.warn(`Skipping task ${task.id}: project has no migrated members`); continue; }
    console.log(`${apply ? 'Migrating' : 'Would migrate'} task ${task.id}`);
    if (apply) await task.ref.update({ memberUids });
    taskChanges++;
  }
  console.log(`${apply ? 'Migrated' : 'Inspected'} ${changed} project(s) and ${taskChanges} task(s).`);
})().catch(error => { console.error(error); process.exitCode = 1; });
