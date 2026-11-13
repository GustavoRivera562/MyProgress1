import { readFile } from "node:fs/promises";
import { after, before, beforeEach, test } from "node:test";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  writeBatch
} from "firebase/firestore";

const projectId = "demo-myprogres-rules";
const workoutId = "11111111-1111-4111-8111-111111111111";
const secondWorkoutId = "22222222-2222-4222-8222-222222222222";
const setId = "33333333-3333-4333-8333-333333333333";
const intakeId = "44444444-4444-4444-8444-444444444444";
const now = 1_800_000_000_000;

let env;

function workout(uid = "userA", id = workoutId, overrides = {}) {
  return {
    id,
    ownerUid: uid,
    startedAt: now,
    finishedAt: null,
    ...overrides
  };
}

function setRecord(parentId = workoutId, id = setId, overrides = {}) {
  return {
    id,
    workoutId: parentId,
    exerciseId: "squat",
    exerciseName: "Sentadilla",
    setNumber: 1,
    weightKg: 80,
    repetitions: 8,
    recordedAt: now + 1,
    ...overrides
  };
}

function water(uid = "userA", id = intakeId, overrides = {}) {
  return {
    id,
    ownerUid: uid,
    amountMl: 500,
    loggedAt: now,
    ...overrides
  };
}

function workoutRef(db, uid = "userA", id = workoutId) {
  return doc(db, "users", uid, "workouts", id);
}

function setRef(db, uid = "userA", parentId = workoutId, id = setId) {
  return doc(db, "users", uid, "workouts", parentId, "sets", id);
}

function waterRef(db, uid = "userA", id = intakeId) {
  return doc(db, "users", uid, "water_intake", id);
}

async function seedWorkout(uid = "userA", id = workoutId) {
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(workoutRef(context.firestore(), uid, id), workout(uid, id));
  });
}

async function seedSet(uid = "userA", parentId = workoutId, id = setId) {
  await seedWorkout(uid, parentId);
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(setRef(context.firestore(), uid, parentId, id), setRecord(parentId, id));
  });
}

before(async () => {
  const rules = await readFile(new URL("../firestore.rules", import.meta.url), "utf8");
  env = await initializeTestEnvironment({
    projectId,
    firestore: { rules }
  });
});

beforeEach(async () => {
  await env.clearFirestore();
});

after(async () => {
  await env?.cleanup();
});

test("Workout: el usuario A crea y lee su propio documento", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  await assertSucceeds(getDoc(workoutRef(db)));
});

test("Workout: A puede listar solo su colección propia", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  await assertSucceeds(getDocs(collection(db, "users", "userA", "workouts")));
});

test("Workout: el usuario B no puede leer el documento de A", async () => {
  await seedWorkout();
  const dbB = env.authenticatedContext("userB").firestore();
  await assertFails(getDoc(workoutRef(dbB, "userA")));
});

test("Workout: el usuario B no puede listar la colección de A", async () => {
  await seedWorkout();
  const dbB = env.authenticatedContext("userB").firestore();
  await assertFails(getDocs(collection(dbB, "users", "userA", "workouts")));
});

test("Workout: usuario no autenticado no puede leer ni crear", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(workoutRef(db)));
  await assertFails(setDoc(workoutRef(db), workout()));
});

test("Workout: el UID del documento debe coincidir con el usuario y la ruta", async () => {
  const dbA = env.authenticatedContext("userA").firestore();
  await assertFails(setDoc(workoutRef(dbA, "userA", secondWorkoutId), workout("userB", secondWorkoutId)));
  await assertFails(setDoc(workoutRef(dbA, "userB", secondWorkoutId), workout("userB", secondWorkoutId)));
});

test("Workout: campos obligatorios, extra y tipos incorrectos se rechazan", async () => {
  const db = env.authenticatedContext("userA").firestore();
  const missing = workout();
  delete missing.startedAt;
  const extra = workout("userA", secondWorkoutId, { privateNote: "extra" });
  const wrongStartedAt = workout("userA", secondWorkoutId, { startedAt: "hoy" });
  const wrongFinishedAt = workout("userA", secondWorkoutId, { finishedAt: "mañana" });

  await assertFails(setDoc(workoutRef(db, "userA", secondWorkoutId), missing));
  await assertFails(setDoc(workoutRef(db, "userA", secondWorkoutId), extra));
  await assertFails(setDoc(workoutRef(db, "userA", secondWorkoutId), wrongStartedAt));
  await assertFails(setDoc(workoutRef(db, "userA", secondWorkoutId), wrongFinishedAt));
});

