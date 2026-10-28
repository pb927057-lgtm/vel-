/**
 * Motor Criptográfico Web Crypto para la simulación y validación en tiempo real de VEL.
 * Utiliza algoritmos y parámetros idénticos a los de la implementación nativa en Android (javax.crypto):
 * - AES-GCM de 256 bits
 * - Nonce de 96 bits (12 bytes)
 * - Tag de autenticación de 128 bits
 * - Datos Autenticados Adicionales (AAD)
 * - HKDF con HMAC-SHA-256 para derivación de claves
 */

export class WebCryptoEngine {
  /**
   * Genera un par de claves ECDH P-256
   */
  static async generateEcdhKeyPair(): Promise<CryptoKeyPair> {
    return await window.crypto.subtle.generateKey(
      {
        name: 'ECDH',
        namedCurve: 'P-256'
      },
      true,
      ['deriveKey', 'deriveBits']
    );
  }

  /**
   * Exporta la clave pública a formato Hex
   */
  static async exportPublicKeyHex(key: CryptoKey): Promise<string> {
    const raw = await window.crypto.subtle.exportKey('raw', key);
    return Array.from(new Uint8Array(raw))
      .map(b => b.toString(16).padStart(2, '0'))
      .join('');
  }

  /**
   * Importa una clave pública desde Hex
   */
  static async importPublicKeyHex(hex: string): Promise<CryptoKey> {
    const bytes = new Uint8Array(hex.match(/.{1,2}/g)!.map(byte => parseInt(byte, 16)));
    return await window.crypto.subtle.importKey(
      'raw',
      bytes,
      {
        name: 'ECDH',
        namedCurve: 'P-256'
      },
      true,
      []
    );
  }

  /**
   * Deriva claves de sesión simétricas AES-256-GCM mediante HKDF (RFC 5869)
   */
  static async deriveSessionKeys(
    localPrivateKey: CryptoKey,
    remotePublicKey: CryptoKey,
    saltHex: string,
    contextInfo: string
  ): Promise<{ terminalToCoreKey: CryptoKey; coreToTerminalKey: CryptoKey }> {
    // 1. Calcular secreto compartido ECDH
    const sharedBits = await window.crypto.subtle.deriveBits(
      {
        name: 'ECDH',
        public: remotePublicKey
      },
      localPrivateKey,
      256
    );

    // 2. Importar como clave base para HKDF
    const hkdfKey = await window.crypto.subtle.importKey(
      'raw',
      sharedBits,
      { name: 'HKDF' },
      false,
      ['deriveKey']
    );

    const encoder = new TextEncoder();
    const saltBytes = new Uint8Array(saltHex.match(/.{1,2}/g)!.map(b => parseInt(b, 16)));

    // Clave de Terminal -> CORE
    const terminalToCoreKey = await window.crypto.subtle.deriveKey(
      {
        name: 'HKDF',
        hash: 'SHA-256',
        salt: saltBytes,
        info: encoder.encode(`VEL-v1-terminal-to-core:${contextInfo}`)
      },
      hkdfKey,
      { name: 'AES-GCM', length: 256 },
      true,
      ['encrypt', 'decrypt']
    );

    // Clave de CORE -> Terminal
    const coreToTerminalKey = await window.crypto.subtle.deriveKey(
      {
        name: 'HKDF',
        hash: 'SHA-256',
        salt: saltBytes,
        info: encoder.encode(`VEL-v1-core-to-terminal:${contextInfo}`)
      },
      hkdfKey,
      { name: 'AES-GCM', length: 256 },
      true,
      ['encrypt', 'decrypt']
    );

    return { terminalToCoreKey, coreToTerminalKey };
  }

  /**
   * Cifra un mensaje mediante AES-256-GCM con nonce de 96 bits y AAD opcional
   */
  static async encrypt(
    key: CryptoKey,
    plaintext: string,
    aadString?: string
  ): Promise<{ nonceBase64: string; ciphertextBase64: string; aadBase64: string }> {
    const encoder = new TextEncoder();
    const plaintextBytes = encoder.encode(plaintext);

    // Nonce único de 96 bits (12 bytes)
    const nonce = window.crypto.getRandomValues(new Uint8Array(12));

    const algorithmParams: AesGcmParams = {
      name: 'AES-GCM',
      iv: nonce,
      tagLength: 128
    };

    if (aadString) {
      algorithmParams.additionalData = encoder.encode(aadString);
    }

    const encryptedBuffer = await window.crypto.subtle.encrypt(
      algorithmParams,
      key,
      plaintextBytes
    );

    const ciphertextBytes = new Uint8Array(encryptedBuffer);

    return {
      nonceBase64: btoa(String.fromCharCode(...nonce)),
      ciphertextBase64: btoa(String.fromCharCode(...ciphertextBytes)),
      aadBase64: aadString ? btoa(aadString) : ''
    };
  }

  /**
   * Descifra y autentica un mensaje AES-256-GCM
   */
  static async decrypt(
    key: CryptoKey,
    nonceBase64: string,
    ciphertextBase64: string,
    aadBase64?: string
  ): Promise<string> {
    const nonce = Uint8Array.from(atob(nonceBase64), c => c.charCodeAt(0));
    const ciphertext = Uint8Array.from(atob(ciphertextBase64), c => c.charCodeAt(0));

    const algorithmParams: AesGcmParams = {
      name: 'AES-GCM',
      iv: nonce,
      tagLength: 128
    };

    if (aadBase64) {
      algorithmParams.additionalData = Uint8Array.from(atob(aadBase64), c => c.charCodeAt(0));
    }

    try {
      const decryptedBuffer = await window.crypto.subtle.decrypt(
        algorithmParams,
        key,
        ciphertext
      );
      return new TextDecoder().decode(decryptedBuffer);
    } catch (err) {
      throw new Error('Fallo de integridad criptográfica: datos manipulados o clave inválida');
    }
  }

  /**
   * Helper para simular manipulación de bits (tampering) en un ciphertext
   */
  static tamperCiphertext(ciphertextBase64: string): string {
    const bytes = Uint8Array.from(atob(ciphertextBase64), c => c.charCodeAt(0));
    if (bytes.length > 0) {
      bytes[0] ^= 0xff; // Invertir bits del primer byte
    }
    return btoa(String.fromCharCode(...bytes));
  }
}
