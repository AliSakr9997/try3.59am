# -*- coding: utf-8 -*-
"""
Ed25519 Content-Pack Verification Tool (PR4)
Verifies SHA-256 and Ed25519 digital signature of content packs.
"""
import base64
import hashlib
import json
import sys
from cryptography.hazmat.primitives.asymmetric import ed25519

def verify_pack(pack_path="app/src/main/assets/content_pack_v1.json",
                manifest_path="app/src/main/assets/content_pack_manifest.json"):
    with open(pack_path, "rb") as f:
        pack_bytes = f.read()
    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest = json.load(f)
        
    # Check SHA-256
    expected_sha = manifest["sha256"]
    actual_sha = hashlib.sha256(pack_bytes).hexdigest()
    if expected_sha.lower() != actual_sha.lower():
        print(f"FAILED: SHA-256 mismatch! Expected {expected_sha}, got {actual_sha}")
        return False
        
    # Check Ed25519 signature
    sig_b64 = manifest["signature_ed25519"]
    pub_b64 = manifest["public_key_ed25519"]
    signature = base64.b64decode(sig_b64)
    pub_raw = base64.b64decode(pub_b64)
    
    public_key = ed25519.Ed25519PublicKey.from_public_bytes(pub_raw)
    try:
        public_key.verify(signature, pack_bytes)
        print("SUCCESS: Content pack SHA-256 and Ed25519 signature verified successfully.")
        return True
    except Exception as e:
        print(f"FAILED: Ed25519 signature verification failed: {e}")
        return False

if __name__ == "__main__":
    ok = verify_pack()
    sys.exit(0 if ok else 1)
