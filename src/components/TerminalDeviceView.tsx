import React, { useState } from 'react';
import {
  Smartphone,
  Shield,
  Lock,
  LockKeyhole,
  CheckCircle,
  XCircle,
  Clock,
  AlertTriangle,
  QrCode,
  Info,
  Radio,
  RefreshCw,
  Send
} from 'lucide-react';
import { VelLogo } from './VelLogo';
import { ExecutionStatus } from '../types/vel';

interface TerminalDeviceViewProps {
  deviceId: string;
  isPaired: boolean;
  pairedCoreId: string | null;
  isRevoked: boolean;
  revocationReason: string | null;
  channelConnected: boolean;
  currentOpStatus: ExecutionStatus | null;
  lastConfirmedDetails: string | null;
  onInitiatePairing: (code: string) => Promise<boolean>;
  onSendProtectedOperation: (opType: string) => Promise<void>;
  pairingCodeInput: string;
  setPairingCodeInput: (val: string) => void;
}

export const TerminalDeviceView: React.FC<TerminalDeviceViewProps> = ({
  deviceId,
  isPaired,
  pairedCoreId,
  isRevoked,
  revocationReason,
  channelConnected,
  currentOpStatus,
  lastConfirmedDetails,
  onInitiatePairing,
  onSendProtectedOperation,
  pairingCodeInput,
  setPairingCodeInput
}) => {
  const [view, setView] = useState<'DASHBOARD' | 'PAIRING_INPUT' | 'DIAGNOSTICS'>('DASHBOARD');
  const [pairingError, setPairingError] = useState('');
  const [isExecuting, setIsExecuting] = useState(false);

  // Pantalla de Vinculación en Terminal (Escanear / Ingresar código de CORE)
  if (view === 'PAIRING_INPUT') {
    return (
      <div className="h-full flex flex-col justify-between p-4 bg-[#0B1220] overflow-y-auto">
        <div>
          <h3 className="text-sm font-bold text-[#3B82F6] mb-1">Enlazar con VEL CORE</h3>
          <p className="text-xs text-[#94A3B8] mb-4">
            Ingresa el código efímero generado por el dispositivo CORE para intercambiar identidades ECDH.
          </p>

          <div className="space-y-2 mb-4">
            <label className="text-xs font-medium text-[#F1F5F9]">Código de Vinculación</label>
            <input
              type="text"
              placeholder="VEL-XXXX-XXXX"
              value={pairingCodeInput}
              onChange={e => {
                setPairingCodeInput(e.target.value.toUpperCase());
                setPairingError('');
              }}
              className="w-full px-3 py-2.5 bg-[#111B2E] border border-[#263449] rounded-lg text-sm font-mono text-[#22D3EE] tracking-wider focus:outline-none focus:border-[#3B82F6]"
            />
            {pairingError && (
              <span className="text-[11px] text-[#F87171]">{pairingError}</span>
            )}
          </div>

          <div className="p-3 bg-[#17243A] rounded-xl border border-[#263449] text-xs text-[#94A3B8] space-y-1.5">
            <div className="font-semibold text-[#F1F5F9]">Propiedades de Seguridad:</div>
            <p>• La vinculación no expone claves privadas.</p>
            <p>• Requiere aprobación física explícita en CORE.</p>
            <p>• Deriva claves de sesión de 256 bits mediante HKDF.</p>
          </div>
        </div>

        <div className="mt-4 space-y-2">
          <button
            onClick={async () => {
              if (!pairingCodeInput.trim()) {
                setPairingError('Por favor introduce el código de un solo uso');
                return;
              }
              const ok = await onInitiatePairing(pairingCodeInput);
              if (ok) {
                setView('DASHBOARD');
              } else {
                setPairingError('Código de vinculación inválido o expirado.');
              }
            }}
            className="w-full py-2.5 bg-[#3B82F6] hover:bg-[#2563EB] text-[#F1F5F9] text-xs font-semibold rounded-lg"
          >
            Vincular y Derivar Claves
          </button>
          <button
            onClick={() => setView('DASHBOARD')}
            className="w-full py-2 bg-[#111B2E] text-[#94A3B8] text-xs rounded-lg border border-[#263449]"
          >
            Cancelar
          </button>
        </div>
      </div>
    );
  }

  // Pantalla de Diagnóstico Técnico
  if (view === 'DIAGNOSTICS') {
    return (
      <div className="h-full flex flex-col justify-between p-4 bg-[#0B1220] overflow-y-auto">
        <div>
          <h3 className="text-sm font-bold text-[#3B82F6] mb-3">Diagnóstico Técnico Terminal</h3>

          <div className="space-y-2 text-xs">
            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Identificador de Dispositivo</span>
              <span className="font-mono text-[#F1F5F9]">{deviceId}</span>
            </div>

            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Cifrado de Extremo a Extremo</span>
              <span className="text-[#34D399] font-semibold">AES-256-GCM / Tag 128 bits</span>
            </div>

            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Canal Relevo Ktor WebSocket</span>
              <span className={channelConnected ? 'text-[#34D399] font-semibold' : 'text-[#FBBF24] font-semibold'}>
                {channelConnected ? 'Conectado (Cero conocimiento del servidor)' : 'Desconectado'}
              </span>
            </div>

            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Autoridad Asignada</span>
              <span className="text-[#22D3EE] font-semibold">{pairedCoreId || 'Ninguna (Sin vincular)'}</span>
            </div>
          </div>
        </div>

        <button
          onClick={() => setView('DASHBOARD')}
          className="w-full mt-4 py-2 bg-[#111B2E] text-[#F1F5F9] text-xs rounded-lg border border-[#263449]"
        >
          Volver
        </button>
      </div>
    );
  }

  // Dashboard Principal de Terminal
  return (
    <div className="h-full flex flex-col justify-between p-4 bg-[#0B1220] overflow-y-auto">
      <div className="space-y-3.5">
        {/* Cabecera */}
        <div className="flex items-center justify-between pb-2 border-b border-[#263449]">
          <div className="flex items-center gap-2">
            <VelLogo size={28} showWordmark={false} />
            <div>
              <h2 className="text-sm font-bold text-[#3B82F6] leading-tight">VEL Terminal</h2>
              <span className="text-[10px] text-[#94A3B8]">Cliente Móvil del Propietario</span>
            </div>
          </div>
          <button
            onClick={() => setView('DIAGNOSTICS')}
            className="p-1.5 rounded-lg bg-[#111B2E] text-[#3B82F6] hover:bg-[#17243A] border border-[#263449]"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>

        {/* ALERTA DE REVOCACIÓN CRÍTICA (Bloqueo Total) */}
        {isRevoked && (
          <div className="p-3.5 bg-[#17243A] rounded-xl border-2 border-[#F87171] shadow-lg animate-pulse">
            <div className="flex items-center gap-2 text-[#F87171] font-bold text-xs">
              <AlertTriangle className="w-4 h-4" />
              <span>TERMINAL REVOCADO</span>
            </div>
            <p className="text-[11px] text-[#F1F5F9] mt-1.5 leading-relaxed">
              Este dispositivo ha sido revocado por la autoridad CORE. Todas las operaciones protegidas están bloqueadas y sus claves han sido destruidas.
            </p>
            {revocationReason && (
              <div className="mt-2 text-[10px] text-[#F87171] bg-[#0B1220] p-1.5 rounded border border-[#F87171]/30">
                Motivo: {revocationReason}
              </div>
            )}
          </div>
        )}

        {/* Estado Criptográfico y Canal */}
        <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449] space-y-1.5 text-xs">
          <div className="flex justify-between items-center">
            <span className="text-[#94A3B8]">Autoridad CORE:</span>
            <span
              className={`font-semibold ${
                isPaired ? 'text-[#22D3EE]' : 'text-[#FBBF24]'
              }`}
            >
              {isPaired ? `Vinculado (${pairedCoreId?.slice(0, 10)}...)` : 'No Vinculado'}
            </span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-[#94A3B8]">Canal Seguro (WSS):</span>
            <span
              className={`font-semibold flex items-center gap-1 ${
                channelConnected ? 'text-[#34D399]' : 'text-[#FBBF24]'
              }`}
            >
              <Radio className="w-3 h-3" /> {channelConnected ? 'Establecido • E2EE' : 'Desconectado'}
            </span>
          </div>

          <div className="flex justify-between items-center">
            <span className="text-[#94A3B8]">Estado de Autorización:</span>
            <span
              className={`font-semibold ${
                isRevoked
                  ? 'text-[#F87171]'
                  : isPaired
                  ? 'text-[#34D399]'
                  : 'text-[#94A3B8]'
              }`}
            >
              {isRevoked ? 'REVOCADO' : isPaired ? 'AUTORIZADO' : 'Sin Credencial'}
            </span>
          </div>
        </div>

        {/* Si no está vinculado ni revocado */}
        {!isPaired && !isRevoked && (
          <button
            onClick={() => setView('PAIRING_INPUT')}
            className="w-full py-2.5 bg-[#3B82F6] hover:bg-[#2563EB] text-[#F1F5F9] text-xs font-semibold rounded-lg flex items-center justify-center gap-2"
          >
            <QrCode className="w-4 h-4" />
            <span>Vincular con VEL CORE</span>
          </button>
        )}

        {/* Operaciones Protegidas (Solo disponibles si está vinculado y NO revocado) */}
        {isPaired && !isRevoked && (
          <div>
            <div className="text-xs font-semibold text-[#F1F5F9] mb-2">
              Operaciones Protegidas (Cifradas AES-GCM)
            </div>
            <div className="space-y-2">
              <button
                disabled={isExecuting}
                onClick={async () => {
                  setIsExecuting(true);
                  await onSendProtectedOperation('LOCK_DEVICE');
                  setIsExecuting(false);
                }}
                className="w-full py-2 px-3 bg-[#17243A] hover:bg-[#1f314f] text-[#F1F5F9] text-xs font-medium rounded-lg border border-[#263449] flex items-center justify-between transition-all disabled:opacity-50"
              >
                <div className="flex items-center gap-2">
                  <Lock className="w-3.5 h-3.5 text-[#22D3EE]" />
                  <span>Bloqueo Remoto de Emergencia</span>
                </div>
                <Send className="w-3 h-3 text-[#94A3B8]" />
              </button>

              <button
                disabled={isExecuting}
                onClick={async () => {
                  setIsExecuting(true);
                  await onSendProtectedOperation('SHIELD_DATA');
                  setIsExecuting(false);
                }}
                className="w-full py-2 px-3 bg-[#17243A] hover:bg-[#1f314f] text-[#F1F5F9] text-xs font-medium rounded-lg border border-[#263449] flex items-center justify-between transition-all disabled:opacity-50"
              >
                <div className="flex items-center gap-2">
                  <Shield className="w-3.5 h-3.5 text-[#3B82F6]" />
                  <span>Activar Escudo Criptográfico</span>
                </div>
                <Send className="w-3 h-3 text-[#94A3B8]" />
              </button>

              <button
                disabled={isExecuting}
                onClick={async () => {
                  setIsExecuting(true);
                  await onSendProtectedOperation('VERIFY_INTEGRITY');
                  setIsExecuting(false);
                }}
                className="w-full py-2 px-3 bg-[#17243A] hover:bg-[#1f314f] text-[#F1F5F9] text-xs font-medium rounded-lg border border-[#263449] flex items-center justify-between transition-all disabled:opacity-50"
              >
                <div className="flex items-center gap-2">
                  <CheckCircle className="w-3.5 h-3.5 text-[#34D399]" />
                  <span>Verificar Integridad de CORE</span>
                </div>
                <Send className="w-3 h-3 text-[#94A3B8]" />
              </button>
            </div>
          </div>
        )}

        {/* Rastreador de Estados de la Solicitud */}
        {currentOpStatus && (
          <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
            <div className="text-[10px] text-[#94A3B8] font-medium uppercase tracking-wider">
              Estado de la Solicitud
            </div>
            <div className="flex items-center gap-2 mt-1">
              {currentOpStatus === 'EXECUTED_AND_CONFIRMED' ? (
                <CheckCircle className="w-4 h-4 text-[#34D399]" />
              ) : currentOpStatus === 'REJECTED' || currentOpStatus === 'COMMUNICATION_ERROR' ? (
                <XCircle className="w-4 h-4 text-[#F87171]" />
              ) : (
                <RefreshCw className="w-4 h-4 text-[#FBBF24] animate-spin" />
              )}

              <span
                className={`text-xs font-bold ${
                  currentOpStatus === 'EXECUTED_AND_CONFIRMED'
                    ? 'text-[#34D399]'
                    : currentOpStatus === 'REJECTED' || currentOpStatus === 'COMMUNICATION_ERROR'
                    ? 'text-[#F87171]'
                    : 'text-[#FBBF24]'
                }`}
              >
                {currentOpStatus === 'PREPARING' && 'Preparando solicitud'}
                {currentOpStatus === 'SENT' && 'Enviada'}
                {currentOpStatus === 'RECEIVED_BY_SERVER' && 'Recibida por el servidor'}
                {currentOpStatus === 'PENDING_CORE_CONFIRMATION' && 'Pendiente de confirmación de CORE'}
                {currentOpStatus === 'EXECUTED_AND_CONFIRMED' && 'Ejecutada y confirmada'}
                {currentOpStatus === 'REJECTED' && 'Rechazada'}
                {currentOpStatus === 'NOT_EXECUTED' && 'No ejecutada'}
                {currentOpStatus === 'UNCERTAIN' && 'Resultado incierto'}
                {currentOpStatus === 'COMMUNICATION_ERROR' && 'Error de comunicación'}
              </span>
            </div>

            {lastConfirmedDetails && (
              <p className="text-[10px] text-[#94A3B8] mt-1.5 border-t border-[#263449] pt-1 font-mono">
                {lastConfirmedDetails}
              </p>
            )}
          </div>
        )}
      </div>

      <div className="pt-2 text-[10px] text-center text-[#94A3B8]">
        El servidor transmite únicamente bytes cifrados; no dispone de las claves.
      </div>
    </div>
  );
};
