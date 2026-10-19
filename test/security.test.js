const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');

/**
 * Security unit tests simulating Firestore Security Rules access control logic
 * based on firestore.rules definitions.
 */

// Helper to simulate rules logic
function canUpdateSharedProject(auth, resourceData, requestResourceData) {
  if (!auth) return false;
  const isOwner = resourceData.ownerUid === auth.uid;
  const isMember = (resourceData.memberUids && resourceData.memberUids.includes(auth.uid)) ||
    (resourceData.members && auth.email && (resourceData.members.includes(auth.email) || resourceData.members.includes(auth.email.toLowerCase()))) ||
    resourceData.ownerUid === auth.uid;

  // Case 1: Owner updates project (ownerUid cannot be changed)
  if (isOwner && requestResourceData.ownerUid === resourceData.ownerUid) {
    return true;
  }

  // Case 2: Invited member joins and registers their uid into memberUids
  const notYetInMemberUids = !resourceData.memberUids || !resourceData.memberUids.includes(auth.uid);
  if (
    auth.email &&
    resourceData.members &&
    (resourceData.members.includes(auth.email) || resourceData.members.includes(auth.email.toLowerCase())) &&
    notYetInMemberUids
  ) {
    const affectedKeys = Object.keys(requestResourceData).filter(k => requestResourceData[k] !== resourceData[k]);
    const onlyAllowedKeys = affectedKeys.every(k => ['memberUids', 'updatedAt'].includes(k));
    const ownerUidIntact = requestResourceData.ownerUid === resourceData.ownerUid;
    const memberUidsValid = Array.isArray(requestResourceData.memberUids) &&
      requestResourceData.memberUids.includes(auth.uid);
    const existingPreserved = !resourceData.memberUids ||
      resourceData.memberUids.every(uid => requestResourceData.memberUids.includes(uid));

    if (onlyAllowedKeys && ownerUidIntact && memberUidsValid && existingPreserved) {
      return true;
    }
  }

  // Case 3: Regular member updates project progress/details (cannot touch ownerUid, members, or memberUids)
  if (isMember && requestResourceData.ownerUid === resourceData.ownerUid) {
    const affectedKeys = Object.keys(requestResourceData).filter(k => requestResourceData[k] !== resourceData[k]);
    const touchesSecurityFields = affectedKeys.some(k => ['ownerUid', 'members', 'memberUids'].includes(k));
    if (!touchesSecurityFields) {
      return true;
    }
  }

  return false;
}

function canUpdateSharedTask(auth, resourceData, requestResourceData, projectData) {
  if (!auth) return false;
  // projId cannot be altered
  if (requestResourceData.projId !== resourceData.projId) return false;
  
  const isTaskMember = resourceData.memberUids && resourceData.memberUids.includes(auth.uid);
  const isProjectMember = projectData && (
    (projectData.memberUids && projectData.memberUids.includes(auth.uid)) ||
    (projectData.ownerUid === auth.uid)
  );

  return Boolean(isTaskMember || isProjectMember);
}

test('Firestore rules: owner can update project but CANNOT change ownerUid', () => {
  const auth = { uid: 'owner123', email: 'owner@test.com' };
  const current = { ownerUid: 'owner123', name: 'Project A', members: ['owner@test.com'], memberUids: ['owner123'] };

  // Valid update
  assert.equal(canUpdateSharedProject(auth, current, { ...current, name: 'Project A Renovated' }), true);

  // Malicious transfer: change ownerUid
  assert.equal(canUpdateSharedProject(auth, current, { ...current, ownerUid: 'hacker999' }), false);
});

test('Firestore rules: regular member CANNOT tamper with ownerUid, members, or memberUids', () => {
  const auth = { uid: 'member456', email: 'member@test.com' };
  const current = {
    ownerUid: 'owner123',
    name: 'Project A',
    members: ['owner@test.com', 'member@test.com'],
    memberUids: ['owner123', 'member456'],
    status: 'Đang làm'
  };

  // Member updating project status/progress: Allowed
  assert.equal(canUpdateSharedProject(auth, current, { ...current, status: 'Hoàn thành' }), true);

  // Member trying to hijack ownership: Blocked
  assert.equal(canUpdateSharedProject(auth, current, { ...current, ownerUid: 'member456' }), false);

  // Member trying to remove other members: Blocked
  assert.equal(canUpdateSharedProject(auth, current, { ...current, members: ['member@test.com'] }), false);

  // Member trying to alter memberUids: Blocked
  assert.equal(canUpdateSharedProject(auth, current, { ...current, memberUids: ['member456'] }), false);
});

test('Firestore rules: invited user by email can ONLY add their own UID to memberUids', () => {
  const auth = { uid: 'invited789', email: 'invited@test.com' };
  const current = {
    ownerUid: 'owner123',
    name: 'Shared Proj',
    members: ['owner@test.com', 'invited@test.com'],
    memberUids: ['owner123']
  };

  // Legitimate join: adding invited UID to memberUids
  assert.equal(canUpdateSharedProject(auth, current, {
    ...current,
    memberUids: ['owner123', 'invited789']
  }), true);

  // Malicious join: attempting to change name or steal ownerUid while joining
  assert.equal(canUpdateSharedProject(auth, current, {
    ...current,
    ownerUid: 'invited789',
    memberUids: ['owner123', 'invited789']
  }), false);
});

test('Firestore rules: unauthenticated users are completely blocked from updates', () => {
  const current = { ownerUid: 'owner123', name: 'Secret' };
  assert.equal(canUpdateSharedProject(null, current, { ...current, name: 'Hacked' }), false);
});

test('Firestore rules: shared tasks projId cannot be altered across projects', () => {
  const auth = { uid: 'user1', email: 'u1@test.com' };
  const proj = { ownerUid: 'user1', memberUids: ['user1'] };
  const task = { id: 't1', projId: 'projA', text: 'Task 1', memberUids: ['user1'] };

  // Normal update
  assert.equal(canUpdateSharedTask(auth, task, { ...task, text: 'Updated Task 1' }, proj), true);

  // Malicious task moving to hijack another project
  assert.equal(canUpdateSharedTask(auth, task, { ...task, projId: 'projB' }, proj), false);
});
