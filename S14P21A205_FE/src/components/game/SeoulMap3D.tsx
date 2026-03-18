import { useState, Suspense } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls, Environment, ContactShadows } from "@react-three/drei";
import DistrictMesh from "./DistrictMesh";
import { seoulDistricts, type DistrictGeo } from "./seoulDistricts";

interface SeoulMap3DProps {
  selectedId: number | null;
  onSelect: (id: number) => void;
}

function MapScene({ selectedId, onSelect }: SeoulMap3DProps) {
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  return (
    <>
      {/* Lighting */}
      <ambientLight intensity={0.5} />
      <directionalLight position={[5, 8, 5]} intensity={0.8} castShadow shadow-mapSize={1024} />
      <directionalLight position={[-3, 4, -2]} intensity={0.3} />

      {/* Base platform */}
      <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.05, 0.5]} receiveShadow>
        <circleGeometry args={[7, 64]} />
        <meshStandardMaterial color="#f0f0ec" metalness={0} roughness={1} />
      </mesh>

      {/* Grid lines on platform */}
      <gridHelper args={[14, 28, "#e2e2e0", "#e2e2e0"]} position={[0, -0.04, 0.5]} />

      {/* District meshes */}
      {seoulDistricts.map((d) => (
        <DistrictMesh
          key={d.id}
          district={d}
          isSelected={selectedId === d.id}
          isHovered={hoveredId === d.id}
          onPointerOver={() => setHoveredId(d.id)}
          onPointerOut={() => setHoveredId(null)}
          onClick={() => onSelect(d.id)}
        />
      ))}

      {/* Shadows */}
      <ContactShadows position={[0, -0.04, 0.5]} opacity={0.3} scale={15} blur={2} />

      {/* Camera controls */}
      <OrbitControls
        makeDefault
        enablePan={false}
        minDistance={6}
        maxDistance={16}
        minPolarAngle={Math.PI / 6}
        maxPolarAngle={Math.PI / 2.5}
        target={[0, 0, 0.5]}
        autoRotate
        autoRotateSpeed={0.5}
      />

      <Environment preset="city" />
    </>
  );
}

export default function SeoulMap3D({ selectedId, onSelect }: SeoulMap3DProps) {
  return (
    <div className="w-full h-full relative z-0">
      <Canvas
        shadows
        camera={{ position: [0, 8, 8], fov: 45 }}
        style={{ background: "transparent" }}
        onPointerMissed={() => {}}
      >
        <Suspense fallback={null}>
          <MapScene selectedId={selectedId} onSelect={onSelect} />
        </Suspense>
      </Canvas>

      {/* Drag hint */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-2 bg-white/80 backdrop-blur-sm px-3 py-1.5 rounded-full shadow-sm text-[11px] text-slate-400 font-medium pointer-events-none">
        <span className="material-symbols-outlined text-[14px]">3d_rotation</span>
        드래그하여 회전 · 스크롤하여 줌
      </div>
    </div>
  );
}