test("Workout: startedAt debe ser positivo y finishedAt no puede precederlo", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertFails(setDoc(workoutRef(db), workout("userA", workoutId, { startedAt: 0 })));
  await assertFails(setDoc(workoutRef(db), workout("userA", workoutId, { startedAt: -1 })));
  await assertFails(setDoc(workoutRef(db), workout("userA", workoutId, { finishedAt: now - 1 })));
});

test("Workout: finishedAt null está permitido al crear un entrenamiento activo", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
});

test("Workout: solo permite actualizar finishedAt y conserva los campos inmutables", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  await assertSucceeds(updateDoc(workoutRef(db), { finishedAt: now + 1000 }));
  await assertFails(updateDoc(workoutRef(db), { startedAt: now + 10 }));
  await assertFails(updateDoc(workoutRef(db), { ownerUid: "userB" }));
  await assertFails(updateDoc(workoutRef(db), { id: secondWorkoutId }));
  await assertFails(updateDoc(workoutRef(db), { privateNote: "extra" }));
});

test("Workout: finishedAt no puede volver a null ni cambiar después de finalizar", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  await assertSucceeds(updateDoc(workoutRef(db), { finishedAt: now + 1000 }));
  await assertFails(updateDoc(workoutRef(db), { finishedAt: null }));
  await assertFails(updateDoc(workoutRef(db), { finishedAt: now + 2000 }));
});

test("Workout: reintento idéntico de actualización se permite; borrado se deniega", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  await assertSucceeds(updateDoc(workoutRef(db), { finishedAt: now + 1000 }));
  await assertSucceeds(setDoc(workoutRef(db), workout("userA", workoutId, { finishedAt: now + 1000 })));
  await assertFails(deleteDoc(workoutRef(db)));
});

test("Sets: creación en el mismo batch que su Workout usa getAfter()", async () => {
  const db = env.authenticatedContext("userA").firestore();
  const batch = writeBatch(db);
  batch.set(workoutRef(db), workout());
  batch.set(setRef(db), setRecord());
  await assertSucceeds(batch.commit());
  await assertSucceeds(getDoc(setRef(db)));
});

test("Sets: un usuario puede leer y crear series bajo su Workout", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  await assertSucceeds(setDoc(setRef(db), setRecord()));
  await assertSucceeds(getDoc(setRef(db)));
  await assertSucceeds(getDocs(collection(db, "users", "userA", "workouts", workoutId, "sets")));
});

test("Sets: B no puede leer ni crear bajo el Workout de A", async () => {
  await seedWorkout();
  const dbB = env.authenticatedContext("userB").firestore();
  await assertFails(getDoc(setRef(dbB, "userA")));
  await assertFails(getDocs(collection(dbB, "users", "userA", "workouts", workoutId, "sets")));
  await assertFails(setDoc(setRef(dbB, "userA"), setRecord()));
});

test("Sets: usuario no autenticado no puede leer ni crear", async () => {
  await seedWorkout();
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(setRef(db)));
  await assertFails(setDoc(setRef(db), setRecord()));
});

test("Sets: el padre debe existir y pertenecer al UID de la ruta", async () => {
  const dbA = env.authenticatedContext("userA").firestore();
  await assertFails(setDoc(setRef(dbA), setRecord()));
  await seedWorkout("userB");
  await assertFails(setDoc(setRef(dbA, "userA", workoutId), setRecord()));
});

test("Sets: ID, workoutId, campos obligatorios y campos extra se validan", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));

  await assertFails(setDoc(setRef(db), setRecord(workoutId, secondWorkoutId)));
  await assertFails(setDoc(setRef(db), setRecord(workoutId, setId, { workoutId: secondWorkoutId })));

  const missing = setRecord(workoutId, secondWorkoutId);
  delete missing.exerciseId;
  await assertFails(setDoc(setRef(db, "userA", workoutId, secondWorkoutId), missing));

  await assertFails(setDoc(
    setRef(db, "userA", workoutId, secondWorkoutId),
    setRecord(workoutId, secondWorkoutId, { unexpected: true })
  ));
});

