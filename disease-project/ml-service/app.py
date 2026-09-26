from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any, Set
import pandas as pd
from pathlib import Path
from datetime import datetime
import joblib, json, re

MODEL_PATH = "rf_disease_model.pkl"
COLS_PATH = "symptom_columns.json"
ADVICE_PATH = "treatment_advice.json"

TRAIN_CSV_PATH = "train_10d_symptoms.csv"   
PROFILE_FREQ_THRESHOLD = 0.20              
RERANK_TOPK = 15                           
RERANK_F1_WEIGHT = 0.7                     

MIN_MATCHED_SYMPTOMS = 4

app = FastAPI(title="Disease Prediction ML Service", version="1.3")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)
def norm(s: str) -> str:
    s = (s or "").lower().strip()
    s = s.replace("_", " ")
    s = re.sub(r"[^\w\s]", " ", s)
    s = re.sub(r"\s+", " ", s)
    return s
if not Path(MODEL_PATH).exists():
    raise RuntimeError(f"Missing model file: {MODEL_PATH}")
rf = joblib.load(MODEL_PATH)
if not Path(COLS_PATH).exists():
    raise RuntimeError(f"Missing columns file: {COLS_PATH}")
with open(COLS_PATH, "r", encoding="utf-8") as f:
    SYMPTOM_COLS: List[str] = json.load(f)
ADVICE: Dict[str, Any] = {}
if Path(ADVICE_PATH).exists():
    with open(ADVICE_PATH, "r", encoding="utf-8") as f:
        ADVICE = json.load(f)
if not ADVICE:
    print("treatment_advice.json not loaded or empty.")
else:
    print(f"loaded treatment advice{len(ADVICE)}")

LOOKUP = {norm(c): c for c in SYMPTOM_COLS}


DISEASE_PROFILES: Dict[str, Set[str]] = {}

if Path(TRAIN_CSV_PATH).exists():
    df_train = pd.read_csv(TRAIN_CSV_PATH)
    df_train = df_train.loc[:, ~df_train.columns.str.contains("^Unnamed")]
    if "diseases" in df_train.columns:
        feat_cols = [c for c in df_train.columns if c != "diseases"]
        freq = df_train.groupby("diseases")[feat_cols].mean()

        for disease in freq.index:
            cols = freq.loc[disease]
            DISEASE_PROFILES[norm(str(disease))] = set(
                cols[cols >= PROFILE_FREQ_THRESHOLD].index
            )

        print(f"loaded DISEASE_PROFILES {len(DISEASE_PROFILES)} diseases from {TRAIN_CSV_PATH}")
    else:
        print("TRAIN_CSV_PATH found but missing 'diseases' column")
else:
    print(f" {TRAIN_CSV_PATH} not found.")

def build_vector(text: str, symptoms: Optional[List[str]] = None):
    t = norm(text)
    matched: List[str] = []
    unmatched: List[str] = []
    for k, col in LOOKUP.items():
        if k and k in t:
            matched.append(col)
    if symptoms:
        for s in symptoms:
            k = norm(s)
            if k in LOOKUP:
                matched.append(LOOKUP[k])
            else:
                unmatched.append(s)

    matched = sorted(list(set(matched)))
    vec = {c: 0 for c in SYMPTOM_COLS}
    for c in matched:
        vec[c] = 1

    X = pd.DataFrame([vec], columns=SYMPTOM_COLS)
    return X, matched, unmatched

def pick_advice(disease: str) -> Dict[str, Any]:
    disease_key = norm(disease)
    if disease_key in ADVICE:
        return ADVICE[disease_key]
    return ADVICE.get("_default", {
        "self_care": ["Rest and stay hydrated"],
        "otc": ["Follow the product label and dosing instructions"],
        "avoid": ["Do not self-prescribe antibiotics or prescription-only medicines"],
        "go_hospital_if": ["Breathing difficulty or chest pain", "Symptoms worsen rapidly"]
    })

def rerank_with_profile(
    proba,
    classes: List[str],
    base_idx,
    matched: List[str],
) -> List[int]:
    if not DISEASE_PROFILES:
        return base_idx[:3].tolist()
    matched_set = set(matched)
    scored = []
    for i in base_idx[:RERANK_TOPK]:
        disease = classes[i]
        p = float(proba[i])

        profile = DISEASE_PROFILES.get(norm(disease), set())
        if not profile:
            scored.append((p * 0.95, i))
            continue

        overlap = len(matched_set & profile)
        precision = overlap / max(1, len(matched_set))
        recall = overlap / max(1, len(profile))
        f1 = 0.0 if (precision + recall) == 0 else (2 * precision * recall / (precision + recall))

        score = p * (1.0 + RERANK_F1_WEIGHT * f1)
        scored.append((score, i))

    scored.sort(reverse=True, key=lambda x: x[0])
    return [i for _, i in scored[:3]]

