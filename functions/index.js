const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.resetEcosystem = functions.region("europe-west3").https.onCall(async (data, context) => {
  // 1. Authentication check
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Bu işlemi gerçekleştirmek için giriş yapmalısınız."
    );
  }

  const callerUid = context.auth.uid;
  const db = admin.firestore();

  // Retrieve caller info to verify Admin role from firestore
  const callerSnap = await db.collection("users").doc(callerUid).get();
  if (!callerSnap.exists) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "Yönetici profili mevcut değil. Temizlik işlemi başlatılamadı."
    );
  }

  const callerData = callerSnap.data();
  const isRoleAdmin = callerData && callerData.role === "ADMIN";
  const isTokenAdmin = context.auth.token.admin === true;

  if (!isRoleAdmin && !isTokenAdmin) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Bu işlemi gerçekleştirmek için yetkiniz yok."
    );
  }

  const collections = [
    "players",
    "clubs",
    "follows",
    "likes",
    "chats",
    "notifications",
    "activities",
    "applications",
    "users",
    "tournaments"
  ];

  let deletedAuthUsers = 0;
  let failedUsers = 0;
  let deletedFirestoreDocuments = 0;
  let failedDocuments = 0;
  let authCleanupFailed = false;
  let verificationFailed = false;

  // 2. Clean Firebase Authentication Users (except the logged-in admin)
  try {
    let nextPageToken;
    const uidsToDelete = [];

    do {
      const listUsersResult = await admin.auth().listUsers(1000, nextPageToken);
      listUsersResult.users.forEach((userRecord) => {
        if (userRecord.uid !== callerUid) {
          uidsToDelete.push(userRecord.uid);
        }
      });
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);

    if (uidsToDelete.length > 0) {
      for (let i = 0; i < uidsToDelete.length; i += 1000) {
        const batchUids = uidsToDelete.slice(i, i + 1000);
        try {
          const deleteResult = await admin.auth().deleteUsers(batchUids);
          deletedAuthUsers += deleteResult.successCount;
          failedUsers += deleteResult.failureCount;
          if (deleteResult.failureCount > 0) {
            authCleanupFailed = true;
          }
        } catch (err) {
          console.error("Batch auth delete error:", err);
          authCleanupFailed = true;
          failedUsers += batchUids.length;
        }
      }
    }
  } catch (error) {
    console.error("Auth list/cleaning error:", error);
    authCleanupFailed = true;
    verificationFailed = true;
  }

  // 3. Clean Firestore Collections
  for (const collName of collections) {
    try {
      const collRef = db.collection(collName);
      const snapshot = await collRef.get();
      
      if (snapshot.empty) {
        continue;
      }

      // Batch delete documents in chunks of 450 to avoid Firestore limits
      const docs = snapshot.docs;
      const chunks = [];
      for (let i = 0; i < docs.length; i += 450) {
        chunks.push(docs.slice(i, i + 450));
      }

      for (const chunk of chunks) {
        const batch = db.batch();
        let opCount = 0;

        for (const doc of chunk) {
          if (collName === "users" && doc.id === callerUid) {
            // Keep the admin user document
            continue;
          }
          batch.delete(doc.ref);
          opCount++;
        }

        if (opCount > 0) {
          await batch.commit();
          deletedFirestoreDocuments += opCount;
        }
      }
    } catch (error) {
      console.error(`Error cleaning collection ${collName}:`, error);
      failedDocuments += 1;
    }
  }

  // 4. Verification Check (counts after delete operations)
  let remainingDocuments = 0;
  for (const collName of collections) {
    try {
      const collRef = db.collection(collName);
      const snapshot = await collRef.get();
      if (collName === "users") {
        remainingDocuments += Math.max(0, snapshot.size - 1);
      } else {
        remainingDocuments += snapshot.size;
      }
    } catch (e) {
      console.error(`Verification failed for collection ${collName}:`, e);
      verificationFailed = true;
    }
  }

  // 5. Auth Verification Check
  let remainingAuthUsers = 0;
  try {
    let nextPageToken;
    do {
      const listUsersResult = await admin.auth().listUsers(1000, nextPageToken);
      remainingAuthUsers += listUsersResult.users.length;
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);
  } catch (e) {
    console.error("Verification of Auth users failed:", e);
    verificationFailed = true;
  }

  const success = !authCleanupFailed &&
                  !verificationFailed &&
                  remainingDocuments === 0 &&
                  remainingAuthUsers === 1 &&
                  failedDocuments === 0 &&
                  failedUsers === 0;

  return {
    success: success,
    deletedAuthUsers: deletedAuthUsers,
    deletedFirestoreDocuments: deletedFirestoreDocuments,
    failedDocuments: failedDocuments,
    failedUsers: failedUsers,
    remainingDocuments: remainingDocuments,
    remainingAuthUsers: remainingAuthUsers,
    verificationFailed: verificationFailed
  };
});
