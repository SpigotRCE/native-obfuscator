from cryptography.hazmat.primitives.hashes import SHA256
from cryptography.hazmat.primitives.kdf.hkdf import HKDFExpand
from cryptography.hazmat.backends import default_backend
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad
import base64

def hkdf_expand(key: bytes, info: str, length: int) -> bytes:
    return HKDFExpand(
        algorithm=SHA256(),
        length=length,
        info=info.encode(),
        backend=default_backend()
    ).derive(key)


MASTER_KEY = base64.b64decode(input("Enter the master key: "))

aes_key = hkdf_expand(MASTER_KEY, "aes-key", 16)
aes_iv  = hkdf_expand(MASTER_KEY, "aes-iv",  16)

cipher = AES.new(aes_key, AES.MODE_CBC, aes_iv)

with open("natives.zip", "rb") as f:
    plaintext = f.read()

with open("natives.zip.enc", "wb") as f:
    f.write(cipher.encrypt(pad(plaintext, AES.block_size)))