class PredictReq(BaseModel):
    text: Optional[str] = Field(default="", description="User symptom description in plain text.")
    symptoms: Optional[List[str]] = Field(default=None, description="Optional explicit symptom list")

class PredictOut(BaseModel):
    timestamp_utc: str
    matched_symptoms: List[str]
    unmatched_symptoms: List[str]
    best: Dict[str, Any]
    top3: List[Dict[str, Any]]
    advice: Dict[str, Any]
    disclaimer: str

@app.get("/health")
def health():
    return {
        "status": "ok",
        "time_utc": datetime.utcnow().isoformat() + "Z",
        "features": len(SYMPTOM_COLS),
        "model_type": type(rf).__name__,
        "advice_loaded": bool(ADVICE),
        "advice_keys": len(ADVICE) if ADVICE else 0,
        "profiles_loaded": bool(DISEASE_PROFILES),
        "profiles_count": len(DISEASE_PROFILES) if DISEASE_PROFILES else 0,
        "min_matched_symptoms": MIN_MATCHED_SYMPTOMS,
    }

@app.get("/meta")
def meta():
    classes = getattr(rf, "classes_", [])
    return {
        "model": type(rf).__name__,
        "artifact_files": [MODEL_PATH, COLS_PATH, ADVICE_PATH, TRAIN_CSV_PATH],
        "num_features": len(SYMPTOM_COLS),
        "num_classes": len(classes) if len(classes) else None,
        "classes_sample": list(map(str, classes[:10])) if len(classes) else None,
        "notes": "Academic demo."
    }

@app.get("/examples")
def examples():
    return {
        "examples": [
            {"text": "I have itchy eyes and runny nose"},
            {"text": "I have headache and nausea"},
            {"text": "I have burning when I pee and frequent urination"},
            {"text": "I have cough, sneezing and runny nose"},
            {"text": "I have skin rash and itching"}
        ],       
    }

@app.get("/disease/{name}/symptoms")
def disease_symptoms(name: str):
    if not DISEASE_PROFILES:
        raise HTTPException(status_code=400, detail="Disease profiles not loaded (missing train CSV).")
    key = norm(name)
    prof = DISEASE_PROFILES.get(key)
    if not prof:
        raise HTTPException(status_code=404, detail=f"No profile for disease: {name}")
    return {
        "disease": name,
        "threshold": PROFILE_FREQ_THRESHOLD,
        "symptom_count": len(prof),
        "symptoms": sorted(list(prof)),
    }

@app.post("/predict", response_model=PredictOut)
def predict(req: PredictReq):
    try:
        X, matched, unmatched = build_vector(req.text or "", req.symptoms)

    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to build feature vector: {e}")

    if len(matched) < MIN_MATCHED_SYMPTOMS:
        raise HTTPException(
            status_code=422,
            detail={
                "message": f"Not enough matched symptoms. Need at least {MIN_MATCHED_SYMPTOMS}.",
                "matched_symptoms": matched,
                "unmatched_symptoms": unmatched,
                "hint": "Use dataset column names (e.g., 'nasal congestion' instead of 'runny nose')."
            }
        )
    if hasattr(rf, "predict_proba"):
        proba = rf.predict_proba(X)[0]
        classes = list(map(str, rf.classes_))
        base_idx = proba.argsort()[::-1]  
        if len(matched) <= 4 and DISEASE_PROFILES:
            top_idx = rerank_with_profile(proba, classes, base_idx, matched)
        else:
            top_idx = base_idx[:3].tolist()
        top3 = [{"disease": classes[i], "confidence": float(proba[i])} for i in top_idx]
        best = top3[0]
    else:
        pred = rf.predict(X)[0]
        top3 = [{"disease": str(pred), "confidence": None}]
        best = top3[0]
    advice = pick_advice(best["disease"])
    return {
        "timestamp_utc": datetime.utcnow().isoformat() + "Z",
        "matched_symptoms": matched,
        "unmatched_symptoms": unmatched,
        "best": best,
        "top3": top3,
        "advice": advice,
        "disclaimer": "Academic demo only. "
    }