test("Sets: exerciseId y exerciseName son strings obligatorios de 1 a 60 caracteres", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));

  for (const invalid of [
    { exerciseId: "" },
    { exerciseId: "x".repeat(61) },
    { exerciseId: 123 },
    { exerciseName: "" },
    { exerciseName: "x".repeat(61) },
    { exerciseName: 123 }
  ]) {
    await assertFails(setDoc(setRef(db), setRecord(workoutId, setId, invalid)));
  }
});

test("Sets: setNumber, weightKg y repetitions respetan tipo y rango", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));

  for (const invalid of [
    { setNumber: 0 },
    { setNumber: 1.5 },
    { setNumber: "1" },
    { weightKg: -0.01 },
    { weightKg: 1000.01 },
    { weightKg: "80" },
    { repetitions: 0 },
    { repetitions: 1001 },
    { repetitions: 1.5 },
    { repetitions: "8" }
  ]) {
    await assertFails(setDoc(setRef(db), setRecord(workoutId, setId, invalid)));
  }
});

test("Sets: recordedAt debe ser un entero positivo", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  await assertFails(setDoc(setRef(db), setRecord(workoutId, setId, { recordedAt: 0 })));
  await assertFails(setDoc(setRef(db), setRecord(workoutId, setId, { recordedAt: "ahora" })));
});

test("Sets: un reintento idéntico pasa, una modificación posterior falla", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(workoutRef(db), workout()));
  const ref = setRef(db);
  await assertSucceeds(setDoc(ref, setRecord()));
  await assertSucceeds(setDoc(ref, setRecord()));
  await assertFails(setDoc(ref, setRecord(workoutId, setId, { repetitions: 9 })));
  await assertFails(deleteDoc(ref));
});

test("Water: A puede crear y leer su propio registro; B no puede acceder", async () => {
  const dbA = env.authenticatedContext("userA").firestore();
  await assertSucceeds(setDoc(waterRef(dbA), water()));
  await assertSucceeds(getDoc(waterRef(dbA)));

  const dbB = env.authenticatedContext("userB").firestore();
  await assertFails(getDoc(waterRef(dbB, "userA")));
  await assertFails(getDocs(collection(dbB, "users", "userA", "water_intake")));
});

test("Water: usuario no autenticado no puede leer ni crear", async () => {
  const db = env.unauthenticatedContext().firestore();
  await assertFails(getDoc(waterRef(db)));
  await assertFails(setDoc(waterRef(db), water()));
});

test("Water: UID, ID, campos obligatorios y campos extra se validan", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertFails(setDoc(waterRef(db), water("userB")));
  await assertFails(setDoc(waterRef(db, "userA", secondWorkoutId), water("userA", intakeId)));

  const missing = water("userA", secondWorkoutId);
  delete missing.loggedAt;
  await assertFails(setDoc(waterRef(db, "userA", secondWorkoutId), missing));

  await assertFails(setDoc(
    waterRef(db, "userA", secondWorkoutId),
    water("userA", secondWorkoutId, { extra: "no" })
  ));
});

test("Water: amountMl debe ser entero entre 1 y 2.000", async () => {
  const db = env.authenticatedContext("userA").firestore();
  for (const amountMl of [0, -1, 2001, 1.5, "500"]) {
    await assertFails(setDoc(waterRef(db), water("userA", intakeId, { amountMl })));
  }
  await assertSucceeds(setDoc(waterRef(db), water()));
});

test("Water: loggedAt debe ser un entero positivo", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertFails(setDoc(waterRef(db), water("userA", intakeId, { loggedAt: 0 })));
  await assertFails(setDoc(waterRef(db), water("userA", intakeId, { loggedAt: "hoy" })));
});

test("Water: reintento idéntico pasa; mutación y borrado fallan", async () => {
  const db = env.authenticatedContext("userA").firestore();
  const ref = waterRef(db);
  await assertSucceeds(setDoc(ref, water()));
  await assertSucceeds(setDoc(ref, water()));
  await assertFails(setDoc(ref, water("userA", intakeId, { amountMl: 600 })));
  await assertFails(deleteDoc(ref));
});

test("Rutas no contempladas permanecen denegadas", async () => {
  const db = env.authenticatedContext("userA").firestore();
  await assertFails(getDoc(doc(db, "public", "unexpected")));
  await assertFails(setDoc(doc(db, "users", "userA", "private_notes", "note1"), { value: "x" }));
});
