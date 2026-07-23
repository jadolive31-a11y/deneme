const admin = require("firebase-admin");
const readline = require("readline");

// Initialize Firebase Admin SDK using the correct project ID
const projectId = "futbolcubul-tr";

admin.initializeApp({
  projectId: projectId
});

const db = admin.firestore();
const auth = admin.auth();

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

async function clearEcosystem(adminUid) {
  if (!adminUid) {
    console.error("Hata: Lütfen korunacak Admin UID değerini parametre olarak belirtin.");
    console.error("Kullanım: node clearEcosystem.js <ADMIN_UID>");
    process.exit(1);
  }

  console.log(`=============================================`);
  console.log(`FUTBOLCUM ECOSYSTEM CLEANER SCRIPT`);
  console.log(`=============================================`);
  console.log(`Hedef Firebase Projesi: ${projectId}`);
  console.log(`Korunacak Aktif Admin UID: ${adminUid}`);
  console.log(`=============================================\n`);

  // 1. SILME ONCESI RAPOR (SAYIM)
  console.log("Mevcut veriler sayılıyor...");
  const beforeCounts = {};
  for (const coll of collections) {
    try {
      const snap = await db.collection(coll).get();
      beforeCounts[coll] = snap.size;
    } catch (e) {
      beforeCounts[coll] = "ERİŞİLEMEDİ";
    }
  }
  
  let totalAuthUsersBefore = 0;
  const authUidsToDelete = [];
  try {
    let nextPageToken;
    do {
      const listUsersResult = await auth.listUsers(1000, nextPageToken);
      listUsersResult.users.forEach((userRecord) => {
        totalAuthUsersBefore++;
        if (userRecord.uid !== adminUid) {
          authUidsToDelete.push(userRecord.uid);
        }
      });
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);
  } catch (error) {
    console.error("Auth sayım hatası:", error);
    totalAuthUsersBefore = "ERİŞİLEMEDİ";
  }

  console.log("\n--- Silme Öncesi Sunucu Sayıları ---");
  console.log(`Firebase Authentication Hesapları: ${totalAuthUsersBefore}`);
  for (const coll of collections) {
    console.log(`Collection '${coll}': ${beforeCounts[coll]} belge`);
  }
  console.log("------------------------------------\n");

  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });

  rl.question("BU İŞLEM GERÇEK VERİLERİ KALICI OLARAK SİLECEKTİR. DEVAM ETMEK İSTİYOR MUSUNUZ? (yes/no): ", async (answer) => {
    if (answer.trim().toLowerCase() !== "yes") {
      console.log("İşlem iptal edildi.");
      rl.close();
      process.exit(0);
    }

    console.log("\nTemizlik işlemi başlatılıyor...\n");

    // 2. AUTHENTICATION TEMIZLIGI
    let deletedAuthCount = 0;
    if (authUidsToDelete.length > 0) {
      console.log(`Admin haricindeki ${authUidsToDelete.length} Authentication hesabı siliniyor...`);
      for (let i = 0; i < authUidsToDelete.length; i += 1000) {
        const batch = authUidsToDelete.slice(i, i + 1000);
        try {
          const result = await auth.deleteUsers(batch);
          deletedAuthCount += result.successCount;
        } catch (e) {
          console.error("Auth silme hatası batch:", e);
        }
      }
      console.log(`Başarıyla silinen Auth hesabı: ${deletedAuthCount}`);
    } else {
      console.log("Silinecek başka Auth hesabı bulunamadı.");
    }

    // 3. FIRESTORE TEMIZLIGI
    let totalFirestoreDeleted = 0;
    for (const coll of collections) {
      console.log(`'${coll}' koleksiyonu temizleniyor...`);
      try {
        const snap = await db.collection(coll).get();
        if (snap.empty) {
          console.log(`-> '${coll}' koleksiyonu boş.`);
          continue;
        }

        const docs = snap.docs;
        const chunks = [];
        for (let i = 0; i < docs.length; i += 450) {
          chunks.push(docs.slice(i, i + 450));
        }

        let deletedInColl = 0;
        for (const chunk of chunks) {
          const batch = db.batch();
          let ops = 0;
          for (const doc of chunk) {
            if (coll === "users" && doc.id === adminUid) {
              continue; // Admin belgesini koru
            }
            batch.delete(doc.ref);
            ops++;
          }
          if (ops > 0) {
            await batch.commit();
            deletedInColl += ops;
            totalFirestoreDeleted += ops;
          }
        }
        console.log(`-> '${coll}' koleksiyonundan ${deletedInColl} belge silindi.`);
      } catch (e) {
        console.error(`-> '${coll}' koleksiyonu silinirken hata oluştu:`, e.message);
      }
    }

    // 4. SILME SONRASI RAPOR (SAYIM)
    console.log("\nSon durum doğrulanıyor...");
    const afterCounts = {};
    for (const coll of collections) {
      try {
        const snap = await db.collection(coll).get();
        afterCounts[coll] = snap.size;
      } catch (e) {
        afterCounts[coll] = "ERİŞİLEMEDİ";
      }
    }
    
    let totalAuthUsersAfter = 0;
    try {
      let nextPageToken;
      do {
        const listUsersResult = await auth.listUsers(1000, nextPageToken);
        totalAuthUsersAfter += listUsersResult.users.length;
        nextPageToken = listUsersResult.pageToken;
      } while (nextPageToken);
    } catch (e) {
      totalAuthUsersAfter = "ERİŞİLEMEDİ";
    }

    console.log("\n=== İŞLEM TAMAMLANDI ===");
    console.log("--- Silme Sonrası Sunucu Sayıları ---");
    console.log(`Firebase Authentication Hesapları: ${totalAuthUsersAfter}`);
    for (const coll of collections) {
      console.log(`Collection '${coll}': ${afterCounts[coll]} belge`);
    }
    console.log("========================================\n");

    rl.close();
    process.exit(0);
  });
}

// Extract UID parameter
const adminUid = process.argv[2];
clearEcosystem(adminUid);
