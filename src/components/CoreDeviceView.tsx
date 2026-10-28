import React, { useState } from 'react';
import {
  Shield,
  Key,
  QrCode,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Info,
  Server,
  Lock,
  ArrowLeft,
  Smartphone,
  Radio
} from 'lucide-react';
import { VelLogo } from './VelLogo';
import { DeviceInfo, DeviceRole, EncryptedPacket } from '../types/vel';

interface CoreDeviceViewProps {
  deviceId: string;
  role: DeviceRole;
  authorizedDevices: DeviceInfo[];
  serverConnected: boolean;
  onSetRole: (role: DeviceRole) => void;
  onApprovePairing: (terminalId: string, deviceName: string) => void;
  onRevokeDevice: (terminalId: string, passwordConfirm: string) => Promise<boolean>;
  activePairingCode: string;
  onRefreshPairingCode: () => void;
}

export const CoreDeviceView: React.FC<CoreDeviceViewProps> = ({
  deviceId,
  role,
  authorizedDevices,
  serverConnected,
  onSetRole,
  onApprovePairing,
  onRevokeDevice,
  activePairingCode,
  onRefreshPairingCode
}) => {
  const [view, setView] = useState<'WELCOME' | 'ROLE_SELECT' | 'DASHBOARD' | 'PAIRING' | 'REVOKE' | 'DIAGNOSTICS'>(
    role === 'UNCONFIGURED' ? 'ROLE_SELECT' : 'DASHBOARD'
  );
  const [selectedTerminalForRevocation, setSelectedTerminalForRevocation] = useState<string>('');
  const [revocationPassword, setRevocationPassword] = useState('');
  const [revocationState, setRevocationState] = useState<'IDLE' | 'PENDING' | 'CONFIRMED' | 'ERROR'>('IDLE');
  const [revocationMessage, setRevocationMessage] = useState('');

  // 1. Pantalla de Bienvenida VEL
  if (view === 'WELCOME') {
    return (
      <div className="h-full flex flex-col items-center justify-center p-6 bg-[#0B1220] text-center">
        <VelLogo size={96} showWordmark={true} />
        <p className="mt-4 text-sm text-[#94A3B8] tracking-wide">
          Protección de la información
        </p>
        <button
          onClick={() => setView('DASHBOARD')}
          className="mt-8 px-6 py-2.5 bg-[#3B82F6] hover:bg-[#2563EB] text-[#F1F5F9] text-xs font-semibold rounded-lg transition-all"
        >
          Acceder a la Autoridad
        </button>
      </div>
    );
  }

  // 2. Selección de Rol inicial
  if (view === 'ROLE_SELECT') {
    return (
      <div className="h-full flex flex-col justify-between p-5 bg-[#0B1220]">
        <div className="flex flex-col items-center text-center mt-3">
          <VelLogo size={56} showWordmark={false} />
          <h2 className="text-lg font-bold text-[#F1F5F9] mt-2">Configuración del Dispositivo</h2>
          <p className="text-xs text-[#94A3B8] mt-1">
            Asigna el rol criptográfico para esta unidad.
          </p>
        </div>

        <div className="flex flex-col gap-3.5 my-auto">
          {/* Tarjeta CORE */}
          <div
            onClick={() => {
              onSetRole('CORE');
              setView('DASHBOARD');
            }}
            className="p-4 rounded-xl bg-[#111B2E] border border-[#22D3EE]/50 hover:border-[#22D3EE] cursor-pointer transition-all"
          >
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-[#22D3EE]">VEL CORE</span>
              <span className="text-[10px] bg-[#17243A] text-[#22D3EE] px-2 py-0.5 rounded border border-[#22D3EE]/30">
                Autoridad
              </span>
            </div>
            <p className="text-xs text-[#94A3B8] mt-1.5 leading-relaxed">
              Dispositivo custodio y autoridad de seguridad.
            </p>
            <button className="w-full mt-3 py-2 bg-[#3B82F6] text-white text-xs font-medium rounded-lg">
              Configurar como CORE
            </button>
          </div>

          {/* Tarjeta Terminal */}
          <div
            onClick={() => {
              onSetRole('TERMINAL');
            }}
            className="p-4 rounded-xl bg-[#111B2E] border border-[#263449] hover:border-[#3B82F6]/60 cursor-pointer transition-all"
          >
            <div className="flex items-center justify-between">
              <span className="text-sm font-bold text-[#3B82F6]">VEL Terminal</span>
              <span className="text-[10px] bg-[#17243A] text-[#3B82F6] px-2 py-0.5 rounded border border-[#3B82F6]/30">
                Cliente Móvil
              </span>
            </div>
            <p className="text-xs text-[#94A3B8] mt-1.5 leading-relaxed">
              Dispositivo móvil para conexión segura con CORE.
            </p>
            <button className="w-full mt-3 py-2 bg-[#17243A] text-[#94A3B8] text-xs font-medium rounded-lg border border-[#263449]">
              Configurar como Terminal
            </button>
          </div>
        </div>

        <p className="text-[10px] text-center text-[#94A3B8] mb-2">
          El rol queda asociado a la identidad criptográfica de hardware.
        </p>
      </div>
    );
  }

  // 3. Flujo de Vinculación Criptográfica (Pairing)
  if (view === 'PAIRING') {
    return (
      <div className="h-full flex flex-col justify-between p-4 bg-[#0B1220] overflow-y-auto">
        <div>
          <div className="flex items-center gap-2 mb-3">
            <button
              onClick={() => setView('DASHBOARD')}
              className="p-1 rounded-lg bg-[#111B2E] text-[#94A3B8] hover:text-[#F1F5F9]"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <h3 className="text-sm font-bold text-[#22D3EE]">Vincular Nuevo Terminal</h3>
          </div>

          <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449] text-xs text-[#94A3B8] mb-3">
            <p>
              El proceso requiere autenticación mutua, intercambio de claves ECDH P-256 y autorización explícita de CORE.
            </p>
          </div>

          {/* Código de Vinculación / QR Mockup */}
          <div className="bg-[#17243A] rounded-xl p-4 border border-[#263449] flex flex-col items-center text-center">
            <div className="p-3 bg-white rounded-lg shadow mb-3">
              <QrCode className="w-28 h-28 text-[#0B1220]" />
            </div>
            <span className="text-[11px] text-[#94A3B8]">Código de Un Solo Uso</span>
            <span className="font-mono text-base font-bold text-[#22D3EE] tracking-wider mt-1">
              {activePairingCode}
            </span>
            <span className="text-[10px] text-[#FBBF24] mt-1">
              Válido por 180s • No contiene claves privadas
            </span>
            <button
              onClick={onRefreshPairingCode}
              className="mt-3 text-[11px] text-[#3B82F6] hover:underline"
            >
              Regenerar código efímero
            </button>
          </div>

          {/* Pasos de validación mutua */}
          <div className="mt-3 space-y-2 text-xs">
            <div className="flex items-center gap-2 text-[#34D399]">
              <CheckCircle className="w-3.5 h-3.5" />
              <span>1. Identidad efímera ECDH generada</span>
            </div>
            <div className="flex items-center gap-2 text-[#34D399]">
              <CheckCircle className="w-3.5 h-3.5" />
              <span>2. Huella criptográfica lista para verificar</span>
            </div>
            <div className="flex items-center gap-2 text-[#FBBF24]">
              <Radio className="w-3.5 h-3.5 animate-pulse" />
              <span>3. Esperando enlace desde Terminal...</span>
            </div>
          </div>
        </div>

        <div className="mt-4 space-y-2">
          <button
            onClick={() => {
              onApprovePairing(`term-${Date.now().toString().slice(-6)}`, 'Terminal Galaxy S24 Pro');
              setView('DASHBOARD');
            }}
            className="w-full py-2.5 bg-[#3B82F6] hover:bg-[#2563EB] text-[#F1F5F9] text-xs font-semibold rounded-lg"
          >
            Aprobar Vinculación y Generar Token
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

  // 4. Pantalla de Revocación
  if (view === 'REVOKE') {
    const targetDevice = authorizedDevices.find(d => d.deviceId === selectedTerminalForRevocation);
    return (
      <div className="h-full flex flex-col justify-between p-4 bg-[#0B1220] overflow-y-auto">
        <div>
          <div className="flex items-center gap-2 mb-3">
            <button
              onClick={() => {
                setView('DASHBOARD');
                setRevocationState('IDLE');
              }}
              className="p-1 rounded-lg bg-[#111B2E] text-[#94A3B8] hover:text-[#F1F5F9]"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <h3 className="text-sm font-bold text-[#F87171]">Revocación de Dispositivo</h3>
          </div>

          {/* Advertencia Crítica */}
          <div className="p-3.5 bg-[#17243A] rounded-xl border border-[#F87171]/40 mb-3.5">
            <div className="flex items-center gap-2 text-[#F87171] font-bold text-xs">
              <AlertTriangle className="w-4 h-4" />
              <span>Consecuencias de la Revocación</span>
            </div>
            <ul className="text-[11px] text-[#F1F5F9] mt-2 space-y-1.5 list-disc list-inside">
              <li>El Terminal perderá inmediatamente autorización criptográfica.</li>
              <li>Su token individual será destruido en CORE.</li>
              <li>El relevo rechazará cualquier intento de conexión.</li>
              <li>No podrá reactivarse con credenciales anteriores.</li>
            </ul>
          </div>

          <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449] mb-3 text-xs">
            <div className="text-[#94A3B8]">Dispositivo Seleccionado:</div>
            <div className="font-semibold text-[#F1F5F9] mt-0.5">{targetDevice?.deviceName || 'Terminal'}</div>
            <div className="font-mono text-[10px] text-[#94A3B8]">{targetDevice?.deviceId}</div>
          </div>

          <div className="space-y-1.5 mb-3">
            <label className="text-xs font-medium text-[#F1F5F9]">
              Contraseña Maestra de CORE
            </label>
            <input
              type="password"
              placeholder="Ingresa la contraseña de CORE"
              value={revocationPassword}
              onChange={e => setRevocationPassword(e.target.value)}
              className="w-full px-3 py-2 bg-[#111B2E] border border-[#263449] rounded-lg text-xs text-[#F1F5F9] focus:outline-none focus:border-[#22D3EE]"
            />
          </div>

          {revocationState !== 'IDLE' && (
            <div className="p-2.5 rounded-lg bg-[#17243A] border border-[#263449] text-xs">
              <div className="text-[10px] text-[#94A3B8]">Estado Real:</div>
              <div
                className={`font-semibold mt-0.5 ${
                  revocationState === 'CONFIRMED'
                    ? 'text-[#34D399]'
                    : revocationState === 'PENDING'
                    ? 'text-[#FBBF24]'
                    : 'text-[#F87171]'
                }`}
              >
                {revocationMessage}
              </div>
            </div>
          )}
        </div>

        <div className="mt-4 space-y-2">
          <button
            onClick={async () => {
              if (!revocationPassword) return;
              setRevocationState('PENDING');
              setRevocationMessage('Pendiente: aplicando revocación local y propagando al relevo...');
              const ok = await onRevokeDevice(selectedTerminalForRevocation, revocationPassword);
              if (ok) {
                setRevocationState('CONFIRMED');
                setRevocationMessage('Confirmado: dispositivo revocado localmente y rechazado por el servidor.');
                setTimeout(() => setView('DASHBOARD'), 1500);
              } else {
                setRevocationState('ERROR');
                setRevocationMessage('Rechazado: contraseña maestra incorrecta.');
              }
            }}
            disabled={!revocationPassword || revocationState === 'PENDING'}
            className="w-full py-2.5 bg-[#F87171] hover:bg-[#EF4444] text-[#F1F5F9] text-xs font-bold rounded-lg disabled:opacity-50"
          >
            Cambiar contraseña y revocar dispositivos autorizados
          </button>
          <button
            onClick={() => {
              setView('DASHBOARD');
              setRevocationState('IDLE');
            }}
            className="w-full py-2 bg-[#111B2E] text-[#94A3B8] text-xs rounded-lg border border-[#263449]"
          >
            Cancelar
          </button>
        </div>
      </div>
    );
  }

  // 5. Diagnóstico Técnico
  if (view === 'DIAGNOSTICS') {
    return (
      <div className="h-full flex flex-col justify-between p-4 bg-[#0B1220] overflow-y-auto">
        <div>
          <div className="flex items-center gap-2 mb-3">
            <button
              onClick={() => setView('DASHBOARD')}
              className="p-1 rounded-lg bg-[#111B2E] text-[#94A3B8] hover:text-[#F1F5F9]"
            >
              <ArrowLeft className="w-4 h-4" />
            </button>
            <h3 className="text-sm font-bold text-[#22D3EE]">Diagnóstico Técnico CORE</h3>
          </div>

          <div className="space-y-2.5 text-xs">
            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Autoridad Criptográfica</span>
              <span className="text-[#34D399] font-semibold">Activa • Android Keystore P-256</span>
            </div>

            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Canal de Relevo (WSS)</span>
              <span className={serverConnected ? 'text-[#34D399] font-semibold' : 'text-[#FBBF24] font-semibold'}>
                {serverConnected ? 'Conectado (wss://relay.vel.secure)' : 'Desconectado'}
              </span>
            </div>

            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Versión del Protocolo</span>
              <span className="text-[#F1F5F9] font-semibold">VEL-v1 (AES-256-GCM / 96-bit Nonce)</span>
            </div>

            <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449]">
              <span className="text-[#94A3B8] block text-[10px]">Control de Privacidad</span>
              <span className="text-[#34D399] font-semibold">Sin almacenamiento en la nube • Cero copias</span>
            </div>
          </div>
        </div>

        <button
          onClick={() => setView('DASHBOARD')}
          className="w-full mt-4 py-2 bg-[#111B2E] text-[#F1F5F9] text-xs rounded-lg border border-[#263449]"
        >
          Volver al Inicio
        </button>
      </div>
    );
  }

  // 6. Pantalla Principal de VEL CORE (Dashboard)
  return (
    <div className="h-full flex flex-col justify-between p-4 bg-[#0B1220] overflow-y-auto">
      <div className="space-y-3.5">
        {/* Cabecera de la autoridad */}
        <div className="flex items-center justify-between pb-2 border-b border-[#263449]">
          <div className="flex items-center gap-2">
            <VelLogo size={28} showWordmark={false} />
            <div>
              <h2 className="text-sm font-bold text-[#22D3EE] leading-tight">VEL CORE</h2>
              <span className="text-[10px] text-[#94A3B8]">Autoridad de Seguridad</span>
            </div>
          </div>
          <button
            onClick={() => setView('DIAGNOSTICS')}
            className="p-1.5 rounded-lg bg-[#111B2E] text-[#22D3EE] hover:bg-[#17243A] border border-[#263449]"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>

        {/* Estado Local de Seguridad */}
        <div className="p-3 bg-[#111B2E] rounded-xl border border-[#263449] space-y-1.5 text-xs">
          <div className="flex justify-between items-center">
            <span className="text-[#94A3B8]">Autoridad Local:</span>
            <span className="text-[#34D399] font-semibold flex items-center gap-1">
              <CheckCircle className="w-3 h-3" /> Activa
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-[#94A3B8]">Servidor de Relevo:</span>
            <span
              className={`font-semibold flex items-center gap-1 ${
                serverConnected ? 'text-[#34D399]' : 'text-[#FBBF24]'
              }`}
            >
              <Radio className="w-3 h-3" /> {serverConnected ? 'Conectado WSS' : 'Desconectado'}
            </span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-[#94A3B8]">Terminales Activos:</span>
            <span className="text-[#22D3EE] font-semibold">
              {authorizedDevices.filter(d => d.status === 'AUTHORIZED').length} autorizados
            </span>
          </div>
        </div>

        {/* Acciones Rápidas */}
        <button
          onClick={() => setView('PAIRING')}
          className="w-full py-2.5 bg-[#3B82F6] hover:bg-[#2563EB] text-[#F1F5F9] text-xs font-semibold rounded-lg flex items-center justify-center gap-2 transition-all shadow-md"
        >
          <QrCode className="w-4 h-4" />
          <span>Vincular Nuevo Terminal</span>
        </button>

        {/* Lista de Terminales Registrados */}
        <div>
          <div className="flex justify-between items-center mb-2">
            <span className="text-xs font-semibold text-[#F1F5F9]">Terminales Registrados</span>
            <span className="text-[10px] text-[#94A3B8]">{authorizedDevices.length} total</span>
          </div>

          {authorizedDevices.length === 0 ? (
            <div className="p-4 rounded-xl bg-[#111B2E] border border-dashed border-[#263449] text-center text-xs text-[#94A3B8]">
              No hay terminales vinculados a esta autoridad.
            </div>
          ) : (
            <div className="space-y-2">
              {authorizedDevices.map(device => {
                const isRevoked = device.status === 'REVOKED';
                return (
                  <div
                    key={device.deviceId}
                    className="p-3 bg-[#111B2E] rounded-xl border border-[#263449] flex flex-col gap-2"
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <div className="text-xs font-bold text-[#F1F5F9]">{device.deviceName}</div>
                        <div className="font-mono text-[10px] text-[#94A3B8]">{device.deviceId}</div>
                      </div>
                      <span
                        className={`text-[10px] px-2 py-0.5 rounded font-bold uppercase ${
                          isRevoked
                            ? 'bg-[#F87171]/20 text-[#F87171] border border-[#F87171]/40'
                            : 'bg-[#34D399]/20 text-[#34D399] border border-[#34D399]/40'
                        }`}
                      >
                        {isRevoked ? 'REVOCADO' : 'AUTORIZADO'}
                      </span>
                    </div>

                    {!isRevoked && (
                      <button
                        onClick={() => {
                          setSelectedTerminalForRevocation(device.deviceId);
                          setView('REVOKE');
                        }}
                        className="mt-1 py-1.5 px-3 bg-[#17243A] hover:bg-[#F87171]/20 text-[#F87171] text-[11px] font-semibold rounded-lg border border-[#F87171]/40 flex items-center justify-center gap-1.5 transition-all"
                      >
                        <AlertTriangle className="w-3.5 h-3.5" />
                        <span>Cambiar contraseña y revocar dispositivos autorizados</span>
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      <div className="pt-2 text-[10px] text-center text-[#94A3B8]">
        CORE conserva la autoridad exclusiva sobre aprobaciones y revocaciones.
      </div>
    </div>
  );
};
