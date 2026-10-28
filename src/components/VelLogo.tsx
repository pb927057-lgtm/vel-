import React from 'react';

interface VelLogoProps {
  size?: number;
  showWordmark?: boolean;
  className?: string;
}

export const VelLogo: React.FC<VelLogoProps> = ({
  size = 72,
  showWordmark = true,
  className = ''
}) => {
  return (
    <div className={`inline-flex flex-col items-center justify-center ${className}`}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 120 120"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="transition-transform duration-300 hover:scale-105"
      >
        {/* Placa base tecnológica */}
        <circle cx="60" cy="60" r="54" fill="#111B2E" stroke="#263449" strokeWidth="2" />

        {/* Curva inferior de enlace entre dos dispositivos */}
        <path
          d="M32 74 C 42 94, 78 94, 88 74"
          stroke="#22D3EE"
          strokeWidth="3.5"
          strokeLinecap="round"
        />

        {/* Letra V tecnológica que acompaña la trayectoria */}
        <path
          d="M36 36 L60 82 L84 36"
          stroke="#3B82F6"
          strokeWidth="5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />

        {/* Nodo de autoridad CORE (izquierda - turquesa) */}
        <circle cx="36" cy="36" r="6" fill="#22D3EE" />
        <circle cx="36" cy="36" r="2.5" fill="#0B1220" />

        {/* Nodo móvil Terminal (derecha - azul) */}
        <circle cx="84" cy="36" r="6" fill="#3B82F6" />
        <circle cx="84" cy="36" r="2.5" fill="#0B1220" />

        {/* Nodo central de verificación criptográfica */}
        <circle cx="60" cy="82" r="4.5" fill="#F1F5F9" />
      </svg>

      {showWordmark && (
        <div className="mt-2 text-center">
          <span className="font-mono tracking-widest text-2xl font-bold text-[#F1F5F9]">
            VEL
          </span>
        </div>
      )}
    </div>
  );
};
