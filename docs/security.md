# EventFlow Security Architecture & HMAC Webhook Verification

## 1. Authentication Mechanisms
- **JWT (JSON Web Token)**: Used for dashboard and user REST API sessions. Signed with HMAC SHA-256 (`eventflow.security.jwt.secret`).
- **API Keys**: Prefix `ef_live_` + 32 random hex bytes. Stored in DB as SHA-256 hash (`HashUtils.sha256(rawKey)`). Raw API key is shown ONCE upon creation.

## 2. Webhook HMAC SHA-256 Signature Verification
Every outgoing webhook POST request contains header `X-EventFlow-Signature`:
```
X-EventFlow-Signature: t=1726484400,v1=9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08
```

### Verification Algorithm for Downstream Consumers
```python
import hmac
import hashlib

def verify_signature(raw_body, signature_header, secret):
    parts = dict(pair.split('=') for pair in signature_header.split(','))
    timestamp = parts['t']
    expected_sig = parts['v1']
    
    signed_payload = f"{timestamp}.{raw_body}".encode('utf-8')
    computed_sig = hmac.new(secret.encode('utf-8'), signed_payload, hashlib.sha256).hexdigest()
    
    return hmac.compare_digest(expected_sig, computed_sig)
```
