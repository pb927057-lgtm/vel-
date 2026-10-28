export type DeviceRole = 'UNCONFIGURED' | 'CORE' | 'TERMINAL';

export type AuthorizationStatus = 'PENDING' | 'AUTHORIZED' | 'REVOKED';

export type ExecutionStatus =
  | 'PREPARING'
  | 'SENT'
  | 'RECEIVED_BY_SERVER'
  | 'PENDING_CORE_CONFIRMATION'
  | 'EXECUTED_AND_CONFIRMED'
  | 'REJECTED'
  | 'NOT_EXECUTED'
  | 'UNCERTAIN'
  | 'COMMUNICATION_ERROR';

export interface DeviceInfo {
  deviceId: string;
  deviceName: string;
  role: DeviceRole;
  status: AuthorizationStatus;
  pairedAt: number;
  lastSeen: number;
  authToken: string;
  publicKeyHex: string;
}

export interface EncryptedPacket {
  protocolVersion: number;
  requestId: string;
  sourceDeviceId: string;
  targetDeviceId: string;
  nonceBase64: string;
  ciphertextBase64: string;
  aadBase64: string;
  timestamp: number;
  // Simulation metadata (client side only)
  decryptedPreview?: string;
  isTampered?: boolean;
}

export interface OperationLog {
  id: string;
  requestId: string;
  operationType: string;
  sourceDeviceId: string;
  targetDeviceId: string;
  status: ExecutionStatus;
  timestamp: number;
  details: string;
  isConfirmed: boolean;
}

export interface TestCaseResult {
  id: string;
  title: string;
  category: 'CRYPTO' | 'AUTH' | 'REPLAY' | 'REVOCATION' | 'RELAY';
  passed: boolean;
  durationMs: number;
  details: string;
}
