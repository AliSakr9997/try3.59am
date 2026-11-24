# -*- coding: utf-8 -*-
"""
Ed25519 Content-Pack Signer (PR4)
Signs clinician-approved Arabic content packs for standalone recovery.
Generates an Ed25519 keypair (persisted in content-packs/keys/) if absent,
computes pack SHA-256, signs pack bytes with Ed25519, and embeds
signature and public key in content_pack_manifest.json.
"""
import base64
import hashlib
import json
import os
from cryptography.hazmat.primitives.asymmetric import ed25519
from cryptography.hazmat.primitives import serialization

KEYS_DIR = "content-packs/keys"
PRIVATE_KEY_PATH = os.path.join(KEYS_DIR, "content_signer_private.pem")
PUBLIC_KEY_PATH = os.path.join(KEYS_DIR, "content_signer_public.pem")

def get_or_create_keypair():
    os.makedirs(KEYS_DIR, exist_ok=True)
    if os.path.exists(PRIVATE_KEY_PATH):
        with open(PRIVATE_KEY_PATH, "rb") as f:
            private_key = serialization.load_pem_private_key(f.read(), password=None)
        with open(PUBLIC_KEY_PATH, "rb") as f:
            public_key = serialization.load_pem_public_key(f.read())
        return private_key, public_key
    
    # Generate new keypair
    private_key = ed25519.Ed25519PrivateKey.generate()
    public_key = private_key.public_key()
    
    with open(PRIVATE_KEY_PATH, "wb") as f:
        f.write(private_key.private_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PrivateFormat.PKCS8,
            encryption_algorithm=serialization.NoEncryption()
        ))
    with open(PUBLIC_KEY_PATH, "wb") as f:
        f.write(public_key.public_bytes(
            encoding=serialization.Encoding.PEM,
            format=serialization.PublicFormat.SubjectPublicKeyInfo
        ))
    print(f"Generated new Ed25519 keypair in {KEYS_DIR}")
    return private_key, public_key

def sign_pack(pack_path="app/src/main/assets/content_pack_v1.json"):
    private_key, public_key = get_or_create_keypair()
    
    with open(pack_path, "rb") as f:
        pack_bytes = f.read()
        
    sha256 = hashlib.sha256(pack_bytes).hexdigest()
    
    # Sign pack bytes with Ed25519
    signature = private_key.sign(pack_bytes)
    
    # Extract raw 32-byte public key
    pub_raw = public_key.public_bytes(
        encoding=serialization.Encoding.Raw,
        format=serialization.PublicFormat.Raw
    )
    
    sig_b64 = base64.b64encode(signature).decode("ascii")
    pub_b64 = base64.b64encode(pub_raw).decode("ascii")
    
    print(f"Pack SHA-256: {sha256}")
    print(f"Ed25519 Signature (b64): {sig_b64[:20]}... ({len(signature)} bytes)")
    print(f"Ed25519 Public Key (b64): {pub_b64}")
    
    manifest_data = {
        "version": 1,
        "file": "content_pack_v1.json",
        "sha256": sha256,
        "chunks": 105,
        "language": "ar-EG",
        "signature_ed25519": sig_b64,
        "public_key_ed25519": pub_b64,
        "note": "Clinician-approved educational content, v1 (105 CBT/ACT chunks). Signed with Ed25519."
    }
    
    manifest_json = json.dumps(manifest_data, ensure_ascii=False, indent=1)
    
    for path in ["app/src/main/assets/content_pack_manifest.json", "content-packs/ar-EG-v1/content_pack_manifest.json"]:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(manifest_json)
        print(f"Updated {path}")

if __name__ == "__main__":
    sign_pack()
