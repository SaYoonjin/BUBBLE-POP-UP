import { useMemo, useState, useRef } from "react";
import { Shape, ExtrudeGeometry, Vector3 } from "three";
import { Html } from "@react-three/drei";
import { useFrame } from "@react-three/fiber";
import type { Mesh } from "three";
import type { DistrictGeo } from "./seoulDistricts";

interface DistrictMeshProps {
  district: DistrictGeo;
  isSelected: boolean;
  isHovered: boolean;
  onPointerOver: () => void;
  onPointerOut: () => void;
  onClick: () => void;
}

const gradeColors: Record<string, string> = {
  "S등급": "#f87171",
  "A등급": "#fbbf24",
  "B등급": "#94a3b8",
};

const gradeHoverColors: Record<string, string> = {
  "S등급": "#ef4444",
  "A등급": "#f59e0b",
  "B등급": "#64748b",
};

const selectedColor = "#A8BFA9";

export default function DistrictMesh({
  district, isSelected, isHovered, onPointerOver, onPointerOut, onClick,
}: DistrictMeshProps) {
  const meshRef = useRef<Mesh>(null);
  const [targetY] = useState({ current: 0 });

  const geometry = useMemo(() => {
    const shape = new Shape();
    const pts = district.polygon;
    shape.moveTo(pts[0][0], -pts[0][1]);
    for (let i = 1; i < pts.length; i++) {
      shape.lineTo(pts[i][0], -pts[i][1]);
    }
    shape.closePath();

    return new ExtrudeGeometry(shape, {
      depth: 0.4,
      bevelEnabled: true,
      bevelThickness: 0.05,
      bevelSize: 0.05,
      bevelSegments: 3,
    });
  }, [district.polygon]);

  const baseColor = isSelected ? selectedColor : gradeColors[district.grade] || "#94a3b8";
  const hoverColor = isSelected ? "#8DA98E" : gradeHoverColors[district.grade] || "#64748b";
  const color = isHovered ? hoverColor : baseColor;

  targetY.current = isSelected ? 0.5 : isHovered ? 0.25 : 0;

  useFrame(() => {
    if (meshRef.current) {
      meshRef.current.position.y += (targetY.current - meshRef.current.position.y) * 0.1;
    }
  });

  return (
    <group>
      <mesh
        ref={meshRef}
        geometry={geometry}
        rotation={[-Math.PI / 2, 0, 0]}
        onPointerOver={(e) => { e.stopPropagation(); onPointerOver(); }}
        onPointerOut={onPointerOut}
        onClick={(e) => { e.stopPropagation(); onClick(); }}
        castShadow
        receiveShadow
      >
        <meshStandardMaterial
          color={color}
          metalness={0.1}
          roughness={0.6}
          transparent
          opacity={isSelected ? 1 : isHovered ? 0.95 : 0.85}
        />
      </mesh>

      {/* Edge outline */}
      <mesh geometry={geometry} rotation={[-Math.PI / 2, 0, 0]} position={[0, meshRef.current?.position.y || 0, 0]}>
        <meshBasicMaterial color="#ffffff" wireframe transparent opacity={0.15} />
      </mesh>

      {/* Label */}
      <Html
        position={new Vector3(district.center[0], (isSelected ? 0.8 : isHovered ? 0.55 : 0.3), district.center[1])}
        center
        distanceFactor={10}
        zIndexRange={[1, 0]}
        style={{ pointerEvents: "none" }}
      >
        <div className={`flex flex-col items-center transition-all duration-200 ${isSelected || isHovered ? "scale-110" : ""}`}>
          <div className={`px-2.5 py-1 rounded-lg text-[11px] font-bold whitespace-nowrap shadow-md ${
            isSelected
              ? "bg-primary text-white"
              : isHovered
                ? "bg-white text-slate-800"
                : "bg-white/90 text-slate-600"
          }`}>
            {district.name}
          </div>
          {(isSelected || isHovered) && (
            <div className="mt-1 bg-slate-800/90 text-white text-[9px] font-bold px-2 py-0.5 rounded whitespace-nowrap">
              {district.grade} · {district.rent}
            </div>
          )}
        </div>
      </Html>
    </group>
  );
}
