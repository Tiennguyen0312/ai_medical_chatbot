import json
import pandas as pd

TRAIN_CSV = "train_10d_symptoms.csv"  
OUT = "disease_profiles.json"

df = pd.read_csv(TRAIN_CSV)
df = df.loc[:, ~df.columns.str.contains("^Unnamed")]
label_col = "diseases"  

if label_col not in df.columns:
    raise ValueError(f"Missing label column '{label_col}'. Columns are: {list(df.columns)[:20]} ...")
symptoms = [c for c in df.columns if c != label_col]
profiles = {}
THRESH = 0.15  
for d, g in df.groupby(label_col):
    freq = g[symptoms].mean()               
    prof = freq[freq >= THRESH].sort_values(ascending=False)
    profiles[str(d)] = list(prof.index)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(profiles, f, ensure_ascii=False, indent=2)
print("Saved:", OUT, "diseases:", len(profiles))
print("Example:", list(profiles.keys())[:3])
