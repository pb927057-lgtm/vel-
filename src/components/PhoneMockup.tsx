import React from 'react';
import { Wifi, Battery, Shield } from 'lucide-react';

interface PhoneMockupProps {
  title: string;
  subtitle: string;
  roleBadge: 'CORE' | 'TERMINAL' | 'UNCONFIGURED';
  children: React.ReactNode;
}

export const PhoneMockup: React.FC<PhoneMockupProps> = ({
  title,
  subtitle,
  roleBadge,
  children
}) => {
  return (
    <div className="flex flex-col items-center">
      {/* Indicador de rol superior fuera del teléfono */}
      <div className="mb-2 flex items-center gap-2">
        <span
          className={`px-2.5 py-0.5 rounded text-xs font-semibold tracking-wider uppercase border ${
            roleBadge === 'CORE'
              ? 'bg-[#17243A] text-[#22D3EE] border-[#22D3EE]/40'
              : roleBadge === 'TERMINAL'
              ? 'bg-[#17243A] text-[#3B82F6] border-[#3B82F6]/40'
              : 'bg-[#111B2E] text-[#94A3B8] border-[#263449]'
          }`}
        >
          {roleBadge === 'CORE' ? 'Autoridad CORE' : roleBadge === 'TERMINAL' ? 'Terminal Móvil' : 'No Configurado'}
        </span>
        <span className="text-xs text-[#94A3B8]">{subtitle}</span>
      </div>

      {/* Chasis de Teléfono Android (380px de ancho para cumplir 360-430 dp estándar) */}
      <div className="w-[380px] h-[780px] bg-[#0B1220] rounded-[40px] p-3 shadow-2xl border-[3px] border-[#263449] relative flex flex-col overflow-hidden">
        {/* Cámara frontal punch-hole */}
        <div className="absolute top-4 left-1/2 -translate-x-1/2 w-4 h-4 rounded-full bg-[#111B2E] border border-[#263449] z-30" />

        {/* Barra de estado de Android */}
        <div className="h-7 w-full flex items-center justify-between px-5 text-[11px] text-[#94A3B8] z-20 select-none">
          <span className="font-medium">10:42</span>
          <div className="flex items-center gap-2">
            <Shield className="w-3 h-3 text-[#22D3EE]" />
            <Wifi className="w-3.5 h-3.5 text-[#F1F5F9]" />
            <Battery className="w-3.5 h-3.5 text-[#F1F5F9]" />
          </div>
        </div>

        {/* Pantalla táctil activa */}
        <div className="flex-1 w-full bg-[#0B1220] rounded-[28px] overflow-hidden flex flex-col relative z-10 border border-[#263449]/50">
          {children}
        </div>

        {/* Barra de gestos de navegación Android */}
        <div className="h-5 w-full flex items-center justify-center">
          <div className="w-32 h-1 bg-[#263449] rounded-full" />
        </div>
      </div>
    </div>
  );
};
