import React, { useState } from 'react';
import {
  Server,
  ShieldCheck,
  ShieldAlert,
  ArrowRight,
  Database,
  Radio,
  Lock,
  EyeOff,
  Terminal,
  Zap,
  CheckCircle2,
  XCircle
} from 'lucide-react';
import { EncryptedPacket } from '../types/vel';

interface RelayInspectorProps {
  packets: EncryptedPacket[];
  revokedDeviceIds: string[];
  serverConnected: boolean;
  onInjectTamper: (packetId: string) => void;
}

export const RelayInspector: React.FC<RelayInspectorProps> = ({
  packets,
  revokedDeviceIds,
  serverConnected,
  onInjectTamper
}) => {
  const [selectedPacket, setSelectedPacket] = useState<EncryptedPacket | null>(
    packets.length > 0 ? packets[packets.length - 1] : null
  );

  return (
    <div className="bg-[#111B2E] border border-[#263449] rounded-2xl p-5 flex flex-col h-full text-[#F1F5F9]">
      {/* Cabecera */}
      <div className="flex flex-wrap items-center justify-between pb-4 border-b border-[#263449] gap-2">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-lg bg-[#17243A] text-[#22D3EE] border border-[#263449]">
            <Server className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-[#F1F5F9] flex items-center gap-2">
              <span>Servidor de Relevo Ktor (Zero-Knowledge Relay)</span>
              <span className="text-[10px] bg-[#3B82F6]/20 text-[#3B82F6] px-2 py-0.5 rounded border border-[#3B82F6]/40">
                WSS Port 8443
              </span>
            </h3>
            <p className="text-xs text-[#94A3B8]">
              Demostración de Cero Conocimiento: el servidor sólo transporta bytes cifrados AES-GCM.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5 text-xs">
            <Radio className={`w-3.5 h-3.5 ${serverConnected ? 'text-[#34D399]' : 'text-[#FBBF24]'}`} />
            <span className={serverConnected ? 'text-[#34D399]' : 'text-[#FBBF24]'}>
              {serverConnected ? 'En línea' : 'Desconectado'}
            </span>
          </div>

          <div className="flex items-center gap-1.5 text-xs bg-[#17243A] px-2.5 py-1 rounded-md border border-[#263449]">
            <span className="text-[#94A3B8]">Revocados:</span>
            <span className="text-[#F87171] font-bold">{revokedDeviceIds.length}</span>
          </div>
        </div>
      </div>

      {/* Explicación de la Garantía Criptográfica */}
      <div className="mt-4 p-3 bg-[#17243A] rounded-xl border border-[#263449] flex items-start gap-3">
        <EyeOff className="w-5 h-5 text-[#22D3EE] flex-shrink-0 mt-0.5" />
        <div className="text-xs leading-relaxed">
          <span className="font-semibold text-[#22D3EE]">Garantía de Privacidad Absoluta (Parte VI.25): </span>
          El servidor no posee las claves privadas de CORE ni de Terminal, ni la clave simétrica de 256 bits. Si una entidad inspecciona la memoria del servidor o intercepta los paquetes, sólo observará texto cifrado y nonces de 96 bits.
        </div>
      </div>

      {/* Contenido dividido: Lista de paquetes retransmitidos y Detalle de inspección */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4 flex-1 overflow-hidden min-h-[300px]">
        {/* Lista de Paquetes que cruzan el servidor */}
        <div className="flex flex-col bg-[#0B1220] rounded-xl border border-[#263449] p-3 overflow-hidden">
          <div className="text-xs font-semibold text-[#94A3B8] mb-2 flex items-center justify-between">
            <span>Tráfico de Sobres Cifrados en Tránsito</span>
            <span className="text-[10px] text-[#22D3EE]">{packets.length} eventos</span>
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {packets.length === 0 ? (
              <div className="h-full flex items-center justify-center text-xs text-[#94A3B8] p-4 text-center">
                Esperando operaciones protegidas entre Terminal y CORE...
              </div>
            ) : (
              packets.map((pkt, idx) => {
                const isSelected = selectedPacket?.requestId === pkt.requestId;
                return (
                  <div
                    key={pkt.requestId + idx}
                    onClick={() => setSelectedPacket(pkt)}
                    className={`p-2.5 rounded-lg border text-xs cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-[#17243A] border-[#22D3EE]'
                        : 'bg-[#111B2E] border-[#263449] hover:border-[#3B82F6]/50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-[11px] text-[#22D3EE]">
                        {pkt.requestId.slice(0, 14)}...
                      </span>
                      <span className="text-[10px] text-[#94A3B8]">
                        {new Date(pkt.timestamp).toLocaleTimeString()}
                      </span>
                    </div>

                    <div className="flex items-center gap-2 mt-1.5 text-[11px]">
                      <span className="text-[#3B82F6] font-medium truncate max-w-[100px]">
                        {pkt.sourceDeviceId.slice(0, 10)}
                      </span>
                      <ArrowRight className="w-3 h-3 text-[#94A3B8]" />
                      <span className="text-[#22D3EE] font-medium truncate max-w-[100px]">
                        {pkt.targetDeviceId.slice(0, 10)}
                      </span>
                    </div>

                    <div className="mt-1 flex items-center justify-between text-[10px]">
                      <span className="font-mono text-[#94A3B8] truncate max-w-[180px]">
                        Cifrado: {pkt.ciphertextBase64.slice(0, 20)}...
                      </span>
                      {pkt.isTampered ? (
                        <span className="text-[#F87171] font-bold flex items-center gap-1">
                          <XCircle className="w-3 h-3" /> Manipulado
                        </span>
                      ) : (
                        <span className="text-[#34D399] flex items-center gap-1">
                          <CheckCircle2 className="w-3 h-3" /> Íntegro
                        </span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Detalle del paquete e inyección de prueba de manipulación (Tampering) */}
        <div className="flex flex-col bg-[#0B1220] rounded-xl border border-[#263449] p-3 overflow-hidden">
          <div className="text-xs font-semibold text-[#94A3B8] mb-2">
            Inspección Criptográfica en Memoria del Servidor
          </div>

          {selectedPacket ? (
            <div className="flex-1 overflow-y-auto space-y-3 pr-1 text-xs">
              <div className="space-y-1">
                <span className="text-[#94A3B8] text-[10px] uppercase font-semibold">Identificador de Solicitud (UUID):</span>
                <div className="font-mono bg-[#111B2E] p-2 rounded border border-[#263449] text-[#22D3EE] text-[11px] break-all">
                  {selectedPacket.requestId}
                </div>
              </div>

              <div className="space-y-1">
                <span className="text-[#94A3B8] text-[10px] uppercase font-semibold">Nonce Único de 96 bits (Base64):</span>
                <div className="font-mono bg-[#111B2E] p-2 rounded border border-[#263449] text-[#FBBF24] text-[11px] break-all">
                  {selectedPacket.nonceBase64}
                </div>
              </div>

              <div className="space-y-1">
                <span className="text-[#94A3B8] text-[10px] uppercase font-semibold">Ciphertext AES-GCM + Tag 128-bit (Base64):</span>
                <div className="font-mono bg-[#111B2E] p-2 rounded border border-[#263449] text-[#F1F5F9] text-[11px] break-all">
                  {selectedPacket.ciphertextBase64}
                </div>
              </div>

              <div className="space-y-1">
                <span className="text-[#94A3B8] text-[10px] uppercase font-semibold">Datos Autenticados Adicionales (AAD):</span>
                <div className="font-mono bg-[#111B2E] p-2 rounded border border-[#263449] text-[#94A3B8] text-[11px] break-all">
                  {selectedPacket.aadBase64 || '(Sin AAD)'}
                </div>
              </div>

              {/* Botón de Ataque de Integridad Controlado */}
              <div className="pt-2">
                <button
                  onClick={() => onInjectTamper(selectedPacket.requestId)}
                  disabled={selectedPacket.isTampered}
                  className="w-full py-2 px-3 bg-[#17243A] hover:bg-[#F87171]/20 text-[#F87171] border border-[#F87171]/40 rounded-lg font-semibold flex items-center justify-center gap-2 transition-all disabled:opacity-40"
                >
                  <ShieldAlert className="w-4 h-4" />
                  <span>
                    {selectedPacket.isTampered
                      ? 'Ciphertext ya ha sido alterado (Tag GCM fallará)'
                      : 'Simular Manipulación de 1 Bit en Servidor (Prueba de Tag)'}
                  </span>
                </button>
                <p className="text-[10px] text-[#94A3B8] mt-1.5 text-center">
                  Al alterar 1 bit en tránsito, CORE o Terminal rechazarán el mensaje inmediatamente sin ejecutarlo.
                </p>
              </div>
            </div>
          ) : (
            <div className="h-full flex items-center justify-center text-xs text-[#94A3B8] text-center p-4">
              Selecciona un sobre cifrado para auditar su carga binaria en el relevo.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
