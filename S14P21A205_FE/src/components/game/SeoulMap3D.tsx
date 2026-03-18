import { useState, Suspense } from "react";
import { Canvas } from "@react-three/fiber";
import { OrbitControls, Environment, ContactShadows } from "@react-three/drei";
import DistrictMesh, { BackgroundMesh } from "./DistrictMesh";
import { seoulDistricts, backgroundGus } from "./seoulDistricts";

interface SeoulMap3DProps {
  selectedId: number | null;
  onSelect: (id: number) => void;
}

function MapScene({ selectedId, onSelect }: SeoulMap3DProps) {
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  return (
    <>
      <ambientLight intensity={0.6} />
      <directionalLight position={[10, 20, 10]} intensity={0.7} castShadow shadow-mapSize={1024} />
      <directionalLight position={[-8, 12, -6]} intensity={0.2} />

      {/* Background gu (flat) */}
      {backgroundGus.map((gu) => (
        <BackgroundMesh key={gu.name} polygon={gu.polygon} name={gu.name} center={gu.center} />
      ))}

      {/* Interactive districts */}
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

      <ContactShadows position={[0, -0.01, 0]} opacity={0.12} scale={45} blur={3} />

      <OrbitControls
        makeDefault
        enablePan={false}
        minDistance={18}
        maxDistance={45}
        minPolarAngle={Math.PI / 6}
        maxPolarAngle={Math.PI / 2.5}
        target={[0, 0, 0]}
        autoRotate
        autoRotateSpeed={0.3}
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
        camera={{ position: [0, 22, 22], fov: 42 }}
        style={{ background: "transparent" }}
        onPointerMissed={() => {}}
      >
        <Suspense fallback={null}>
          <MapScene selectedId={selectedId} onSelect={onSelect} />
        </Suspense>
      </Canvas>

      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-2 bg-white/80 backdrop-blur-sm px-3 py-1.5 rounded-full shadow-sm text-[11px] text-slate-400 font-medium pointer-events-none">
        <span className="material-symbols-outlined text-[14px]">3d_rotation</span>
        드래그하여 회전 · 스크롤하여 줌
      </div>
    </div>
  );
}
