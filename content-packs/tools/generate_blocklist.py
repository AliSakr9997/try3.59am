# -*- coding: utf-8 -*-
"""
Generates the curated DNS blocklist (v1), computes SHA-256, and writes
both app/src/main/assets/blocklist_v1.txt and app/src/main/assets/blocklist_manifest.json.
"""
import hashlib
import json
import os

CURATED_DOMAINS = [
    # Top Tube Networks & Aggregators
    "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
    "youporn.com", "tube8.com", "porn.com", "spankbang.com", "eporner.com",
    "tnaflix.com", "beeg.com", "hclips.com", "sex.com", "porntrex.com",
    "nudevista.com", "sunporno.com", "xvideos2.com", "pornhubpremium.com",
    "youporngay.com", "gayporn.com", "pornmd.com", "tubev.com", "fapdu.com",
    "pervclips.com", "xxx.com", "sexvideos.com", "daftsex.com", "thumbzilla.com",
    "xcafe.com", "xtube.com", "vporn.com", "txxx.com", "upornia.com",
    "hotmovs.com", "hqporner.com", "czechav.com", "heavy-r.com", "motherless.com",
    
    # Commercial Studios & Networks
    "brazzers.com", "realitykings.com", "bangbros.com", "naughtyamerica.com",
    "twistys.com", "digitalplayground.com", "evilangel.com", "fakehub.com",
    "mofos.com", "rk.com", "teamskeet.com", "kink.com", "wicked.com",
    
    # Live Cam Services
    "chaturbate.com", "livejasmin.com", "stripchat.com", "bongacams.com",
    "cam4.com", "camsoda.com", "myfreecams.com", "streamate.com", "jerkmate.com",
    "flirt4free.com", "imlive.com", "camversity.com", "xcams.com",
    
    # Adult Creator & Subscription Platforms
    "onlyfans.com", "fansly.com", "manyvids.com", "modelhub.com",
    "loyalfans.com", "justfor.fans", "fancentro.com", "admireme.vip",
    
    # Adult Dating & Hookup
    "adultfriendfinder.com", "ashley-madison.com", "fling.com", "fetlife.com",
    "alt.com", "passion.com", "benaughty.com", "casualx.com", "pure.app",
    
    # Hentai, Adult Anime & Erotica
    "nhentai.net", "hanime.tv", "hentaihaven.xxx", "tsumino.com", "hitomi.la",
    "gelbooru.com", "rule34.xxx", "luscious.net", "literotica.com", "asstr.org",
    
    # Known Free Web Proxies & Adult Bypasses
    "hide.me", "kproxy.com", "proxysite.com", "croxyproxy.com", "blockaway.net",
    "zalmos.com", "4everproxy.com", "whoer.net", "megaproxy.com"
]

def main():
    # Normalize, deduplicate, sort
    cleaned = sorted(list(set(d.strip().lower() for d in CURATED_DOMAINS if d.strip())))
    
    header = (
        "# Recovery Companion - DNS blocklist v1 (curated starter set).\n"
        "# One domain per line. Suffix-matched: subdomains are blocked automatically.\n"
        "# Layered with Cloudflare Family 1.1.1.3 upstream resolver.\n"
    )
    body = "\n".join(cleaned) + "\n"
    full_content = header + body
    
    asset_file = "app/src/main/assets/blocklist_v1.txt"
    with open(asset_file, "wb") as f:
        f.write(full_content.encode("utf-8"))
        
    sha256 = hashlib.sha256(full_content.encode("utf-8")).hexdigest()
    print(f"Generated {len(cleaned)} blocked domains.")
    print(f"SHA-256: {sha256}")
    
    manifest = {
        "version": 1,
        "file": "blocklist_v1.txt",
        "sha256": sha256,
        "domains": len(cleaned)
    }
    manifest_str = json.dumps(manifest, indent=1)
    with open("app/src/main/assets/blocklist_manifest.json", "w", encoding="utf-8") as f:
        f.write(manifest_str)
    print("Updated app/src/main/assets/blocklist_manifest.json")

if __name__ == "__main__":
    main()
